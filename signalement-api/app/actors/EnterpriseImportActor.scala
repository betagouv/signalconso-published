package actors

import akka.NotUsed
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.FlowShape
import akka.stream.KillSwitches
import akka.stream.SharedKillSwitch
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.alpakka.csv.scaladsl.CsvToMap
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Merge
import akka.stream.scaladsl.Partition
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import company.CompanyFile
import company.EnterpriseImportInfo
import company.EtablissementFile
import company.UniteLegaleFile
import company.companydata.CompanyDataRepositoryInterface
import company.entrepriseimportinfo.EnterpriseImportInfoRepository
import play.api.Logger
import utils.SIREN

import java.io.BufferedInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.UUID
import java.util.zip.ZipInputStream
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object EnterpriseSyncActor {
  def props = Props[EnterpriseSyncActor]()

  sealed trait Command

  final case class Start(
      companyFile: CompanyFile
  ) extends Command

  final case class Cancel(name: String) extends Command

}

final case class ProcessedFile(
    infoId: UUID,
    stream: SharedKillSwitch
)

class EnterpriseSyncActor(
    enterpriseSyncInfoRepo: EnterpriseImportInfoRepository,
    companyDataRepository: CompanyDataRepositoryInterface
)(implicit
    val executionContext: ExecutionContext
) extends Actor {

  import EnterpriseSyncActor._

  implicit val session = SlickSession.forConfig("slick.dbs.company_db")
  implicit val actorSystem: ActorSystem = context.system
  val batchSize = 5000
  actorSystem.registerOnTermination(() => session.close())

  private[this] val logger = Logger(this.getClass)

  val sharedKillSwitch = KillSwitches.shared("my-kill-switch")
  private[this] var processedFiles: Map[String, ProcessedFile] = Map()

  override def receive = {
    case Start(companyFile) =>
      for {
        _ <- cancel(companyFile.name)
        jobId = UUID.randomUUID()
        _ <- enterpriseSyncInfoRepo.create(
          EnterpriseImportInfo(
            id = jobId,
            fileName = companyFile.name,
            fileUrl = companyFile.url.toString,
            linesCount = companyFile.approximateSize
          )
        )
        filePath = s"./${companyFile.name}.csv"
        _ = logger.debug(s"------------------  Downloading ${companyFile.name} file ------------------")
        _ <- {
          val inputstream = new ZipInputStream(new BufferedInputStream(companyFile.url.openStream()))
          inputstream.getNextEntry
          StreamConverters
            .fromInputStream(() => inputstream)
            .runWith(FileIO.toPath(Paths.get(filePath)))
        }
        _ = logger.debug(s"File saved in ${filePath}")
      } yield ingestFile(jobId, companyFile)
      ()

    case Cancel(name) =>
      cancel(name)
      ()

  }

  private[this] def cancel(name: String) = for {
    _ <- enterpriseSyncInfoRepo.updateAllError(name, "<CANCELLED>")
    _ <- enterpriseSyncInfoRepo.updateAllEndedAt(name, OffsetDateTime.now)
    _ <- Future.successful(processedFiles.get(name).map { processFile =>
      processedFiles = processedFiles - name
      processFile.stream.shutdown()
    })
  } yield ()

  private[this] def ingestFile[T](
      jobId: UUID,
      companyFile: CompanyFile
  ) = {

    logger.debug(s"Start importing from ${companyFile.url.toString}")
    var linesDone = 0d

    val source: Source[Map[String, String], Any] =
      FileIO
        .fromPath(Paths.get(s"./${companyFile.name}.csv"))
        .via(CsvParsing.lineScanner(maximumLineLength = 4096))
        .drop(1)
        .via(CsvToMap.withHeadersAsStrings(StandardCharsets.UTF_8, companyFile.headers: _*))

    val EtablissementIngestionFlow: Flow[Map[String, String], Int, NotUsed] =
      Flow[Map[String, String]]
        .map(_.view.mapValues(x => Option(x).filter(_.trim.nonEmpty)).toMap: Map[String, Option[String]])
        .filter { columnsValueMap =>
          columnsValueMap.contains("siret") && columnsValueMap.contains("siren")
        }
        .grouped(batchSize)
        .via(
          Slick.flow(4, group => group.map(companyDataRepository.insertAll).reduceLeft(_.andThen(_)))
        )
        .via(sharedKillSwitch.flow)

    val UniteLegaleIngestionFlow: Flow[Map[String, String], Int, NotUsed] =
      Flow[Map[String, String]]
        .map(_.filterNot(_._2.trim.isEmpty))
        .map { columsValueMap =>
          columsValueMap.get("siren").map { siren =>
            val enterpriseName = columsValueMap
              .get("denominationunitelegale")
              .orElse(columsValueMap.get("denominationusuelle1unitelegale"))
              .orElse(columsValueMap.get("denominationusuelle2unitelegale"))
              .orElse(columsValueMap.get("denominationusuelle3unitelegale"))
              .getOrElse(
                columsValueMap.getOrElse("prenomusuelunitelegale", "") + " " + columsValueMap
                  .get("nomusageunitelegale")
                  .orElse(columsValueMap.get("nomunitelegale"))
                  .getOrElse("")
              )
            (SIREN(siren), enterpriseName)
          }
        }
        .collect { case Some(value) =>
          value
        }
        .grouped(batchSize)
        .map { x =>
          logger.debug(s"Processing ${x.size} elements")
          x
        }
        .via(
          Slick.flow(4, group => group.map(companyDataRepository.updateName(_)).reduceLeft(_.andThen(_)))
        )
        .via(sharedKillSwitch.flow)

    val processFileFlow = Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val partition = builder.add(
        Partition[Map[String, String]](
          2,
          _ =>
            companyFile match {
              case EtablissementFile => 0
              case UniteLegaleFile   => 1
            }
        )
      )

      val merge = builder.add(Merge[Int](inputPorts = 2, eagerComplete = true))

      partition.out(0) ~> EtablissementIngestionFlow ~> merge.in(0)
      partition.out(1) ~> UniteLegaleIngestionFlow ~> merge.in(1)

      FlowShape(partition.in, merge.out)
    })

    val stream = source
      .via(processFileFlow)
      .map { _ =>
        linesDone = linesDone + batchSize
        enterpriseSyncInfoRepo.updateLinesDone(jobId, linesDone)
      }
      .runWith(Sink.ignore)

    processedFiles = processedFiles + (companyFile.name -> ProcessedFile(
      infoId = jobId,
      stream = sharedKillSwitch
    ))

    stream
      .flatMap(_ => enterpriseSyncInfoRepo.updateEndedAt(jobId))
      .recoverWith { case err =>
        logger.error(s"Error occurred while importing ${companyFile.url}", err)
        enterpriseSyncInfoRepo.updateError(jobId, err.toString)
      }

  }
}
