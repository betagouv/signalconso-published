package controllers

import com.mohiva.play.silhouette.api.Silhouette
import io.scalaland.chimney.dsl.TransformerOps
import models.report.review.ResponseConsumerReviewApi
import orchestrators.ReportConsumerReviewOrchestrator
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import utils.silhouette.auth.AuthEnv

import java.util.UUID
import scala.concurrent.ExecutionContext

class ReportConsumerReviewController(
    reportConsumerReviewOrchestrator: ReportConsumerReviewOrchestrator,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  def reviewOnReportResponse(reportUUID: UUID): Action[JsValue] = UnsecuredAction.async(parse.json) {
    implicit request =>
      for {
        review <- request.parseBody[ResponseConsumerReviewApi]()
        _ <- reportConsumerReviewOrchestrator.handleReviewOnReportResponse(reportUUID, review)
      } yield Ok
  }

  def getReview(reportUUID: UUID): Action[AnyContent] = SecuredAction.async { _ =>
    logger.debug(s"Get report response review for report id : ${reportUUID}")
    for {
      maybeResponseConsumerReview <- reportConsumerReviewOrchestrator.find(reportUUID)
      maybeResponseConsumerReviewApi = maybeResponseConsumerReview.map(_.into[ResponseConsumerReviewApi].transform)
    } yield Ok(Json.toJson(maybeResponseConsumerReviewApi))

  }

}
