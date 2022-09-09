package orchestrators

import controllers.error.AppError.ActivationCodeAlreadyUsed
import controllers.error.AppError.CompanyActivationCodeExpired
import controllers.error.AppError.CompanyActivationSiretOrCodeInvalid
import controllers.error.AppError.ServerError
import models.AccessLevel
import models.AccessToken
import models.Company
import models.User
import models.access.ActivationLinkRequest

import java.time.OffsetDateTime.now

import cats.implicits.catsSyntaxOption
import cats.implicits.toTraverseOps
import company.CompanyData
import company.companydata.CompanyDataRepositoryInterface
import models.UserRole.Admin
import models.UserRole.DGCCRF
import models.UserRole.Professionnel
import models.access.UserWithAccessLevel
import models.access.UserWithAccessLevel.toApi
import play.api.Logger
import repositories.accesstoken.AccessTokenRepositoryInterface
import repositories.company.CompanyRepositoryInterface
import repositories.companyaccess.CompanyAccessRepositoryInterface
import utils.SIRET

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CompanyAccessOrchestrator(
    companyDataRepository: CompanyDataRepositoryInterface,
    companyAccessRepository: CompanyAccessRepositoryInterface,
    val companyRepository: CompanyRepositoryInterface,
    val accessTokenRepository: AccessTokenRepositoryInterface,
    val accessesOrchestrator: ProAccessTokenOrchestrator
)(implicit val ec: ExecutionContext) {

  val logger = Logger(this.getClass)

  def sendActivationLink(siret: SIRET, activationLinkRequest: ActivationLinkRequest): Future[Unit] =
    for {
      company <- companyRepository
        .findBySiret(siret)
        .flatMap(maybeCompany =>
          maybeCompany.liftTo[Future] {
            logger.warn(s"Unable to activate company $siret, siret is unknown")
            CompanyActivationSiretOrCodeInvalid(siret)
          }
        )
      _ = logger.debug("Company found")
      token <-
        accessTokenRepository
          .fetchActivationToken(company.id)
          .flatMap(_.liftTo[Future] {
            logger.warn(s"No activation token found for siret $siret")
            CompanyActivationSiretOrCodeInvalid(siret)
          })
      _ = logger.debug("Token found")
      _ <- validateToken(token, activationLinkRequest, siret)
      _ = logger.debug("Token validated")
      _ <- accessesOrchestrator.addUserOrInvite(company, activationLinkRequest.email, AccessLevel.ADMIN, None)
    } yield ()

  def validateToken(
      accessToken: AccessToken,
      activationLinkRequest: ActivationLinkRequest,
      siret: SIRET
  ): Future[Unit] =
    if (activationLinkRequest.token != accessToken.token) {
      logger.warn(s"Unable to activate company $siret, code is not valid.")
      Future.failed(CompanyActivationSiretOrCodeInvalid(siret))
    } else if (!accessToken.valid) {
      logger.warn(s"Unable to activate company $siret, code has already been used.")
      Future.failed(ActivationCodeAlreadyUsed(activationLinkRequest.email))
    } else if (accessToken.expirationDate.exists(expiration => now isAfter expiration)) {
      logger.warn(s"Unable to activate company $siret, code has expired.")
      Future.failed(CompanyActivationCodeExpired(siret))
    } else Future.unit

  def listAccesses(company: Company, user: User): Future[List[UserWithAccessLevel]] =
    getHeadOffice(company).flatMap {

      case Some(headOffice) if headOffice.siret == company.siret =>
        logger.debug(s"$company is a head office, returning access for head office")
        for {
          userLevel <- companyAccessRepository.getUserLevel(company.id, user)
          access <- getHeadOfficeAccess(user, userLevel, company, editable = true)
        } yield access

      case maybeHeadOffice =>
        logger.debug(s"$company is not a head office, returning access for head office and subsidiaries")
        for {
          userAccessLevel <- companyAccessRepository.getUserLevel(company.id, user)
          subsidiaryUserAccess <- getSubsidiaryAccess(user, userAccessLevel, List(company), editable = true)
          maybeHeadOfficeCompany <- maybeHeadOffice match {
            case Some(headOffice) => companyRepository.findBySiret(headOffice.siret)
            case None             =>
              // No head office found in company database ( Company DB is not synced )
              Future.successful(None)
          }
          headOfficeAccess <- maybeHeadOfficeCompany.map { headOfficeCompany =>
            getHeadOfficeAccess(user, userAccessLevel, headOfficeCompany, editable = false)
          }.sequence
          _ = logger.debug(s"Removing duplicate access")
          filteredHeadOfficeAccess = headOfficeAccess.map(
            _.filterNot(a => subsidiaryUserAccess.exists(_.userId == a.userId))
          )
        } yield filteredHeadOfficeAccess.getOrElse(List.empty) ++ subsidiaryUserAccess
    }

  private def getHeadOffice(company: Company): Future[Option[CompanyData]] =
    companyDataRepository.getHeadOffice(company.siret).flatMap {
      case Nil =>
        logger.warn(s"No head office for siret ${company.siret}")
        Future.successful(None)
      case c :: Nil =>
        Future.successful(Some(c))
      case companies =>
        logger.error(s"Multiple head offices for siret ${company.siret} company data ids ${companies.map(_.id)} ")
        Future.failed(
          ServerError(s"Unexpected error when fetching head office for company with siret ${company.siret}")
        )
    }

  private def getHeadOfficeAccess(
      user: User,
      userLevel: AccessLevel,
      company: Company,
      editable: Boolean
  ): Future[List[UserWithAccessLevel]] =
    getUserAccess(user, userLevel, List(company), editable, isHeadOffice = true)

  private def getSubsidiaryAccess(
      user: User,
      userLevel: AccessLevel,
      companies: List[Company],
      editable: Boolean
  ): Future[List[UserWithAccessLevel]] =
    getUserAccess(user, userLevel, companies, editable, isHeadOffice = false)

  private def getUserAccess(
      user: User,
      userLevel: AccessLevel,
      companies: List[Company],
      editable: Boolean,
      isHeadOffice: Boolean
  ): Future[List[UserWithAccessLevel]] =
    for {
      companyAccess <- companyAccessRepository
        .fetchUsersWithLevel(companies.map(_.id))
    } yield (userLevel, user.userRole) match {
      case (_, Admin) =>
        logger.debug(s"Signal conso admin user : setting editable to true")
        companyAccess.map { case (user, level) => toApi(user, level, editable = true, isHeadOffice) }
      case (_, DGCCRF) =>
        logger.debug(s"Signal conso dgccrf user : setting editable to false")
        companyAccess.map { case (user, level) => toApi(user, level, editable = false, isHeadOffice) }
      case (AccessLevel.ADMIN, Professionnel) =>
        companyAccess.map {
          case (companyUser, level) if companyUser.id == user.id =>
            toApi(companyUser, level, editable = false, isHeadOffice)
          case (companyUser, level) =>
            toApi(companyUser, level, editable, isHeadOffice)
        }
      case (_, Professionnel) =>
        logger.debug(s"User PRO does not have admin access to company : setting editable to false")
        companyAccess.map { case (user, level) => toApi(user, level, editable = false, isHeadOffice) }
      case _ =>
        logger.error(s"User is not supposed to access this feature")
        List.empty[UserWithAccessLevel]
    }

}
