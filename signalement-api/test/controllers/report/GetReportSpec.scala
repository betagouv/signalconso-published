package controllers.report

import java.time.OffsetDateTime
import java.util.UUID
import akka.util.Timeout
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.mohiva.play.silhouette.test._
import loader.SignalConsoComponents
import models._
import models.event.Event
import models.report.Gender
import models.report.Report
import models.report.ReportStatus
import models.report.ReportWithFiles
import models.report.WebsiteURL
import orchestrators.CompaniesVisibilityOrchestrator
import org.specs2.Spec
import org.specs2.concurrent.ExecutionEnv
import play.api.Application
import play.api.ApplicationLoader
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.mailer.Attachment
import play.api.mvc.Result
import play.api.test.Helpers.contentAsJson
import play.api.test._
import play.mvc.Http.Status
import repositories.event.EventFilter
import repositories.event.EventRepositoryInterface
import repositories.report.ReportRepositoryInterface
import repositories.reportfile.ReportFileRepositoryInterface
import services.MailerService
import utils.Constants.ActionEvent.ActionEventValue
import utils.Constants.ActionEvent
import utils.Constants.EventType
import utils.silhouette.auth.AuthEnv
import utils.AppSpec
import utils.EmailAddress
import utils.Fixtures
import utils.TestApp

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object GetReportByUnauthenticatedUser extends GetReportSpec {
  override def is =
    s2"""
         Given an unauthenticated user                                ${step { someLoginInfo = None }}
         When retrieving the report                                   ${step {
        someResult = Some(getReport(neverRequestedReport.id))
      }}
         Then user is not authorized                                  ${userMustBeUnauthorized()}
    """
}

object GetReportByAdminUser extends GetReportSpec {
  override def is =
    s2"""
         Given an authenticated admin user                            ${step { someLoginInfo = Some(adminLoginInfo) }}
         When retrieving the report                                   ${step {
        someResult = Some(getReport(neverRequestedReport.id))
      }}
         Then the report is rendered to the user as an Admin          ${reportMustBeRenderedForUserRole(
        neverRequestedReport,
        UserRole.Admin
      )}
    """
}

object GetReportByNotConcernedProUser extends GetReportSpec {
  override def is =
    s2"""
         Given an authenticated pro user which is not concerned by the report   ${step {
        someLoginInfo = Some(notConcernedProLoginInfo)
      }}
         When getting the report                                                ${step {
        someResult = Some(getReport(neverRequestedReport.id))
      }}
         Then the report is not found                                           ${reportMustBeNotFound()}
    """
}

object GetReportByConcernedProUserFirstTime extends GetReportSpec {
  override def is =
    s2"""
         Given an authenticated pro user which is concerned by the report       ${step {
        someLoginInfo = Some(concernedProLoginInfo)
      }}
         When retrieving the report for the first time                          ${step {
        someResult = Some(getReport(neverRequestedReport.id))
      }}
         Then an event "ENVOI_SIGNALEMENT is created                            ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.REPORT_READING_BY_PRO
      )}
         And the report reportStatusList is updated to "SIGNALEMENT_TRANSMIS"   ${reportStatusMustMatch(
        neverRequestedReport.id,
        ReportStatus.Transmis
      )}
         And a mail is sent to the consumer                                     ${mailMustHaveBeenSent(
        neverRequestedReport.email,
        "L'entreprise a pris connaissance de votre signalement",
        views.html.mails.consumer.reportTransmission(neverRequestedReport).toString,
        attachementService.attachmentSeqForWorkflowStepN(3)
      )}
         And the report is rendered to the user as a Professional               ${reportMustBeRenderedForUserRole(
        neverRequestedReport.copy(status = ReportStatus.Transmis),
        UserRole.Professionnel
      )}
      """
}

object GetFinalReportByConcernedProUserFirstTime extends GetReportSpec {
  override def is =
    s2"""
         Given an authenticated pro user which is concerned by the report       ${step {
        someLoginInfo = Some(concernedProLoginInfo)
      }}
         When retrieving a final report for the first time                      ${step {
        someResult = Some(getReport(neverRequestedFinalReport.id))
      }}
         Then an event "ENVOI_SIGNALEMENT is created                            ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.REPORT_READING_BY_PRO
      )}
         And the report reportStatusList is not updated                         ${reportStatusMustMatch(
        neverRequestedFinalReport.id,
        neverRequestedFinalReport.status
      )}
         And no mail is sent                                                    ${mailMustNotHaveBeenSent()}
         And the report is rendered to the user as a Professional               ${reportMustBeRenderedForUserRole(
        neverRequestedFinalReport,
        UserRole.Professionnel
      )}
    """
}

