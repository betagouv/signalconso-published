package controllers.report

import java.net.URI
import java.time.OffsetDateTime
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.mohiva.play.silhouette.test._
import models._
import models.event.Event
import models.report.Report
import models.report.ReportFile
import models.report.ReportFileOrigin
import models.report.ReportResponse
import models.report.ReportResponseType
import models.report.ReportStatus
import models.report.reportfile.ReportFileId
import org.specs2.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import play.api.libs.json.Json
import play.api.libs.mailer.Attachment
import play.api.mvc.Result
import play.api.test._
import play.mvc.Http.Status
import utils.Constants.ActionEvent.ActionEventValue
import utils.Constants.ActionEvent
import utils.silhouette.auth.AuthEnv
import utils.AppSpec
import utils.EmailAddress
import utils.Fixtures
import utils.SIREN
import utils.TestApp

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ReportResponseByUnauthenticatedUser(implicit ee: ExecutionEnv) extends ReportResponseSpec {
  override def is =
    s2"""
         Given an unauthenticated user                                ${step { someLoginInfo = None }}
         When post a response                                         ${step {
        someResult = Some(postReportResponse(reportResponseAccepted))
      }}
         Then result status is not authorized                         ${resultStatusMustBe(Status.UNAUTHORIZED)}
    """
}

class ReportResponseByNotConcernedProUser(implicit ee: ExecutionEnv) extends ReportResponseSpec {
  override def is =
    s2"""
         Given an authenticated pro user which is not concerned by the report   ${step {
        someLoginInfo = Some(notConcernedProLoginInfo)
      }}
         When post a response                                                   ${step {
        someResult = Some(postReportResponse(reportResponseAccepted))
      }}
         Then result status is not found                                        ${resultStatusMustBe(Status.NOT_FOUND)}
    """
}

class ReportResponseProAnswer(implicit ee: ExecutionEnv) extends ReportResponseSpec {
  override def is =
    s2"""
        Given an authenticated pro user which is concerned by the report         ${step {
        someLoginInfo = Some(concernedProLoginInfo)
      }}
        When post a response with type "ACCEPTED"                                ${step {
        someResult = Some(postReportResponse(reportResponseAccepted))
      }}
        Then an event "REPORT_PRO_RESPONSE" is created                           ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.REPORT_PRO_RESPONSE
      )}
        And an event "EMAIL_CONSUMER_REPORT_RESPONSE" is created                 ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_CONSUMER_REPORT_RESPONSE
      )}
        And an event "EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT" is created              ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT
      )}
        And the response files are attached to the report                        ${reportFileMustHaveBeenAttachedToReport()}
        And the report reportStatusList is updated to "ReportStatus.PromesseAction"          ${reportMustHaveBeenUpdatedWithStatus(
        ReportStatus.PromesseAction
      )}
        And an acknowledgment email is sent to the consumer                      ${mailMustHaveBeenSent(
        reportFixture.email,
        "L'entreprise a répondu à votre signalement, donnez nous votre avis sur sa réponse",
        views.html.mails.consumer
          .reportToConsumerAcknowledgmentPro(
            report,
            reportResponseAccepted,
            frontRoute.dashboard.reportReview(report.id.toString)
          )
          .toString,
        attachementService.ConsumerProResponseNotificationAttachement
      )}
        And an acknowledgment email is sent to the professional                  ${mailMustHaveBeenSent(
        concernedProUser.email,
        "Votre réponse au signalement",
        views.html.mails.professional.reportAcknowledgmentPro(reportResponseAccepted, concernedProUser).toString
      )}
    """
}

class ReportResponseHeadOfficeProAnswer(implicit ee: ExecutionEnv) extends ReportResponseSpec {

  override def is =
    s2"""
        Given an authenticated pro user which have rights on head office         ${step {
        someLoginInfo = Some(concernedHeadOfficeProLoginInfo)
      }}
        When post a response with type "ACCEPTED"                                ${step {
        someResult = Some(postReportResponse(reportResponseAccepted))
      }}
        Then an event "REPORT_PRO_RESPONSE" is created                           ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.REPORT_PRO_RESPONSE
      )}
        And an event "EMAIL_CONSUMER_REPORT_RESPONSE" is created                 ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_CONSUMER_REPORT_RESPONSE
      )}
        And an event "EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT" is created              ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT
      )}
        And the response files are attached to the report                        ${reportFileMustHaveBeenAttachedToReport()}
        And the report reportStatusList is updated to "ReportStatus.PromesseAction"          ${reportMustHaveBeenUpdatedWithStatus(
        ReportStatus.PromesseAction
      )}
        And an acknowledgment email is sent to the consumer                      ${mailMustHaveBeenSent(
        reportFixture.email,
        "L'entreprise a répondu à votre signalement, donnez nous votre avis sur sa réponse",
        views.html.mails.consumer
          .reportToConsumerAcknowledgmentPro(
            report,
            reportResponseAccepted,
            frontRoute.dashboard.reportReview(report.id.toString)
          )
          .toString,
        attachementService.ConsumerProResponseNotificationAttachement
      )}
        And an acknowledgment email is sent to the professional                  ${mailMustHaveBeenSent(
        concernedHeadOfficeProUser.email,
        "Votre réponse au signalement",
        views.html.mails.professional
          .reportAcknowledgmentPro(reportResponseAccepted, concernedHeadOfficeProUser)
          .toString
      )}
    """
}

