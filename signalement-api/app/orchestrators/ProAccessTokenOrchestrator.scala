package orchestrators

import cats.implicits.catsSyntaxOption
import company.companydata.CompanyDataRepositoryInterface
import config.EmailConfiguration
import config.TokenConfiguration
import controllers.error.AppError._
import io.scalaland.chimney.dsl.TransformerOps
import models._
import models.event.Event
import models.event.Event.stringToDetailsJsValue
import models.token._
import models.token.TokenKind.CompanyJoin
import play.api.Logger
import repositories.accesstoken.AccessTokenRepositoryInterface
import repositories.company.CompanyRepositoryInterface
import repositories.companyaccess.CompanyAccessRepositoryInterface
import repositories.event.EventRepositoryInterface
import repositories.user.UserRepositoryInterface
import services.Email.ProCompanyAccessInvitation
import services.Email.ProNewCompanyAccess
import services.MailServiceInterface
import utils.Constants.ActionEvent
import utils.Constants.EventType
import utils.EmailAddress
import utils.FrontRoute
import utils.SIRET

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

class ProAccessTokenOrchestrator(
    userOrchestrator: UserOrchestratorInterface,
    companyRepository: CompanyRepositoryInterface,
    companyAccessRepository: CompanyAccessRepositoryInterface,
    companyDataRepository: CompanyDataRepositoryInterface,
    accessTokenRepository: AccessTokenRepositoryInterface,
    userRepository: UserRepositoryInterface,
    eventRepository: EventRepositoryInterface,
    mailService: MailServiceInterface,
    frontRoute: FrontRoute,
    emailConfiguration: EmailConfiguration,
    tokenConfiguration: TokenConfiguration
)(implicit val executionContext: ExecutionContext) {

  val logger = Logger(this.getClass)
  implicit val ccrfEmailSuffix = emailConfiguration.ccrfEmailSuffix
  implicit val timeout: akka.util.Timeout = 5.seconds

  def listProPendingToken(company: Company, user: User): Future[List[ProAccessToken]] =
    for {
      tokens <- accessTokenRepository.fetchPendingTokens(company)
    } yield tokens
      .map { token =>
        ProAccessToken(token.id, token.companyLevel, token.emailedTo, token.expirationDate, token.token, user.userRole)
      }

  def proFirstActivationCount(ticks: Option[Int]) =
    companyAccessRepository
      .proFirstActivationCount(ticks.getOrElse(12))
      .map(StatsOrchestrator.formatStatData(_, ticks.getOrElse(12)))

  def activateProUser(draftUser: DraftUser, token: String, siret: SIRET) = for {
    token <- fetchCompanyToken(token, siret)
    user <- userOrchestrator.createUser(draftUser, token, UserRole.Professionnel)
    _ <- bindPendingTokens(user)
    _ <- eventRepository.create(
      Event(
        UUID.randomUUID(),
        None,
        token.companyId,
        Some(user.id),
        OffsetDateTime.now,
        EventType.PRO,
        ActionEvent.ACCOUNT_ACTIVATION,
        stringToDetailsJsValue(s"Email du compte : ${token.emailedTo.getOrElse("")}")
      )
    )
  } yield ()

  private def bindPendingTokens(user: User) =
    accessTokenRepository
      .fetchPendingTokens(user.email)
      .flatMap(tokens =>
        Future.sequence(
          tokens.filter(_.companyId.isDefined).map(accessTokenRepository.createCompanyAccessAndRevokeToken(_, user))
        )
      )

  private def fetchCompanyToken(token: String, siret: SIRET): Future[AccessToken] = for {
    company <- companyRepository
      .findBySiret(siret)
      .flatMap(maybeCompany => maybeCompany.liftTo[Future](CompanySiretNotFound(siret)))
    accessToken <- accessTokenRepository
      .findValidToken(company, token)
      .flatMap(maybeToken => maybeToken.liftTo[Future](AccountActivationTokenNotFoundOrInvalid(token)))
  } yield accessToken

  def fetchCompanyUserActivationToken(siret: SIRET, token: String): Future[CompanyUserActivationToken] =
    for {
      company <- companyRepository.findBySiret(siret)
      maybeAccessToken <- company
        .map(accessTokenRepository.findValidToken(_, token))
        .getOrElse(Future.failed[Option[AccessToken]](CompanySiretNotFound(siret)))
      accessToken <- maybeAccessToken
        .map(Future.successful)
        .getOrElse(Future.failed[AccessToken](CompanyActivationTokenNotFound(token, siret)))
      emailTo <-
        accessToken.emailedTo
          .map(Future.successful)
          .getOrElse(Future.failed[EmailAddress](ServerError(s"Email should be defined for access token $token")))

    } yield accessToken
      .into[CompanyUserActivationToken]
      .withFieldConst(_.emailedTo, emailTo)
      .withFieldConst(_.companySiret, siret)
      .transform

  def addUserOrInvite(
      company: Company,
      email: EmailAddress,
      level: AccessLevel,
      invitedBy: Option[User]
  ): Future[Unit] =
    userRepository.findByLogin(email.value).flatMap {
      case Some(user) =>
        logger.debug("User with email already exist, creating access")
        addInvitedUserAndNotify(user, company, level, invitedBy)
      case None =>
        logger.debug("No user found for given email, sending invitation")
        sendInvitation(company, email, level, invitedBy)
    }

  def addUserOrInvite(
      sirets: List[SIRET],
      email: EmailAddress,
      level: AccessLevel,
      invitedBy: Option[User]
  ): Future[Unit] =
    for {
      companiesData <- Future.sequence(sirets.map(companyDataRepository.searchBySiret(_)))
      companies <-
        Future.sequence(companiesData.flatten.map { case (companyData, activity) =>
          companyRepository.getOrCreate(
            companyData.siret,
            companyData.toSearchResult(activity.map(_.label)).toCompany()
          )
        })
      _ <- Future.sequence(companies.map(company => addUserOrInvite(company, email, level, invitedBy)))
    } yield ()

  def addInvitedUserAndNotify(user: User, company: Company, level: AccessLevel, invitedBy: Option[User]) =
    for {
      _ <- accessTokenRepository.giveCompanyAccess(company, user, level)
      _ <- mailService.send(ProNewCompanyAccess(user.email, company, invitedBy))
      _ = logger.debug(s"User ${user.id} may now access company ${company.id}")
    } yield ()

  private def genInvitationToken(
      company: Company,
      level: AccessLevel,
      validity: Option[java.time.temporal.TemporalAmount],
      emailedTo: EmailAddress
  ): Future[String] =
    for {
      existingToken <- accessTokenRepository.fetchToken(company, emailedTo)
      _ <- existingToken
        .map { existingToken =>
          logger.debug("Found existing token for that user and company, updating existing token")
          accessTokenRepository.updateToken(existingToken, level, validity)
        }
        .getOrElse(Future(None))
      token <- existingToken
        .map(Future(_))
        .getOrElse {
          logger.debug("Creating user invitation token")
          accessTokenRepository.create(
            AccessToken.build(
              kind = CompanyJoin,
              token = UUID.randomUUID.toString,
              validity = tokenConfiguration.companyJoinDuration,
              companyId = Some(company.id),
              level = Some(level),
              emailedTo = Some(emailedTo)
            )
          )
        }
    } yield token.token

  def sendInvitation(company: Company, email: EmailAddress, level: AccessLevel, invitedBy: Option[User]): Future[Unit] =
    for {
      tokenCode <- genInvitationToken(company, level, tokenConfiguration.companyJoinDuration, email)
      _ <- mailService.send(
        ProCompanyAccessInvitation(
          recipient = email,
          company = company,
          invitationUrl = frontRoute.dashboard.Pro.register(company.siret, tokenCode),
          invitedBy = invitedBy
        )
      )
      _ = logger.debug(s"Token sent to ${email} for company ${company.id}")
    } yield ()

}
