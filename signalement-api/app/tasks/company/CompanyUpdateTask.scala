package tasks.company

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.alpakka.slick.scaladsl.SlickSession
import company.CompanySearchResult
import company.companydata.CompanyDataRepositoryInterface
import config.CompanyUpdateTaskConfiguration
import play.api.Logger
import repositories.company.CompanyRepositoryInterface
import repositories.company.CompanyTable
import tasks.computeStartingTime

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

class CompanyUpdateTask(
    actorSystem: ActorSystem,
    companyUpdateConfiguration: CompanyUpdateTaskConfiguration,
    companyRepository: CompanyRepositoryInterface,
    companyDataRepository: CompanyDataRepositoryInterface
)(implicit
    executionContext: ExecutionContext,
    materializer: Materializer
) {

  implicit val session = SlickSession.forConfig("slick.dbs.default")
  val batchSize = 5000
  actorSystem.registerOnTermination(() => session.close())

  import session.profile.api._

  val logger: Logger = Logger(this.getClass)
  implicit val timeout: akka.util.Timeout = 5.seconds

  val startTime = companyUpdateConfiguration.startTime
  val initialDelay: FiniteDuration = computeStartingTime(startTime)

  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = initialDelay, interval = 1.day) { () =>
    logger.debug(s"initialDelay - ${initialDelay}");
    runTask()
    ()
  }

  def runTask() =
//    val backend = HttpClientFutureBackend()

    Slick
      .source(CompanyTable.table.result)
      .grouped(500)
      .mapAsync(1) { companies =>
//        val response =
//          basicRequest
//            .post(uri"http://localhost:9001/api/companies/search")
//            .body(companies.map(_.siret))
//            .response(asJson[List[CompanySearchResult]])
//            .send(backend)
//        response
//          .map(_.body)
//          .map {
//            case Right(companyList) =>
//              companyList.map { companySearchResult =>
//                companySearchResult
//              }
//            case Left(value) =>
//              logger.warn("Error calling company update", value)
//              List.empty
//          }

        companyDataRepository
          .searchBySirets(companies.map(_.siret).toList, includeClosed = true)
          .map(companies =>
            companies.map { case (companyData, maybeActivity) =>
              companyData.toSearchResult(maybeActivity.map(_.label))
            }
          )

      }
      .map((companies: Seq[CompanySearchResult]) =>
        companies.map(c => companyRepository.updateBySiret(c.siret, c.isOpen, c.isHeadOffice))
      )
      .log("company update")
      .run()
      .map(_ => logger.info("Company update done"))

}
