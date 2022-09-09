package repositories.dataeconomie
import akka.NotUsed
import akka.stream.scaladsl.Source
import models.report.Report

trait DataEconomieRepositoryInterface {

  def reports(): Source[Report, NotUsed]
}