class ReportResponseProRejectedAnswer(implicit ee: ExecutionEnv) extends ReportResponseSpec {
  override def is =
    s2"""
        Given an authenticated pro user which is concerned by the report         ${step {
        someLoginInfo = Some(concernedProLoginInfo)
      }}
        When post a response with type "REJECTED"                                ${step {
        someResult = Some(postReportResponse(reportResponseRejected))
      }}
        Then an event "REPORT_PRO_RESPONSE" is created                           ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.REPORT_PRO_RESPONSE
      )}
        And an event "EMAIL_CONSUMER_REPORT_RESPONSE" is created                 ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_CONSUMER_REPORT_RESPONSE
      )}
        And an event "EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT" is created              ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT
      )}
        And the report reportStatusList is updated to "ReportStatus.Infonde"      ${reportMustHaveBeenUpdatedWithStatus(
        ReportStatus.Infonde
      )}
        And an acknowledgment email is sent to the consumer                      ${mailMustHaveBeenSent(
        reportFixture.email,
        "L'entreprise a répondu à votre signalement, donnez nous votre avis sur sa réponse",
        views.html.mails.consumer
          .reportToConsumerAcknowledgmentPro(
            report,
            reportResponseRejected,
            frontRoute.dashboard.reportReview(report.id.toString)
          )
          .toString,
        attachementService.ConsumerProResponseNotificationAttachement
      )}
        And an acknowledgment email is sent to the professional                  ${mailMustHaveBeenSent(
        concernedProUser.email,
        "Votre réponse au signalement",
        views.html.mails.professional.reportAcknowledgmentPro(reportResponseRejected, concernedProUser).toString
      )}
    """
}

class ReportResponseProNotConcernedAnswer(implicit ee: ExecutionEnv) extends ReportResponseSpec {
  override def is =
    s2"""
        Given an authenticated pro user which is concerned by the report         ${step {
        someLoginInfo = Some(concernedProLoginInfo)
      }}
        When post a response with type "NOT_CONCERNED"                           ${step {
        someResult = Some(postReportResponse(reportResponseNotConcerned))
      }}
        Then an event "REPORT_PRO_RESPONSE" is created                           ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.REPORT_PRO_RESPONSE
      )}
        And an event "EMAIL_CONSUMER_REPORT_RESPONSE" is created                 ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_CONSUMER_REPORT_RESPONSE
      )}
        And an event "EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT" is created              ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT
      )}
        And the report reportStatusList is updated to "MAL_ATTRIBUE"             ${reportMustHaveBeenUpdatedWithStatus(
        ReportStatus.MalAttribue
      )}
        And an acknowledgment email is sent to the consumer                      ${mailMustHaveBeenSent(
        reportFixture.email,
        "L'entreprise a répondu à votre signalement, donnez nous votre avis sur sa réponse",
        views.html.mails.consumer
          .reportToConsumerAcknowledgmentPro(
            report,
            reportResponseNotConcerned,
            frontRoute.dashboard.reportReview(report.id.toString)
          )
          .toString,
        attachementService.ConsumerProResponseNotificationAttachement
      )}
        And an acknowledgment email is sent to the professional                  ${mailMustHaveBeenSent(
        concernedProUser.email,
        "Votre réponse au signalement",
        views.html.mails.professional.reportAcknowledgmentPro(reportResponseNotConcerned, concernedProUser).toString
      )}
    """
}

abstract class ReportResponseSpec(implicit ee: ExecutionEnv) extends Specification with AppSpec with FutureMatchers {

  lazy val reportRepository = components.reportRepository
  lazy val reportFileRepository = components.reportFileRepository
  lazy val userRepository = components.userRepository
  lazy val eventRepository = components.eventRepository
  lazy val companyRepository = components.companyRepository
  lazy val companyAccessRepository = components.companyAccessRepository
  lazy val companyDataRepository = components.companyDataRepository
  lazy val AccessTokenRepositoryInterface = components.accessTokenRepository
  lazy val mailerService = components.mailer
  lazy val attachementService = components.attachmentService
  implicit lazy val frontRoute = components.frontRoute

  val contactEmail = EmailAddress("contact@signal.conso.gouv.fr")

  val siretForConcernedPro = Fixtures.genSiret().sample.get
  val siretForNotConcernedPro = Fixtures.genSiret().sample.get

  val company = Fixtures.genCompany.sample.get.copy(siret = siretForConcernedPro)
  val headOfficeCompany =
    Fixtures.genCompany.sample.get.copy(siret = Fixtures.genSiret(Some(SIREN(siretForConcernedPro))).sample.get)

