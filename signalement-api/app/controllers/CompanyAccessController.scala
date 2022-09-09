package controllers

import com.mohiva.play.silhouette.api.Silhouette
import models._
import models.access.ActivationLinkRequest
import orchestrators.CompaniesVisibilityOrchestrator
import orchestrators.CompanyAccessOrchestrator
import orchestrators.ProAccessTokenOrchestrator
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import repositories.accesstoken.AccessTokenRepositoryInterface
import repositories.company.CompanyRepositoryInterface
import repositories.companyaccess.CompanyAccessRepositoryInterface
import repositories.user.UserRepositoryInterface
import utils.EmailAddress
import utils.SIRET
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithRole

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CompanyAccessController(
    val userRepository: UserRepositoryInterface,
    val companyRepository: CompanyRepositoryInterface,
    val companyAccessRepository: CompanyAccessRepositoryInterface,
    val accessTokenRepository: AccessTokenRepositoryInterface,
    val accessesOrchestrator: ProAccessTokenOrchestrator,
    val companyVisibilityOrch: CompaniesVisibilityOrchestrator,
    val companyAccessOrchestrator: CompanyAccessOrchestrator,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseCompanyController(controllerComponents) {

  val logger: Logger = Logger(this.getClass())

  def listAccesses(siret: String) = withCompany(siret, List(AccessLevel.ADMIN)).async { implicit request =>
    companyAccessOrchestrator
      .listAccesses(request.company, request.identity)
      .map(userWithAccessLevel => Ok(Json.toJson(userWithAccessLevel)))
  }

  def countAccesses(siret: String) = withCompany(siret, List(AccessLevel.ADMIN, AccessLevel.MEMBER)).async {
    implicit request =>
      companyAccessOrchestrator
        .listAccesses(request.company, request.identity)
        .map(_.length)
        .map(count => Ok(Json.toJson(count)))
  }

  def myCompanies = SecuredAction.async { implicit request =>
    companyAccessRepository
      .fetchCompaniesWithLevel(request.identity)
      .map(companies => Ok(Json.toJson(companies)))
  }

  def updateAccess(siret: String, userId: UUID) = withCompany(siret, List(AccessLevel.ADMIN)).async {
    implicit request =>
      request.body.asJson
        .map(json => (json \ "level").as[AccessLevel])
        .map(level =>
          for {
            user <- userRepository.get(userId)
            _ <- user
              .map(u => companyAccessRepository.createUserAccess(request.company.id, u.id, level))
              .getOrElse(Future(()))
          } yield if (user.isDefined) Ok else NotFound
        )
        .getOrElse(Future(NotFound))
  }

  def removeAccess(siret: String, userId: UUID) = withCompany(siret, List(AccessLevel.ADMIN)).async {
    implicit request =>
      for {
        user <- userRepository.get(userId)
        _ <- user
          .map(u => companyAccessRepository.createUserAccess(request.company.id, u.id, AccessLevel.NONE))
          .getOrElse(Future(()))
      } yield if (user.isDefined) Ok else NotFound
  }

  case class AccessInvitation(email: EmailAddress, level: AccessLevel)

  def sendInvitation(siret: String) = withCompany(siret, List(AccessLevel.ADMIN)).async(parse.json) {
    implicit request =>
      implicit val reads = Json.reads[AccessInvitation]
      request.body
        .validate[AccessInvitation]
        .fold(
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          invitation =>
            accessesOrchestrator
              .addUserOrInvite(request.company, invitation.email, invitation.level, Some(request.identity))
              .map(_ => Ok)
        )
  }

  case class AccessInvitationList(email: EmailAddress, level: AccessLevel, sirets: List[SIRET])

  def sendGroupedInvitations = SecuredAction(WithRole(UserRole.Admin)).async(parse.json) { implicit request =>
    implicit val reads = Json.reads[AccessInvitationList]
    request.body
      .validate[AccessInvitationList]
      .fold(
        errors => {
          logger.error(s"$errors")
          Future.successful(BadRequest(JsError.toJson(errors)))
        },
        invitations =>
          accessesOrchestrator
            .addUserOrInvite(invitations.sirets, invitations.email, invitations.level, Some(request.identity))
            .map(_ => Ok)
      )
  }

  def listPendingTokens(siret: String) = withCompany(siret, List(AccessLevel.ADMIN)).async { implicit request =>
    accessesOrchestrator
      .listProPendingToken(request.company, request.identity)
      .map(tokens => Ok(Json.toJson(tokens)))
  }

  def removePendingToken(siret: String, tokenId: UUID) = withCompany(siret, List(AccessLevel.ADMIN)).async {
    implicit request =>
      for {
        token <- accessTokenRepository.getToken(request.company, tokenId)
        _ <- token.map(accessTokenRepository.invalidateToken(_)).getOrElse(Future(()))
      } yield if (token.isDefined) Ok else NotFound
  }

  def fetchTokenInfo(siret: String, token: String) = UnsecuredAction.async { _ =>
    accessesOrchestrator
      .fetchCompanyUserActivationToken(SIRET.fromUnsafe(siret), token)
      .map(token => Ok(Json.toJson(token)))
  }

  def sendActivationLink(siret: String) = UnsecuredAction.async(parse.json) { implicit request =>
    for {
      activationLinkRequest <- request.parseBody[ActivationLinkRequest]()
      _ <- companyAccessOrchestrator.sendActivationLink(SIRET.fromUnsafe(siret), activationLinkRequest)
    } yield Ok

  }

  case class AcceptTokenRequest(token: String)

  def acceptToken(siret: String) = SecuredAction.async(parse.json) { implicit request =>
    implicit val reads = Json.reads[AcceptTokenRequest]
    request.body
      .validate[AcceptTokenRequest]
      .fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        acceptTokenRequest =>
          for {
            company <- companyRepository.findBySiret(SIRET.fromUnsafe(siret))
            token <- company
              .map(
                accessTokenRepository
                  .findValidToken(_, acceptTokenRequest.token)
                  .map(
                    _.filter(
                      _.emailedTo.filter(_ != request.identity.email).isEmpty
                    )
                  )
              )
              .getOrElse(Future(None))
            applied <- token
              .map(t =>
                accessTokenRepository
                  .createCompanyAccessAndRevokeToken(t, request.identity)
              )
              .getOrElse(Future(false))
          } yield if (applied) Ok else NotFound
      )
  }

  def proFirstActivationCount(ticks: Option[Int]) = SecuredAction.async(parse.empty) { _ =>
    accessesOrchestrator.proFirstActivationCount(ticks).map(x => Ok(Json.toJson(x)))
  }

}
