package controllers

import com.mohiva.play.silhouette.api.Silhouette
import models.report.Report
import models.report.ReportFile
import models.report.ReportFileOrigin
import models.report.ReportFileToExternal
import models.report.ReportFilter
import models.report.ReportToExternal
import models.report.ReportWithFiles
import models.report.ReportWithFilesToExternal
import models.report.ReportWithFilesToExternal.format
import orchestrators.ReportOrchestrator
import play.api.Logger
import play.api.libs.json.Json
import utils.QueryStringMapper
import utils.silhouette.api.APIKeyEnv

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import models.PaginatedResult.paginatedResultWrites
import models.report.ReportTag
import play.api.mvc.ControllerComponents
import repositories.report.ReportRepositoryInterface
import repositories.reportfile.ReportFileRepositoryInterface

class ReportToExternalController(
    reportRepository: ReportRepositoryInterface,
    reportFileRepository: ReportFileRepositoryInterface,
    reportOrchestrator: ReportOrchestrator,
    val silhouette: Silhouette[APIKeyEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends ApiKeyBaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  def getReportToExternal(uuid: String) = SecuredAction.async { _ =>
    logger.debug("Calling report to external")
    Try(UUID.fromString(uuid)) match {
      case Failure(_) => Future.successful(PreconditionFailed)
      case Success(id) =>
        for {
          report <- reportRepository.get(id)
          reportFiles <- report.map(r => reportFileRepository.retrieveReportFiles(r.id)).getOrElse(Future(List.empty))
        } yield report
          .map(report => ReportWithFiles(report, reportFiles.filter(_.origin == ReportFileOrigin.CONSUMER)))
          .map(ReportWithFilesToExternal.fromReportWithFiles)
          .map(report => Ok(Json.toJson(report)))
          .getOrElse(NotFound)
    }
  }

  def searchReportsToExternal() = SecuredAction.async { implicit request =>
    val qs = new QueryStringMapper(request.queryString)
    val filter = ReportFilter(
      siretSirenList = qs.string("siret").map(List(_)).getOrElse(List()),
      start = qs.timeWithLocalDateRetrocompatStartOfDay("start"),
      end = qs.timeWithLocalDateRetrocompatEndOfDay("end"),
      withTags = qs.seq("tags").map(ReportTag.withName)
    )

    for {
      reportsWithFiles <- reportRepository.getReportsWithFiles(
        filter = filter
      )
    } yield Ok(
      Json.toJson(
        reportsWithFiles.map { case (report, fileList) =>
          ReportWithFilesToExternal(
            ReportToExternal.fromReport(report),
            fileList.map(ReportFileToExternal.fromReportFile)
          )
        }
      )
    )
  }

  def searchReportsToExternalV2() = SecuredAction.async { implicit request =>
    val qs = new QueryStringMapper(request.queryString)
    val filter = ReportFilter(
      siretSirenList = qs.string("siret").map(List(_)).getOrElse(List()),
      start = qs.timeWithLocalDateRetrocompatStartOfDay("start"),
      end = qs.timeWithLocalDateRetrocompatEndOfDay("end"),
      withTags = qs.seq("tags").map(ReportTag.withName)
    )
    val offset = qs.long("offset")
    val limit = qs.int("limit")

    for {
      reportsWithFiles <- reportOrchestrator.getReportsWithFile(
        filter = filter,
        offset,
        limit,
        (r: Report, m: Map[UUID, List[ReportFile]]) =>
          ReportWithFilesToExternal(
            ReportToExternal.fromReport(r),
            m.getOrElse(r.id, Nil).map(ReportFileToExternal.fromReportFile)
          )
      )
    } yield Ok(
      Json.toJson(reportsWithFiles)(paginatedResultWrites[ReportWithFilesToExternal](ReportWithFilesToExternal.format))
    )
  }

  /** @deprecated
    *   Keep it for retro-compatibility purpose but searchReportsToExternal() is the good one.
    */
  def searchReportsToExternalBySiret(siret: String) = SecuredAction.async { implicit request =>
    val qs = new QueryStringMapper(request.queryString)
    val filter = ReportFilter(
      siretSirenList = List(siret),
      start = qs.timeWithLocalDateRetrocompatStartOfDay("start"),
      end = qs.timeWithLocalDateRetrocompatEndOfDay("end")
    )
    for {
      reports <- reportRepository.getReports(filter, Some(0), Some(1000000))
    } yield Ok(Json.toJson(reports.entities.map(ReportToExternal.fromReport)))
  }

}
