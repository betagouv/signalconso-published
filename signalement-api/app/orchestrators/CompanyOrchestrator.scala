package orchestrators

import company.CompanySearchResult
import company.companydata.CompanyDataRepositoryInterface
import config.TaskConfiguration
import controllers.CompanyObjects.CompanyList
import controllers.error.AppError.CompanyNotFound
import io.scalaland.chimney.dsl.TransformerOps
import models.SearchCompanyIdentity.SearchCompanyIdentityId
import models.event.Event.stringToDetailsJsValue
import models._
import models.event.Event
import models.report.ReportFilter
import models.report.ReportStatus
import models.report.ReportTag
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import repositories.accesstoken.AccessTokenRepositoryInterface
import repositories.company.CompanyRepositoryInterface
import repositories.event.EventRepositoryInterface
import repositories.report.ReportRepositoryInterface
import repositories.website.WebsiteRepositoryInterface
import utils.Constants.ActionEvent
import utils.Constants.EventType
import utils.SIREN
import utils.SIRET

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CompanyOrchestrator(
    val companyRepository: CompanyRepositoryInterface,
    val companiesVisibilityOrchestrator: CompaniesVisibilityOrchestrator,
    val reportRepository: ReportRepositoryInterface,
    val companyDataRepository: CompanyDataRepositoryInterface,
    val websiteRepository: WebsiteRepositoryInterface,
    val accessTokenRepository: AccessTokenRepositoryInterface,
    val eventRepository: EventRepositoryInterface,
    val taskConfiguration: TaskConfiguration
)(implicit ec: ExecutionContext) {

  val logger: Logger = Logger(this.getClass)

  def create(companyCreation: CompanyCreation): Future[Company] =
    companyRepository
      .getOrCreate(companyCreation.siret, companyCreation.toCompany())

  def fetchHosts(companyId: UUID): Future[Seq[String]] =
    reportRepository.getHostsByCompany(companyId)

  def searchRegisteredById(
      companyIdFilter: UUID,
      user: User
  ): Future[PaginatedResult[CompanyWithNbReports]] =
    for {
      visibleByUserCompanyIdFilter <- user.userRole match {
        case UserRole.Professionnel =>
          restrictCompanyIdFilterOnProVisibility(user, companyIdFilter)
        case _ => Future.successful(SearchCompanyIdentityId(companyIdFilter))
      }
      companyIdFilter = CompanyRegisteredSearch(identity = Some(visibleByUserCompanyIdFilter))
      paginatedResults <- companyRepository
        .searchWithReportsCount(companyIdFilter, PaginatedSearch(None, None), user.userRole)

      companiesWithNbReports = paginatedResults.entities.map { case (company, count, responseCount) =>
        toCompanyWithNbReports(company, count, responseCount)
      }
    } yield paginatedResults.copy(entities = companiesWithNbReports)

  def searchRegistered(
      search: CompanyRegisteredSearch,
      paginate: PaginatedSearch,
      user: User
  ): Future[PaginatedResult[CompanyWithNbReports]] =
    companyRepository
      .searchWithReportsCount(search, paginate, user.userRole)
      .map(x =>
        x.copy(entities = x.entities.map { case (company, count, responseCount) =>
          toCompanyWithNbReports(company, count, responseCount)
        })
      )

  private def toCompanyWithNbReports(company: Company, count: Int, responseCount: Int) = {
    val responseRate: Float = if (count > 0) (responseCount.toFloat / count) * 100 else 0f
    company
      .into[CompanyWithNbReports]
      .withFieldConst(_.count, count)
      .withFieldConst(_.responseRate, responseRate.round)
      .transform
  }

  private def restrictCompanyIdFilterOnProVisibility(user: User, companyIdFilter: UUID) =
    companiesVisibilityOrchestrator
      .fetchVisibleCompanies(user)
      .map(_.map(_.company.id))
      .map { proVisibleCompanyIds =>
        if (proVisibleCompanyIds.contains(companyIdFilter)) {
          logger.debug(s"$companyIdFilter is visible by pro, allowing the filter ")
          SearchCompanyIdentityId(companyIdFilter)
        } else throw CompanyNotFound(companyIdFilter)
      }

  def getCompanyResponseRate(companyId: UUID, userRole: UserRole): Future[Int] = {

    val (tagFilter, statusFilter) = userRole match {
      case UserRole.Professionnel =>
        logger.debug("User is pro, filtering tag and status not visible by pro user")
        (ReportTag.ReportTagHiddenToProfessionnel, ReportStatus.statusVisibleByPro)
      case UserRole.Admin | UserRole.DGCCRF => (Seq.empty[ReportTag], Seq.empty[ReportStatus])
    }
    val responseReportsFilter =
      ReportFilter(
        companyIds = Seq(companyId),
        status = ReportStatus.statusWithProResponse,
        withoutTags = tagFilter
      )
    val totalReportsFilter =
      ReportFilter(companyIds = Seq(companyId), status = statusFilter, withoutTags = tagFilter)

    val totalReportsCount = reportRepository.count(totalReportsFilter)
    val responseReportsCount = reportRepository.count(responseReportsFilter)
    for {
      total <- totalReportsCount
      responses <- responseReportsCount
    } yield (responses.toFloat / total * 100).round
  }

  def searchCompany(q: String, postalCode: String): Future[List[CompanySearchResult]] = {
    logger.debug(s"searchCompany $postalCode $q")
    companyDataRepository
      .search(q, postalCode)
      .map(results => results.map(result => result._1.toSearchResult(result._2.map(_.label))))
  }

  def searchCompanyByIdentity(identity: String): Future[List[CompanySearchResult]] = {
    logger.debug(s"searchCompanyByIdentity $identity")

    (identity.replaceAll("\\s", "") match {
      case q if q.matches(SIRET.pattern) =>
        companyDataRepository.searchBySiretIncludingHeadOfficeWithActivity(SIRET.fromUnsafe(q))
      case q =>
        SIREN.pattern.r
          .findFirstIn(q)
          .map(siren =>
            for {
              headOffice <- companyDataRepository.searchHeadOfficeBySiren(SIREN(siren))
              companies <- headOffice
                .map(company => Future(List(company)))
                .getOrElse(companyDataRepository.searchBySiren(SIREN(siren)))
            } yield companies
          )
          .getOrElse(Future(List.empty))
    }).map(companiesWithActivity =>
      companiesWithActivity.map { case (company, activity) =>
        company.toSearchResult(activity.map(_.label))
      }
    )
  }

  def searchCompanyByWebsite(url: String): Future[Seq[CompanySearchResult]] = {
    logger.debug(s"searchCompaniesByHost $url")
    for {
      companiesByUrl <-
        websiteRepository.searchCompaniesByUrl(
          url
        )
      _ = logger.debug(s"Found ${companiesByUrl.map(t => (t._1.host, t._2.siret, t._2.name))}")
      results <- Future.sequence(companiesByUrl.map { case (website, company) =>
        companyDataRepository
          .searchBySiret(company.siret)
          .map { companies =>
            logger.debug(s"Found ${companies.length} entries in company database")
            companies.map { case (company, activity) =>
              company.toSearchResult(activity.map(_.label), website.isMarketplace)
            }
          }
      })
    } yield results.flatten
  }

  def companiesToActivate(): Future[List[JsObject]] =
    for {
      accesses <- accessTokenRepository.companiesToActivate()
      eventsMap <- eventRepository.fetchEvents(accesses.map { case (_, c) => c.id })
    } yield accesses
      .map { case (t, c) =>
        (
          c,
          t,
          eventsMap
            .get(c.id)
            .map(_.count(e => e.action == ActionEvent.POST_ACCOUNT_ACTIVATION_DOC))
            .getOrElse(0),
          eventsMap
            .get(c.id)
            .flatMap(_.find(e => e.action == ActionEvent.POST_ACCOUNT_ACTIVATION_DOC))
            .map(_.creationDate),
          eventsMap
            .get(c.id)
            .flatMap(_.find(e => e.action == ActionEvent.ACTIVATION_DOC_REQUIRED))
            .map(_.creationDate)
        )
      }
      .filter { case (_, _, noticeCount, lastNotice, lastRequirement) =>
        !lastNotice.exists(
          _.isAfter(
            lastRequirement.getOrElse(
              OffsetDateTime.now.minus(
                taskConfiguration.report.reportReminderByPostDelay
                  .multipliedBy(Math.min(noticeCount, 3))
              )
            )
          )
        )
      }
      .map { case (c, t, _, lastNotice, _) =>
        Json.obj(
          "company" -> Json.toJson(c),
          "lastNotice" -> lastNotice,
          "tokenCreation" -> t.creationDate
        )
      }

  def confirmContactByPostOnCompanyList(companyList: CompanyList, identity: UUID): Future[List[Event]] =
    Future
      .sequence(companyList.companyIds.map { companyId =>
        eventRepository.create(
          Event(
            UUID.randomUUID(),
            None,
            Some(companyId),
            Some(identity),
            OffsetDateTime.now(),
            EventType.PRO,
            ActionEvent.POST_ACCOUNT_ACTIVATION_DOC
          )
        )
      })

  def updateCompanyAddress(
      id: UUID,
      identity: UUID,
      companyAddressUpdate: CompanyAddressUpdate
  ): Future[Option[Company]] =
    for {
      company <- companyRepository.get(id)
      updatedCompany <-
        company
          .map(c => companyRepository.update(c.id, c.copy(address = companyAddressUpdate.address)).map(Some(_)))
          .getOrElse(Future(None))
      _ <- updatedCompany
        .filter(c => !company.map(_.address).contains(c.address))
        .map(c =>
          eventRepository.create(
            Event(
              UUID.randomUUID(),
              None,
              Some(c.id),
              Some(identity),
              OffsetDateTime.now(),
              EventType.PRO,
              ActionEvent.COMPANY_ADDRESS_CHANGE,
              stringToDetailsJsValue(s"Addresse précédente : ${company.map(_.address).getOrElse("")}")
            )
          )
        )
        .getOrElse(Future(None))
      _ <- updatedCompany
        .filter(_ => companyAddressUpdate.activationDocumentRequired)
        .map(c =>
          eventRepository.create(
            Event(
              UUID.randomUUID(),
              None,
              Some(c.id),
              Some(identity),
              OffsetDateTime.now(),
              EventType.PRO,
              ActionEvent.ACTIVATION_DOC_REQUIRED
            )
          )
        )
        .getOrElse(Future(None))
    } yield updatedCompany

  def handleUndeliveredDocument(
      siret: SIRET,
      identity: UUID,
      undeliveredDocument: UndeliveredDocument
  ): Future[Option[Event]] =
    for {
      company <- companyRepository.findBySiret(siret)
      event <- company
        .map(c =>
          eventRepository
            .create(
              Event(
                UUID.randomUUID(),
                None,
                Some(c.id),
                Some(identity),
                OffsetDateTime.now(),
                EventType.ADMIN,
                ActionEvent.ACTIVATION_DOC_RETURNED,
                stringToDetailsJsValue(s"Date de retour : ${undeliveredDocument.returnedDate
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
              )
            )
            .map(Some(_))
        )
        .getOrElse(Future(None))
    } yield event

}
