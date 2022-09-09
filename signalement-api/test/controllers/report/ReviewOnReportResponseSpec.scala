package controllers.report

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.mohiva.play.silhouette.test.FakeRequestWithAuthenticator

import java.util.UUID
import controllers.routes
import models.User
import models.report.Report
import models.report.ReportStatus
import models.report.review.ResponseConsumerReview
import models.report.review.ResponseConsumerReviewApi
import models.report.review.ResponseConsumerReviewId
import models.report.review.ResponseEvaluation
import org.specs2.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import play.api.libs.json.Json
import play.api.mvc.Result
import play.mvc.Http.Status
import utils.Constants.ActionEvent
import utils.Constants.EventType
import utils.Constants.ActionEvent.ActionEventValue
import utils.AppSpec
import utils.Fixtures
import utils.TestApp
import play.api.test.Helpers._
import play.api.test._
import repositories.event.EventFilter
import utils.silhouette.auth.AuthEnv

import java.time.OffsetDateTime
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ReviewOnReportWithoutResponse(implicit ee: ExecutionEnv) extends ReviewOnReportResponseSpec {
  override def is =
    s2"""
         Given a report without response                              ${step { reportId = reportWithoutResponse.id }}
         When post a review                                          ${step {
        someResult = Some(postReview(review))
      }}
         Then result status is forbidden                              ${resultStatusMustBe(Status.FORBIDDEN)}
    """
}

class FirstReviewOnReport(implicit ee: ExecutionEnv) extends ReviewOnReportResponseSpec {
  override def is =
    s2"""
         Given a report with a response                               ${step { reportId = reportWithResponse.id }}
         When post a review                                          ${step {
        someResult = Some(postReview(review))
      }}
         Then result status is OK                                     ${resultStatusMustBe(Status.OK)}
         And an event "REVIEW_ON_REPORT_RESPONSE" is created          ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.REPORT_REVIEW_ON_RESPONSE
      )}
    """
}

class SecondReviewOnReport(implicit ee: ExecutionEnv) extends ReviewOnReportResponseSpec {
  override def is =
    s2"""
         Given a report with a review                                ${step { reportId = reportWithReview.id }}
         When post a review                                          ${step {
        someResult = Some(postReview(review))
      }}
         Then result status is CONFLICT                               ${resultStatusMustBe(Status.FORBIDDEN)}
    """
}

class GetReviewOnReport(implicit ee: ExecutionEnv) extends ReviewOnReportResponseSpec {
  override def is =
    s2"""
         Given a report with a review   When post a review then the response is found $e1"""

  def e1 = {
    val result = route(
      app,
      FakeRequest(GET, routes.ReportConsumerReviewController.getReview(reportWithExistingReview.id).toString)
        .withAuthenticator[AuthEnv](loginInfo(adminUser))
    ).get

    status(result) must beEqualTo(OK)
    val responseConsumerReviewApi = Helpers.contentAsJson(result).as[ResponseConsumerReviewApi]
    responseConsumerReviewApi.evaluation mustEqual consumerReview.evaluation
    responseConsumerReviewApi.details mustEqual consumerReview.details
  }
}

abstract class ReviewOnReportResponseSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with FutureMatchers {

  val adminUser = Fixtures.genAdminUser.sample.get
  def loginInfo(user: User) = LoginInfo(CredentialsProvider.ID, user.email.value)

  implicit val env: FakeEnvironment[AuthEnv] =
    new FakeEnvironment[AuthEnv](Seq(adminUser).map(user => loginInfo(user) -> user))

  val (app, components) = TestApp.buildApp(
    Some(
      env
    )
  )

  lazy val reportRepository = components.reportRepository
  lazy val eventRepository = components.eventRepository
  lazy val responseConsumerReviewRepository = components.responseConsumerReviewRepository
  lazy val companyRepository = components.companyRepository

  val review = ResponseConsumerReviewApi(ResponseEvaluation.Positive, None)

  val company = Fixtures.genCompany.sample.get

  val reportWithoutResponse = Fixtures.genReportForCompany(company).sample.get.copy(status = ReportStatus.Transmis)

  val reportWithResponse = Fixtures.genReportForCompany(company).sample.get.copy(status = ReportStatus.PromesseAction)
  val responseEvent =
    Fixtures.genEventForReport(reportWithResponse.id, EventType.PRO, ActionEvent.REPORT_PRO_RESPONSE).sample.get

  val reportWithReview = Fixtures.genReportForCompany(company).sample.get.copy(status = ReportStatus.PromesseAction)
  val reportWithExistingReview =
    Fixtures.genReportForCompany(company).sample.get.copy(status = ReportStatus.PromesseAction)
  val responseWithReviewEvent =
    Fixtures.genEventForReport(reportWithReview.id, EventType.PRO, ActionEvent.REPORT_PRO_RESPONSE).sample.get

  val consumerReview =
    ResponseConsumerReview(
      ResponseConsumerReviewId.generateId(),
      reportWithExistingReview.id,
      ResponseEvaluation.Positive,
      OffsetDateTime.now(),
      Some("Response Details...")
    )

  val consumerConflictReview =
    ResponseConsumerReview(
      ResponseConsumerReviewId.generateId(),
      reportWithReview.id,
      ResponseEvaluation.Positive,
      OffsetDateTime.now(),
      Some("Response Details...")
    )

  var reportId = UUID.randomUUID()

  var someResult: Option[Result] = None

  override def setupData() =
    Await.result(
      for {
        _ <- companyRepository.getOrCreate(company.siret, company)
        _ <- reportRepository.create(reportWithoutResponse)
        _ <- reportRepository.create(reportWithResponse)
        _ <- reportRepository.create(reportWithReview)
        _ <- responseConsumerReviewRepository.create(consumerConflictReview)
        _ <- reportRepository.create(reportWithExistingReview)
        _ <- responseConsumerReviewRepository.create(consumerReview)
        _ <- eventRepository.create(responseEvent)
        _ <- eventRepository.create(responseWithReviewEvent)
      } yield (),
      Duration.Inf
    )

  def postReview(reviewOnReportResponse: ResponseConsumerReviewApi) =
    Await.result(
      components.reportConsumerReviewController
        .reviewOnReportResponse(reportId)
        .apply(
          FakeRequest("POST", s"/api/reports/${reportId}/response/review").withBody(Json.toJson(reviewOnReportResponse))
        ),
      Duration.Inf
    )

  def resultStatusMustBe(status: Int) =
    someResult must beSome and someResult.get.header.status === status

  def eventMustHaveBeenCreatedWithAction(action: ActionEventValue) = {
    val events =
      Await.result(eventRepository.getEvents(reportId, EventFilter(action = Some(action))), Duration.Inf).toList
    events.length must beEqualTo(1)
  }

  def reportMustHaveBeenUpdatedWithStatus(status: ReportStatus) = {
    val report = Await.result(reportRepository.get(reportId), Duration.Inf).get
    report must reportStatusMatcher(status)
  }

  def reportStatusMatcher(status: ReportStatus): org.specs2.matcher.Matcher[Report] = { report: Report =>
    (status == report.status, s"status doesn't match ${status} - ${report}")
  }
}