object GetReportByConcernedProUserNotFirstTime extends GetReportSpec {
  override def is =
    s2"""
         Given an authenticated pro user which is concerned by the report       ${step {
        someLoginInfo = Some(concernedProLoginInfo)
      }}
         When retrieving the report not for the first time                      ${step {
        someResult = Some(getReport(alreadyRequestedReport.id))
      }}
         Then no event is created                                               ${eventMustNotHaveBeenCreated()}
         And the report reportStatusList is not updated                         ${reportStatusMustMatch(
        alreadyRequestedReport.id,
        alreadyRequestedReport.status
      )}
         And no mail is sent                                                    ${mailMustNotHaveBeenSent()}
         And the report is rendered to the user as a Professional               ${reportMustBeRenderedForUserRole(
        alreadyRequestedReport,
        UserRole.Professionnel
      )}

    """
}

trait GetReportSpec extends Spec with GetReportContext {

  import org.specs2.matcher.MatchersImplicits._

  implicit val ee = ExecutionEnv.fromGlobalExecutionContext

  implicit val timeout: Timeout = 30.seconds

  var someLoginInfo: Option[LoginInfo] = None
  var someResult: Option[Result] = None

  def getReport(reportUUID: UUID) =
    Await.result(
      components.reportController
        .getReport(reportUUID)
        .apply(someLoginInfo.map(FakeRequest().withAuthenticator[AuthEnv](_)).getOrElse(FakeRequest())),
      Duration.Inf
    )

  def userMustBeUnauthorized() =
    someResult must beSome and someResult.get.header.status === Status.UNAUTHORIZED

  def reportMustBeNotFound() =
    someResult must beSome and someResult.get.header.status === Status.NOT_FOUND

  def reportMustBeRenderedForUserRole(report: Report, userRole: UserRole) = {
    implicit val someUserRole = Some(userRole)
    someResult must beSome and contentAsJson(Future.successful(someResult.get)) === Json.toJson(
      ReportWithFiles(report, List.empty)
    )
  }

  def mailMustHaveBeenSent(
      recipient: EmailAddress,
      subject: String,
      bodyHtml: String,
      attachments: Seq[Attachment] = attachementService.defaultAttachments
  ) =
    there was one(mailerService)
      .sendEmail(
        emailConfiguration.from,
        Seq(recipient),
        Nil,
        subject,
        bodyHtml,
        attachments
      )

  def mailMustNotHaveBeenSent() =
    there was no(components.mailer)
      .sendEmail(
        any[EmailAddress],
        any[Seq[EmailAddress]],
        any[Seq[EmailAddress]],
        anyString,
        anyString,
        any[Seq[Attachment]]
      )

  def reportStatusMustMatch(id: UUID, status: ReportStatus) = {

    val maybeReport = Await.result(
      mockReportRepository.get(id),
      Duration.Inf
    )
    maybeReport.map(_ => status) mustEqual (Some(status))
  }

  def reportMustNotHaveBeenUpdated() =
    there was no(mockReportRepository).update(any[UUID], any[Report])

  def eventMustHaveBeenCreatedWithAction(action: ActionEventValue) =
    there was one(mockEventRepository).create(argThat(eventActionMatcher(action)))

  def eventActionMatcher(action: ActionEventValue): org.specs2.matcher.Matcher[Event] = { event: Event =>
    (action == event.action, s"action doesn't match ${action}")
  }

  def eventMustNotHaveBeenCreated() =
    there was no(mockEventRepository).create(any[Event])

}

trait GetReportContext extends AppSpec {

  implicit val ec = ExecutionContext.global

  val siretForConcernedPro = Fixtures.genSiret().sample.get
  // TODO Check why not used

  val siretForNotConcernedPro = Fixtures.genSiret().sample.get

  val company = Fixtures.genCompany.sample.get
  val companyData = Fixtures.genCompanyData(Some(company))

  val address = Fixtures.genAddress()

  private val valueGender: Option[Gender] = Fixtures.genGender.sample.get
  val neverRequestedReport = Report(
    gender = valueGender,
    category = "category",
    subcategories = List("subcategory"),
    details = List(),
    companyId = Some(company.id),
    companyName = Some("companyName"),
    companyAddress = address.sample.get,
    companySiret = Some(company.siret),
    companyActivityCode = company.activityCode,
    websiteURL = WebsiteURL(None, None),
    phone = None,
    firstName = "firstName",
    lastName = "lastName",
    email = EmailAddress("email"),
    contactAgreement = true,
    employeeConsumer = false,
    status = ReportStatus.TraitementEnCours
  )