  val companyData = Fixtures.genCompanyData(Some(company)).sample.get
  val headOfficeCompanyData =
    Fixtures.genCompanyData(Some(headOfficeCompany)).sample.get.copy(etablissementSiege = Some("true"))

  val reportFixture = Fixtures.genReportForCompany(company).sample.get.copy(status = ReportStatus.Transmis)

  var reviewUrl = new URI("")
  var report = reportFixture

  val concernedProUser = Fixtures.genProUser.sample.get
  val concernedProLoginInfo = LoginInfo(CredentialsProvider.ID, concernedProUser.email.value)

  val concernedHeadOfficeProUser = Fixtures.genProUser.sample.get
  val concernedHeadOfficeProLoginInfo = LoginInfo(CredentialsProvider.ID, concernedHeadOfficeProUser.email.value)

  val notConcernedProUser = Fixtures.genProUser.sample.get
  val notConcernedProLoginInfo = LoginInfo(CredentialsProvider.ID, notConcernedProUser.email.value)

  var someLoginInfo: Option[LoginInfo] = None
  var someResult: Option[Result] = None

  val reportResponseFile = ReportFile(
    ReportFileId.generateId(),
    None,
    OffsetDateTime.now,
    "fichier.jpg",
    "123_fichier.jpg",
    ReportFileOrigin.PROFESSIONAL,
    None
  )

  val reportResponseAccepted = ReportResponse(
    ReportResponseType.ACCEPTED,
    "details for consumer",
    Some("details for dgccrf"),
    List(reportResponseFile.id)
  )
  val reportResponseRejected =
    ReportResponse(ReportResponseType.REJECTED, "details for consumer", Some("details for dgccrf"), List.empty)
  val reportResponseNotConcerned =
    ReportResponse(ReportResponseType.NOT_CONCERNED, "details for consumer", Some("details for dgccrf"), List.empty)

  override def setupData() = {
    reviewUrl = new URI(
      configLoader.dashboardURL.toString + s"/suivi-des-signalements/${reportFixture.id}/avis"
    )
    Await.result(
      for {
        _ <- userRepository.create(concernedProUser)
        _ <- userRepository.create(concernedHeadOfficeProUser)
        _ <- userRepository.create(notConcernedProUser)

        _ <- companyRepository.getOrCreate(company.siret, company)
        _ <- companyRepository.getOrCreate(headOfficeCompany.siret, headOfficeCompany)

        _ <- companyAccessRepository.createUserAccess(company.id, concernedProUser.id, AccessLevel.ADMIN)
        _ <- companyAccessRepository.createUserAccess(
          headOfficeCompany.id,
          concernedHeadOfficeProUser.id,
          AccessLevel.ADMIN
        )

        _ <- companyDataRepository.create(companyData)
        _ <- companyDataRepository.create(headOfficeCompanyData)

        _ <- reportRepository.create(reportFixture)
        _ <- reportFileRepository.create(reportResponseFile)
      } yield (),
      Duration.Inf
    )
  }

  implicit val env: Environment[AuthEnv] = new FakeEnvironment[AuthEnv](
    Seq(
      concernedProLoginInfo -> concernedProUser,
      concernedHeadOfficeProLoginInfo -> concernedHeadOfficeProUser,
      notConcernedProLoginInfo -> notConcernedProUser
    )
  )

  val (app, components) = TestApp.buildApp(
    Some(
      env
    )
  )

  def postReportResponse(reportResponse: ReportResponse) =
    Await.result(
      components.reportController
        .reportResponse(reportFixture.id)
        .apply(
          someLoginInfo
            .map(FakeRequest().withAuthenticator[AuthEnv](_))
            .getOrElse(FakeRequest("POST", s"/api/reports/${reportFixture.id}/response"))
            .withBody(Json.toJson(reportResponse))
        ),
      Duration.Inf
    )

  def resultStatusMustBe(status: Int) =
    someResult must beSome and someResult.get.header.status === status

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

  def eventMustHaveBeenCreatedWithAction(action: ActionEventValue) = {
    val events = Await.result(eventRepository.list(), Duration.Inf).toList
    events.map(_.action) must contain(action)
  }

  def eventActionMatcher(action: ActionEventValue): org.specs2.matcher.Matcher[Event] = { event: Event =>
    (action == event.action, s"action doesn't match ${action}")
  }

  def reportMustHaveBeenUpdatedWithStatus(status: ReportStatus) = {
    report = Await.result(reportRepository.get(reportFixture.id), Duration.Inf).get
    report must reportStatusMatcher(status)
  }

  def reportStatusMatcher(status: ReportStatus): org.specs2.matcher.Matcher[Report] = { report: Report =>
    (status == report.status, s"status doesn't match ${status} - ${report}")
  }

  def reportFileMustHaveBeenAttachedToReport() = {
    val reportFile = Await.result(reportFileRepository.get(reportResponseFile.id), Duration.Inf).get
    reportFile must beEqualTo(reportResponseFile.copy(reportId = Some(reportFixture.id)))
  }

}
