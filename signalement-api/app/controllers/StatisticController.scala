package controllers

import com.mohiva.play.silhouette.api.Silhouette
import models._
import models.report.ReportFilter.transmittedReportsFilter
import models.report.ReportFilter
import models.report.ReportResponseType
import models.report.ReportStatus
import models.report.ReportStatus.LanceurAlerte
import models.report.ReportStatus.statusWithProResponse
import models.report.ReportTag.ReportTagHiddenToProfessionnel
import orchestrators.StatsOrchestrator
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import utils.QueryStringMapper
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithRole

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class StatisticController(
    statsOrchestrator: StatsOrchestrator,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  def getReportsCount() = SecuredAction.async { request =>
    ReportFilter
      .fromQueryString(request.queryString, request.identity.userRole)
      .fold(
        error => {
          logger.error("Cannot parse querystring", error)
          Future.successful(BadRequest)
        },
        filters =>
          statsOrchestrator
            .getReportCount(filters)
            .map(count => Ok(Json.obj("value" -> count)))
      )
  }

  /** Nom de fonction adoubé par Saïd. En cas d'incompréhension, merci de le contacter directement
    */
  def getReportsCountCurve() = SecuredAction.async { request =>
    ReportFilter
      .fromQueryString(request.queryString, request.identity.userRole)
      .fold(
        error => {
          logger.error("Cannot parse querystring", error)
          Future.successful(BadRequest)
        },
        filters => {
          val mapper = new QueryStringMapper(request.queryString)
          val ticks = mapper.int("ticks").getOrElse(12)
          val tickDuration = mapper
            .string("tickDuration")
            .flatMap(CurveTickDuration.namesToValuesMap.get)
            .getOrElse(CurveTickDuration.Month)
          statsOrchestrator.getReportsCountCurve(filters, ticks, tickDuration).map(curve => Ok(Json.toJson(curve)))
        }
      )
  }

  def getPublicStatCount(publicStat: PublicStat) = Action.async {
    ((publicStat.filter, publicStat.percentageBaseFilter) match {
      case (filter, Some(percentageBaseFilter)) =>
        statsOrchestrator.getReportCountPercentageWithinReliableDates(filter, percentageBaseFilter)
      case (filter, _) =>
        statsOrchestrator.getReportCount(filter)
    }).map(curve => Ok(Json.toJson(curve)))
  }

  def getPublicStatCurve(publicStat: PublicStat) = Action.async {
    ((publicStat.filter, publicStat.percentageBaseFilter) match {
      case (filter, Some(percentageBaseFilter)) =>
        statsOrchestrator.getReportsCountPercentageCurve(filter, percentageBaseFilter)
      case (filter, _) =>
        statsOrchestrator.getReportsCountCurve(filter)
    }).map(curve => Ok(Json.toJson(curve)))
  }

  def getDelayReportReadInHours(companyId: Option[UUID]) = SecuredAction(
    WithRole(UserRole.Admin, UserRole.DGCCRF)
  ).async {
    statsOrchestrator
      .getReadAvgDelay(companyId)
      .map(count => Ok(Json.toJson(StatsValue(count.map(_.toHours.toInt)))))
  }

  def getDelayReportResponseInHours(companyId: Option[UUID]) = SecuredAction.async { request =>
    statsOrchestrator
      .getResponseAvgDelay(companyId: Option[UUID], request.identity.userRole)
      .map(count => Ok(Json.toJson(StatsValue(count.map(_.toHours.toInt)))))
  }

  def getReportResponseReviews(companyId: Option[UUID]) = SecuredAction.async {
    statsOrchestrator.getReportResponseReview(companyId).map(x => Ok(Json.toJson(x)))
  }

  def getReportsTagsDistribution(companyId: Option[UUID]) = SecuredAction.async { request =>
    statsOrchestrator.getReportsTagsDistribution(companyId, request.identity.userRole).map(x => Ok(Json.toJson(x)))
  }

  def getReportsStatusDistribution(companyId: Option[UUID]) = SecuredAction.async { request =>
    statsOrchestrator.getReportsStatusDistribution(companyId, request.identity.userRole).map(x => Ok(Json.toJson(x)))
  }

  def getProReportToTransmitStat() =
    Action.async { _ =>
      // Includes the reports that we want to transmit to a pro
      // but we have not identified the company
      val filter = ReportFilter(
        status = ReportStatus.values.filterNot(_ == LanceurAlerte),
        withoutTags = ReportTagHiddenToProfessionnel
      )
      statsOrchestrator.getReportsCountCurve(filter).map(curve => Ok(Json.toJson(curve)))
    }

  def getProReportTransmittedStat() = SecuredAction.async { _ =>
    statsOrchestrator.getReportsCountCurve(transmittedReportsFilter).map(curve => Ok(Json.toJson(curve)))
  }

  def getProReportResponseStat(responseTypeQuery: Option[List[ReportResponseType]]) =
    SecuredAction.async(parse.empty) { _ =>
      val statusFilter = responseTypeQuery
        .filter(_.nonEmpty)
        .map(_.map(ReportStatus.fromResponseType))
        .getOrElse(statusWithProResponse)
      val filter = ReportFilter(status = statusFilter)
      statsOrchestrator.getReportsCountCurve(filter).map(curve => Ok(Json.toJson(curve)))
    }

  def dgccrfAccountsCurve(ticks: Option[Int]) = SecuredAction.async(parse.empty) { _ =>
    statsOrchestrator.dgccrfAccountsCurve(ticks.getOrElse(12)).map(x => Ok(Json.toJson(x)))
  }

  def dgccrfActiveAccountsCurve(ticks: Option[Int]) = SecuredAction.async(parse.empty) { _ =>
    statsOrchestrator.dgccrfActiveAccountsCurve(ticks.getOrElse(12)).map(x => Ok(Json.toJson(x)))
  }

  def dgccrfSubscription(ticks: Option[Int]) = SecuredAction.async(parse.empty) { _ =>
    statsOrchestrator.dgccrfSubscription(ticks.getOrElse(12)).map(x => Ok(Json.toJson(x)))
  }

  def dgccrfControlsCurve(ticks: Option[Int]) = SecuredAction.async(parse.empty) { _ =>
    statsOrchestrator.dgccrfControlsCurve(ticks.getOrElse(12)).map(x => Ok(Json.toJson(x)))
  }

  def countByDepartments() = SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { implicit request =>
    val mapper = new QueryStringMapper(request.queryString)
    val start = mapper.localDate("start")
    val end = mapper.localDate("end")
    statsOrchestrator.countByDepartments(start, end).map(res => Ok(Json.toJson(res)))
  }

}
