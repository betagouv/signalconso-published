package controllers

import com.mohiva.play.silhouette.api.LoginEvent
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import config.EmailConfiguration
import models._
import orchestrators._
import play.api._
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import repositories.user.UserRepositoryInterface
import utils.EmailAddress
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithPermission

import java.util.UUID
import scala.concurrent.ExecutionContext

class AccountController(
    val silhouette: Silhouette[AuthEnv],
    userOrchestrator: UserOrchestrator,
    userRepository: UserRepositoryInterface,
    accessesOrchestrator: AccessesOrchestrator,
    proAccessTokenOrchestrator: ProAccessTokenOrchestrator,
    emailConfiguration: EmailConfiguration,
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  implicit val contactAddress = emailConfiguration.contactAddress
  implicit val ccrfEmailSuffix = emailConfiguration.ccrfEmailSuffix

  def fetchUser = SecuredAction.async { implicit request =>
    for {
      userOpt <- userRepository.get(request.identity.id)
    } yield userOpt
      .map { user =>
        Ok(Json.toJson(user))
      }
      .getOrElse(NotFound)
  }

  def activateAccount = UnsecuredAction.async(parse.json) { implicit request =>
    for {
      activationRequest <- request.parseBody[ActivationRequest]()
      _ <- activationRequest.companySiret match {
        case Some(siret) =>
          proAccessTokenOrchestrator.activateProUser(activationRequest.draftUser, activationRequest.token, siret)
        case None => accessesOrchestrator.activateDGCCRFUser(activationRequest.draftUser, activationRequest.token)
      }
    } yield NoContent

  }

  def sendDGCCRFInvitation = SecuredAction(WithPermission(UserPermission.inviteDGCCRF)).async(parse.json) {
    implicit request =>
      request
        .parseBody[EmailAddress](JsPath \ "email")
        .flatMap(email => accessesOrchestrator.sendDGCCRFInvitation(email).map(_ => Ok))
  }

  def fetchPendingDGCCRF = SecuredAction(WithPermission(UserPermission.inviteDGCCRF)).async { request =>
    accessesOrchestrator
      .listDGCCRFPendingToken(request.identity)
      .map(tokens => Ok(Json.toJson(tokens)))
  }

  def fetchDGCCRFUsers = SecuredAction(WithPermission(UserPermission.inviteDGCCRF)).async { _ =>
    for {
      users <- userRepository.list(UserRole.DGCCRF)
    } yield Ok(
      Json.toJson(
        users.map(u =>
          Json.obj(
            "email" -> u.email,
            "firstName" -> u.firstName,
            "lastName" -> u.lastName,
            "lastEmailValidation" -> u.lastEmailValidation
          )
        )
      )
    )
  }

  def fetchTokenInfo(token: String) = UnsecuredAction.async { _ =>
    accessesOrchestrator
      .fetchDGCCRFUserActivationToken(token)
      .map(token => Ok(Json.toJson(token)))
  }

  def validateEmail() = UnsecuredAction.async(parse.json) { implicit request =>
    for {
      token <- request.parseBody[String](JsPath \ "token")
      user <- accessesOrchestrator.validateDGCCRFEmail(token)
      authenticator <- silhouette.env.authenticatorService
        .create(LoginInfo(CredentialsProvider.ID, user.email.toString))
      _ = silhouette.env.eventBus.publish(LoginEvent(user, request))
      authToken <- silhouette.env.authenticatorService.init(authenticator)
    } yield Ok(Json.obj("token" -> authToken, "user" -> user))
  }

  def forceValidateEmail(email: String) =
    SecuredAction(WithPermission(UserPermission.inviteDGCCRF)).async { _ =>
      accessesOrchestrator.resetLastEmailValidation(EmailAddress(email)).map(_ => NoContent)
    }

  def edit(id: UUID) = SecuredAction.async(parse.json) { implicit request =>
    for {
      userUpdate <- request.parseBody[UserUpdate]()
      updatedUserOpt <- userOrchestrator.edit(id, userUpdate)
    } yield updatedUserOpt match {
      case Some(updatedUser) => Ok(Json.toJson(updatedUser))
      case _                 => NotFound
    }
  }
}
