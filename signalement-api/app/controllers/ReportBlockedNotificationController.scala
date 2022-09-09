package controllers

import com.mohiva.play.silhouette.api.Silhouette
import models.UserRole
import models.report.ReportBlockedNotificationBody
import orchestrators.ReportBlockedNotificationOrchestrator
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import utils.silhouette.api.APIKeyEnv
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithRole

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ReportBlockedNotificationController(
    val silhouette: Silhouette[AuthEnv],
    val silhouetteAPIKey: Silhouette[APIKeyEnv],
    val orchestrator: ReportBlockedNotificationOrchestrator,
    controllerComponents: ControllerComponents
)(implicit
    val ec: ExecutionContext
) extends BaseController(controllerComponents) {

  def getAll() = SecuredAction(WithRole(UserRole.Professionnel)).async { implicit request =>
    orchestrator.findByUserId(request.identity.id).map(entities => Ok(Json.toJson(entities)))
  }

  def create() = SecuredAction(WithRole(UserRole.Professionnel)).async(parse.json) { implicit request =>
    request.body
      .validate[ReportBlockedNotificationBody]
      .fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        body =>
          orchestrator.createIfNotExists(request.identity.id, body.companyIds).map(entity => Ok(Json.toJson(entity)))
      )
  }

  def delete() = SecuredAction(WithRole(UserRole.Professionnel)).async(parse.json) { implicit request =>
    request.body
      .validate[ReportBlockedNotificationBody]
      .fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        body => orchestrator.delete(request.identity.id, body.companyIds).map(_ => Ok)
      )
  }
}
