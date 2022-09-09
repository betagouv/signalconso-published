package repositories.dataeconomie

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.scaladsl.Source
import models.report.Report
import repositories.report.ReportTable

class DataEconomieRepository(
    system: ActorSystem
) extends DataEconomieRepositoryInterface {

  implicit val session = SlickSession.forConfig("slick.dbs.default")
  val batchSize = 5000
  system.registerOnTermination(() => session.close())

  import session.profile.api._

  override def reports(): Source[Report, NotUsed] =
    Slick
      .source(ReportTable.table.result)
      .log("user")

}
