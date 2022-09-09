package controllers

import com.mohiva.play.silhouette.api.Silhouette
import models.Rating
import play.api.Logger
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import repositories.rating.RatingRepositoryInterface
import utils.silhouette.auth.AuthEnv

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class RatingController(
    ratingRepository: RatingRepositoryInterface,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit
    val ec: ExecutionContext
) extends BaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  def rate = UserAwareAction.async(parse.json) { implicit request =>
    request.body
      .validate[Rating]
      .fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        rating =>
          ratingRepository
            .create(
              rating.copy(id = Some(UUID.randomUUID()), creationDate = Some(OffsetDateTime.now()))
            )
            .map(rating => Ok(Json.toJson(rating)))
      )
  }
}
