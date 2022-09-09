package controllers

import com.mohiva.play.silhouette.api.Silhouette
import orchestrators.AuthOrchestrator
import play.api._
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import utils.silhouette.auth.AuthEnv
import models.auth.PasswordChange
import models.auth.UserCredentials
import models.auth.UserLogin
import models.auth.UserPassword
import play.api.mvc.Action
import play.api.mvc.ControllerComponents

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class AuthController(
    val silhouette: Silhouette[AuthEnv],
    authOrchestrator: AuthOrchestrator,
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  implicit val timeout: akka.util.Timeout = 5.seconds

  def authenticate: Action[JsValue] = UnsecuredAction.async(parse.json) { implicit request =>
    for {
      userLogin <- request.parseBody[UserCredentials]()
      userSession <- authOrchestrator.login(userLogin, request)
    } yield Ok(Json.toJson(userSession))

  }

  def forgotPassword: Action[JsValue] = UnsecuredAction.async(parse.json) { implicit request =>
    for {
      userLogin <- request.parseBody[UserLogin]()
      _ <- authOrchestrator.forgotPassword(userLogin)
    } yield Ok
  }

  def resetPassword(token: UUID): Action[JsValue] = UnsecuredAction.async(parse.json) { implicit request =>
    for {
      userPassword <- request.parseBody[UserPassword]()
      _ <- authOrchestrator.resetPassword(token, userPassword)
    } yield NoContent
  }

  def changePassword = SecuredAction.async(parse.json) { implicit request =>
    for {
      updatePassword <- request.parseBody[PasswordChange]()
      _ <- authOrchestrator.changePassword(request.identity, updatePassword)
    } yield NoContent

  }

}
