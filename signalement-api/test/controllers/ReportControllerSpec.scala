package controllers

import play.api.mvc.Results

import java.util.UUID
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import config.EmailConfiguration
import config.SignalConsoConfiguration
import config.TokenConfiguration
import config.UploadConfiguration
import controllers.error.AppError.InvalidEmail
import controllers.error.ErrorPayload
import models.report.review.ResponseEvaluation.Positive
import models.report.review.ResponseConsumerReview
import models.report.review.ResponseConsumerReviewId
import com.mohiva.play.silhouette.test.FakeRequestWithAuthenticator
import loader.SignalConsoComponents
import models.report.ReportFile
import models.report.ReportFileOrigin
import models.report.reportfile.ReportFileId
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import play.api.Application
import play.api.ApplicationLoader
import play.api.Configuration
import play.api.Logger
import services.MailerService
import services.S3ServiceInterface
import utils.Constants.ActionEvent.POST_ACCOUNT_ACTIVATION_DOC
import utils.Constants.EventType
import utils.silhouette.auth.AuthEnv
import utils.EmailAddress
import utils.Fixtures
import utils.S3ServiceMock
import utils.TestApp

import java.net.URI
import java.time.OffsetDateTime
import java.time.Period
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ReportControllerSpec(implicit ee: ExecutionEnv) extends Specification with Results with Mockito {

  val logger: Logger = Logger(this.getClass)

  "ReportController" should {

    "return a BadRequest with errors if report is invalid" in new Context {
      val testEnv = application()
      import testEnv._

      new WithApplication(app) {

        val jsonBody = Json.toJson("category" -> "")

        val request = FakeRequest("POST", "/api/reports").withJsonBody(jsonBody)

        val result = route(app, request).get

        Helpers.status(result) must beEqualTo(BAD_REQUEST)
      }
    }

    "return a BadRequest on invalid email" in new Context {
      val testEnv = application()
      import testEnv._

      new WithApplication(app) {

        val draftReport = Fixtures.genDraftReport.sample
        val jsonBody = Json.toJson(draftReport)

        val request = FakeRequest("POST", "/api/reports").withJsonBody(jsonBody)

        val result = route(app, request).get

        Helpers.status(result) must beEqualTo(BAD_REQUEST)
        Helpers.contentAsJson(result) must beEqualTo(
          Json.toJson(ErrorPayload(InvalidEmail(draftReport.get.email.value)))
        )
      }
    }

    "block spammed email" in new Context {
      val blockedEmail = "spammer@gmail.com"
      val testEnv = application(skipValidation = true, List(blockedEmail))
      import testEnv._

      new WithApplication(app) {

        val draftReport = Fixtures.genDraftReport.sample.get.copy(email = EmailAddress(blockedEmail))
        val jsonBody = Json.toJson(draftReport)

        val request = FakeRequest("POST", "/api/reports").withJsonBody(jsonBody)

        val result = route(app, request).get
        Helpers.status(result) must beEqualTo(OK)

        Helpers.contentAsBytes(result).isEmpty mustEqual true

      }
    }

    "delete report" in new Context {

      val testEnv = application()
      import testEnv._

      new WithApplication(app) {

        val company = Fixtures.genCompany.sample.get
        val report = Fixtures.genReportForCompany(company).sample.get
        val event = Fixtures.genEventForReport(report.id, EventType.PRO, POST_ACCOUNT_ACTIVATION_DOC).sample.get
        val reportFile = ReportFile(
          ReportFileId.generateId(),
          Some(report.id),
          OffsetDateTime.now(),
          "fileName",
          "storageName",
          ReportFileOrigin(""),
          None
        )
        val review =
          ResponseConsumerReview(ResponseConsumerReviewId.generateId(), report.id, Positive, OffsetDateTime.now(), None)

        Await.result(
          for {
            _ <- companyRepository.create(company)
            _ <- reportRepository.create(report)
            _ <- reportFileRepository.create(reportFile)
            _ <- eventRepository.create(event)
            _ <- responseConsumerReviewRepository.create(review)
          } yield (),
          Duration.Inf
        )

        val request =
          FakeRequest("DELETE", s"/api/reports/${report.id.toString}").withAuthenticator[AuthEnv](adminLoginInfo)
        val result = route(app, request).get

        Helpers.status(result) must beEqualTo(NO_CONTENT)
        Helpers.contentAsBytes(result).isEmpty mustEqual true

        val (maybeReport, maybeReportFile, maybeEvent, maybeReview) = Await.result(
          for {
            maybeReport <- reportRepository.get(report.id)
            maybeReportFile <- reportFileRepository.get(reportFile.id)
            maybeEvent <- eventRepository.get(event.id)
            maybeReview <- responseConsumerReviewRepository.get(review.id)
          } yield (maybeReport, maybeReportFile, maybeEvent, maybeReview),
          Duration.Inf
        )
        maybeReport must beNone
        maybeReportFile must beNone
        maybeEvent must beNone
        maybeReview must beNone

      }
    }

  }

  trait Context extends Scope {

    val adminIdentity = Fixtures.genAdminUser.sample.get
    val adminLoginInfo = LoginInfo(CredentialsProvider.ID, adminIdentity.email.value)
    val proIdentity = Fixtures.genProUser.sample.get
    val proLoginInfo = LoginInfo(CredentialsProvider.ID, proIdentity.email.value)

    val companyId = UUID.randomUUID

    implicit val env: Environment[AuthEnv] =
      new FakeEnvironment[AuthEnv](Seq(adminLoginInfo -> adminIdentity, proLoginInfo -> proIdentity))

    val mockMailerService = mock[MailerService]
    val mockS3Service = new S3ServiceMock()

    def application(skipValidation: Boolean = false, spammerBlacklist: List[String] = List.empty) = new {

      class FakeApplicationLoader(skipValidation: Boolean = false) extends ApplicationLoader {
        var components: SignalConsoComponents = _

        override def load(context: ApplicationLoader.Context): Application = {
          components = new SignalConsoComponents(context) {

            override def authEnv: Environment[AuthEnv] = env
            override def configuration: Configuration = Configuration(
              "play.evolutions.enabled" -> false,
              "slick.dbs.default.db.connectionPool" -> "disabled",
              "play.mailer.mock" -> true,
              "skip-report-email-validation" -> true,
              "silhouette.authenticator.sharedSecret" -> "sharedSecret",
              "play.tmpDirectory" -> "./target"
            ).withFallback(
              super.configuration
            )

            override def s3Service: S3ServiceInterface = mockS3Service
            override def tokenConfiguration = TokenConfiguration(None, None, None, Period.ZERO, None)
            override def uploadConfiguration = UploadConfiguration(Seq.empty, false, "/tmp")
            override def signalConsoConfiguration: SignalConsoConfiguration =
              SignalConsoConfiguration(
                "",
                new URI("http://test.com"),
                new URI("http://test.com"),
                new URI("http://test.com"),
                tokenConfiguration,
                uploadConfiguration,
                spammerBlacklist
              )

            override def emailConfiguration: EmailConfiguration =
              EmailConfiguration(
                EmailAddress("test@sc.com"),
                EmailAddress("test@sc.com"),
                skipValidation,
                "",
                List(""),
                ".*".r
              )

          }
          components.application
        }

      }

      val loader = new FakeApplicationLoader(skipValidation)

      val app = TestApp.buildApp(loader)

      lazy val reportRepository = loader.components.reportRepository
      lazy val reportFileRepository = loader.components.reportFileRepository
      lazy val eventRepository = loader.components.eventRepository
      lazy val responseConsumerReviewRepository = loader.components.responseConsumerReviewRepository
      lazy val companyRepository = loader.components.companyRepository

    }

  }

}