  val neverRequestedFinalReport = Report(
    gender = valueGender,
    category = "category",
    subcategories = List("subcategory"),
    details = List(),
    companyId = Some(company.id),
    companyName = Some("companyName"),
    companyAddress = address.sample.get,
    companySiret = Some(company.siret),
    companyActivityCode = company.activityCode,
    websiteURL = WebsiteURL(None, None),
    phone = None,
    firstName = "firstName",
    lastName = "lastName",
    email = EmailAddress("email"),
    contactAgreement = true,
    employeeConsumer = false,
    status = ReportStatus.ConsulteIgnore
  )

  val alreadyRequestedReport = Report(
    gender = valueGender,
    category = "category",
    subcategories = List("subcategory"),
    details = List(),
    companyId = Some(company.id),
    companyName = Some("companyName"),
    companyAddress = address.sample.get,
    companySiret = Some(company.siret),
    companyActivityCode = company.activityCode,
    websiteURL = WebsiteURL(None, None),
    phone = None,
    firstName = "firstName",
    lastName = "lastName",
    email = EmailAddress("email"),
    contactAgreement = true,
    employeeConsumer = false,
    status = ReportStatus.Transmis
  )

  val adminUser = Fixtures.genAdminUser.sample.get
  val adminLoginInfo = LoginInfo(CredentialsProvider.ID, adminUser.email.value)

  val concernedProUser = Fixtures.genProUser.sample.get
  val concernedProLoginInfo = LoginInfo(CredentialsProvider.ID, concernedProUser.email.value)

  val notConcernedProUser = Fixtures.genProUser.sample.get
  val notConcernedProLoginInfo = LoginInfo(CredentialsProvider.ID, notConcernedProUser.email.value)

  implicit val env: Environment[AuthEnv] = new FakeEnvironment[AuthEnv](
    Seq(
      adminLoginInfo -> adminUser,
      concernedProLoginInfo -> concernedProUser,
      notConcernedProLoginInfo -> notConcernedProUser
    )
  )

  val mockReportRepository = new ReportRepositoryMock()
  mockReportRepository.create(neverRequestedReport)
  mockReportRepository.create(neverRequestedFinalReport)
  mockReportRepository.create(alreadyRequestedReport)

  val mockReportFileRepository = mock[ReportFileRepositoryInterface]
  val mockEventRepository = mock[EventRepositoryInterface]
  val mockMailerService = mock[MailerService]
  val mockCompaniesVisibilityOrchestrator = mock[CompaniesVisibilityOrchestrator]

  mockCompaniesVisibilityOrchestrator.fetchVisibleCompanies(any[User]) answers { (pro: Any) =>
    Future(
      if (pro.asInstanceOf[User].id == concernedProUser.id) List(CompanyWithAccess(company, AccessLevel.ADMIN))
      else List()
    )
  }

  mockReportFileRepository.retrieveReportFiles(any[UUID]) returns Future(List.empty)

  mockEventRepository.create(any[Event]) answers { (event: Any) => Future(event.asInstanceOf[Event]) }
  mockEventRepository.getEvents(neverRequestedReport.id, EventFilter(None)) returns Future(List.empty)
  mockEventRepository.getEvents(neverRequestedFinalReport.id, EventFilter(None)) returns Future(List.empty)
  mockEventRepository.getEvents(alreadyRequestedReport.id, EventFilter(None)) returns Future(
    List(
      Event(
        UUID.randomUUID(),
        Some(alreadyRequestedReport.id),
        Some(company.id),
        Some(concernedProUser.id),
        OffsetDateTime.now(),
        EventType.PRO,
        ActionEvent.REPORT_READING_BY_PRO
      )
    )
  )

  class FakeApplicationLoader extends ApplicationLoader {
    var components: SignalConsoComponents = _

    override def load(context: ApplicationLoader.Context): Application = {
      components = new SignalConsoComponents(context) {

        override def authEnv: Environment[AuthEnv] = env
        override def reportRepository: ReportRepositoryInterface = mockReportRepository
        override def reportFileRepository: ReportFileRepositoryInterface = mockReportFileRepository
        override def mailer: MailerService = mockMailerService
        override def eventRepository: EventRepositoryInterface = mockEventRepository
        override def companiesVisibilityOrchestrator: CompaniesVisibilityOrchestrator =
          mockCompaniesVisibilityOrchestrator

        override def configuration: Configuration = Configuration(
          "play.evolutions.enabled" -> false,
          "slick.dbs.default.db.connectionPool" -> "disabled",
          "play.mailer.mock" -> true
        ).withFallback(
          super.configuration
        )

      }
      components.application
    }

  }

  val appLoader = new FakeApplicationLoader()
  val app: Application = TestApp.buildApp(appLoader)
  val components: SignalConsoComponents = appLoader.components

  lazy val mailerService = components.mailer
  lazy val attachementService = components.attachmentService
  lazy val mailService = components.mailService

}
