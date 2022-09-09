package controllers

import models.UserPermission
import com.mohiva.play.silhouette.api.Silhouette
import orchestrators.EventsOrchestratorInterface
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import utils.SIRET
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithPermission

import java.util.UUID
import scala.concurrent.ExecutionContext

class EventsController(
    eventsOrchestrator: EventsOrchestratorInterface,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit
    val ec: ExecutionContext
) extends BaseController(controllerComponents) {

  def getCompanyEvents(siret: SIRET, eventType: Option[String]): Action[AnyContent] =
    SecuredAction(WithPermission(UserPermission.listReports)).async { implicit request =>
      logger.info(s"Fetching events for company $siret with eventType $eventType")
      eventsOrchestrator
        .getCompanyEvents(siret = siret, eventType = eventType, userRole = request.identity.userRole)
        .map(events => Ok(Json.toJson(events)))
    }

  def getReportEvents(reportId: UUID, eventType: Option[String]): Action[AnyContent] =
    SecuredAction(WithPermission(UserPermission.listReports)).async { implicit request =>
      logger.info(s"Fetching events for report $reportId with eventType $eventType")
      eventsOrchestrator
        .getReportsEvents(reportId = reportId, eventType = eventType, userRole = request.identity.userRole)
        .map(events => Ok(Json.toJson(events)))
    }

}
