package orchestrators

import cats.implicits.catsSyntaxMonadError
import cats.implicits.catsSyntaxOption
import controllers.error.AppError.CannotDeleteWebsite
import controllers.error.AppError.MalformedHost
import controllers.error.AppError.WebsiteHostIsAlreadyIdentified
import controllers.error.AppError.WebsiteNotFound
import controllers.error.AppError.WebsiteNotIdentified
import models.Company
import models.CompanyCreation
import models.PaginatedResult
import models.User
import models.investigation.InvestigationStatus.NotProcessed
import models.investigation.DepartmentDivision
import models.investigation.DepartmentDivisionOptionValue
import models.investigation.InvestigationStatus
import models.investigation.Practice
import models.investigation.WebsiteInvestigationApi
import models.website.IdentificationStatus._
import models.website.WebsiteCompanyReportCount.toApi
import models.website._
import play.api.Logger
import repositories.company.CompanyRepositoryInterface
import repositories.website.WebsiteRepositoryInterface
import utils.Country
import utils.DateUtils
import utils.URL

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class WebsitesOrchestrator(
    val repository: WebsiteRepositoryInterface,
    val companyRepository: CompanyRepositoryInterface
)(implicit
    ec: ExecutionContext
) {

  val logger: Logger = Logger(this.getClass)

  def searchByHost(host: String): Future[Seq[Country]] =
    for {
      validHost <- URL(host).getHost.liftTo[Future](MalformedHost(host))
      websites <- repository.searchValidWebsiteCountryAssociationByHost(validHost)
    } yield websites
      .flatMap(_.companyCountry)
      .map(Country.fromName)

  def getWebsiteCompanyCount(
      maybeHost: Option[String],
      identificationStatus: Option[Seq[IdentificationStatus]],
      maybeOffset: Option[Long],
      maybeLimit: Option[Int],
      investigationStatusFilter: Option[Seq[InvestigationStatus]],
      practiceFilter: Option[Seq[Practice]],
      attributionFilter: Option[Seq[DepartmentDivision]],
      start: Option[OffsetDateTime],
      end: Option[OffsetDateTime],
      hasAssociation: Option[Boolean]
  ): Future[PaginatedResult[WebsiteCompanyReportCount]] =
    for {
      websites <- repository.listWebsitesCompaniesByReportCount(
        maybeHost,
        identificationStatus,
        maybeOffset,
        maybeLimit,
        investigationStatusFilter,
        practiceFilter,
        attributionFilter,
        start,
        end,
        hasAssociation
      )
      _ = logger.debug("Website company report fetched")
      websitesWithCount = websites.copy(entities = websites.entities.map(toApi))
    } yield websitesWithCount

  def updateWebsiteIdentificationStatus(
      websiteId: WebsiteId,
      newIdentificationStatus: IdentificationStatus
  ): Future[Website] = for {
    website <- findWebsite(websiteId)
    _ = if (website.companyCountry.isEmpty && website.companyId.isEmpty) {
      throw WebsiteNotIdentified(website.host)
    }
    _ <-
      if (newIdentificationStatus == Identified) { validateAndCleanAssociation(website) }
      else Future.unit
    _ = logger.debug(s"Updating website kind to ${newIdentificationStatus}")
    updatedWebsite <- update(website.copy(identificationStatus = newIdentificationStatus))
  } yield updatedWebsite

  private def validateAndCleanAssociation(website: Website) = {
    logger.debug(s"Validating that ${website.host} has not already been validated")
    for {
      _ <- repository
        .searchValidAssociationByHost(website.host)
        .ensure(WebsiteHostIsAlreadyIdentified(website.host))(_.isEmpty)
      _ = logger.debug(s"Removing other websites with the same host : ${website.host}")
      _ <- repository.removeOtherNonIdentifiedWebsitesWithSameHost(website)
    } yield website
  }

  def updateCompany(websiteId: WebsiteId, companyToAssign: CompanyCreation, user: User): Future[WebsiteAndCompany] =
    for {
      company <- {
        logger.debug(s"Updating website (id ${websiteId}) with company siret : ${companyToAssign.siret}")
        getOrCreateCompay(companyToAssign)
      }
      website <- findWebsite(websiteId)
      websiteToUpdate = website.copy(
        companyCountry = None,
        companyId = Some(company.id)
      )
      updatedWebsite <- updateIdentification(websiteToUpdate, user)
    } yield WebsiteAndCompany.toApi(updatedWebsite, Some(company))

  def updateCompanyCountry(websiteId: WebsiteId, companyCountry: String, user: User): Future[WebsiteAndCompany] = for {
    website <- {
      logger.debug(s"Updating website (id ${websiteId.value}) with company country : ${companyCountry}")
      findWebsite(websiteId)
    }
    websiteToUpdate = website.copy(
      companyCountry = Some(companyCountry),
      companyId = None
    )
    updatedWebsite <- updateIdentification(websiteToUpdate, user)
  } yield WebsiteAndCompany.toApi(updatedWebsite, maybeCompany = None)

  private def updateIdentification(website: Website, user: User) = {
    logger.debug(s"Removing other websites with the same host : ${website.host}")
    for {
      _ <- repository
        .removeOtherNonIdentifiedWebsitesWithSameHost(website)
      _ = logger.debug(s"updating identification status when Admin is updating identification")
      websiteToUpdate = if (user.isAdmin) website.copy(identificationStatus = Identified) else website
      _ = logger.debug(s"Website to update : ${websiteToUpdate}")
      updatedWebsite <- update(websiteToUpdate)
      _ = logger.debug(s"Website company country successfully updated")
    } yield updatedWebsite
  }

  def delete(websiteId: WebsiteId): Future[Unit] =
    for {
      maybeWebsite <- repository.get(websiteId)
      website <- maybeWebsite.liftTo[Future](WebsiteNotFound(websiteId))
      isWebsiteUnderInvestigation = website.attribution.isEmpty && website.investigationStatus != NotProcessed
      isWebsiteIdentified = website.identificationStatus == Identified
      _ <-
        if (isWebsiteIdentified || isWebsiteUnderInvestigation) {
          logger.debug(s"Cannot delete identified / under investigation website")
          Future.failed(CannotDeleteWebsite(website.host))
        } else {
          Future.unit
        }
      _ <- repository.delete(websiteId)
    } yield ()

  def updateInvestigation(investigationApi: WebsiteInvestigationApi): Future[Website] = for {
    maybeWebsite <- repository.get(investigationApi.id)
    website <- maybeWebsite.liftTo[Future](WebsiteNotFound(investigationApi.id))
    _ = logger.debug("Update investigation")
    updatedWebsite = investigationApi.copyToDomain(website)
    website <- update(updatedWebsite)
  } yield website

  def listDepartmentDivision(): Seq[DepartmentDivisionOptionValue] =
    DepartmentDivision.values.map(d => DepartmentDivisionOptionValue(d.entryName, d.name))

  def listInvestigationStatus(): Seq[InvestigationStatus] = InvestigationStatus.values

  def listPractice(): Seq[Practice] = Practice.values

  private[this] def getOrCreateCompay(companyCreate: CompanyCreation): Future[Company] = companyRepository
    .getOrCreate(
      companyCreate.siret,
      companyCreate.toCompany()
    )

  private[this] def findWebsite(websiteId: WebsiteId): Future[Website] = for {
    maybeWebsite <- {
      logger.debug(s"Searching for website with id : $websiteId")
      repository.get(websiteId)
    }
    website <- maybeWebsite.liftTo[Future](WebsiteNotFound(websiteId))
    _ = logger.debug(s"Found website")
  } yield website

  private def update(website: Website) = repository.update(website.id, website.copy(lastUpdated = OffsetDateTime.now()))

  def fetchUnregisteredHost(
      host: Option[String],
      start: Option[String],
      end: Option[String]
  ): Future[List[WebsiteHostCount]] =
    repository
      .getUnkonwnReportCountByHost(host, DateUtils.parseDate(start), DateUtils.parseDate(end))
      .map(_.map { case (host, count) => WebsiteHostCount(host, count) })

}
