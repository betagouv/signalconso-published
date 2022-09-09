package controllers

import java.time.LocalDate
import java.time.OffsetDateTime
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test._
import models._
import models.report.ReportStatus
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.matcher.JsonMatchers
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import play.api.test.Helpers._
import play.api.test._
import utils.silhouette.auth.AuthEnv
import utils.AppSpec
import utils.Fixtures
import utils.TestApp

import scala.concurrent.Await
import scala.concurrent.duration._

class ReportStatisticSpec(implicit ee: ExecutionEnv) extends StatisticControllerSpec {
  override def is =
    s2"""it should
       return reports count                               ${getReportCount}
       return reports curve                               ${getReportsCurve}
       return reports curve filted by status              ${getReportsCurveFilteredByStatus}
       return a public stat curve                         ${gePublicStatCurve}
       """

  def aMonthlyStat(monthlyStat: CountByDate): Matcher[String] =
    /("count").andHave(monthlyStat.count) and
      /("date").andHave(monthlyStat.date.toString)

  def haveMonthlyStats(monthlyStats: Matcher[String]*): Matcher[String] =
    have(allOf(monthlyStats: _*))

  def getReportCount = {
    val request = FakeRequest(GET, routes.StatisticController.getReportsCount().toString)
      .withAuthenticator[AuthEnv](loginInfo(adminUser))
    val result = route(app, request).get
    status(result) must beEqualTo(OK)
    val content = contentAsJson(result).toString
    content must /("value" -> allReports.length)
  }

  def getReportsCurve = {
    val request = FakeRequest(GET, routes.StatisticController.getReportsCountCurve().toString)
      .withAuthenticator[AuthEnv](loginInfo(adminUser))
    val result = route(app, request).get
    status(result) must beEqualTo(OK)
    val content = contentAsJson(result).toString
    val startDate = LocalDate.now.withDayOfMonth(1)
    content must haveMonthlyStats(
      aMonthlyStat(CountByDate(0, startDate.minusMonths(2L))),
      aMonthlyStat(CountByDate(lastMonthReports.length, startDate.minusMonths(1L))),
      aMonthlyStat(CountByDate(currentMonthReports.length, startDate))
    )
  }

  def getReportsCurveFilteredByStatus = {
    val request =
      FakeRequest(
        GET,
        routes.StatisticController
          .getReportsCountCurve()
          .toString + "?status=PromesseAction&status=Infonde&status=MalAttribue"
      )
        .withAuthenticator[AuthEnv](loginInfo(adminUser))
    val result = route(app, request).get
    status(result) must beEqualTo(OK)
    val content = contentAsJson(result).toString
    val startDate = LocalDate.now.withDayOfMonth(1)
    content must haveMonthlyStats(
      aMonthlyStat(CountByDate(lastMonthReportsWithResponse.length, startDate.minusMonths(1L))),
      aMonthlyStat(CountByDate(currentMonthReportsWithResponse.length, startDate))
    )
  }

  def gePublicStatCurve = {
    val request =
      FakeRequest(GET, routes.StatisticController.getPublicStatCurve(PublicStat.PromesseAction).toString)
    val result = route(app, request).get
    status(result) must beEqualTo(OK)
    val content = contentAsJson(result).toString
    val startDate = LocalDate.now.withDayOfMonth(1)
    content must haveMonthlyStats(
      aMonthlyStat(CountByDate(lastMonthReportsAccepted.length, startDate.minusMonths(1L))),
      aMonthlyStat(CountByDate(currentMonthReportsAccepted.length, startDate))
    )
  }
}

abstract class StatisticControllerSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with FutureMatchers
    with JsonMatchers {

  lazy val companyRepository = components.companyRepository
  lazy val reportRepository = components.reportRepository

  val adminUser = Fixtures.genAdminUser.sample.get

  val company = Fixtures.genCompany.sample.get

  val lastYearReportsToProcess = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.TraitementEnCours)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusYears(1)))
  val lastYearReportsAccepted = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.PromesseAction)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusYears(1)))
  val lastYearReportsRejected = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.Infonde)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusYears(1)))
  val lastYearReportsNotConcerned = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.MalAttribue)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusYears(1)))
  val lastYearReportsClosedByNoAction = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.ConsulteIgnore)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusYears(1)))
  val lastYearReportsNotForwarded = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.NA)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusYears(1))) :::
    Fixtures
      .genReportsForCompanyWithStatus(company, ReportStatus.LanceurAlerte)
      .sample
      .get
      .map(_.copy(creationDate = OffsetDateTime.now().minusYears(1)))

  val lastYearReportsWithResponse = lastYearReportsAccepted ::: lastYearReportsRejected ::: lastYearReportsNotConcerned
  val lastYearReportsReadByPro = lastYearReportsWithResponse ::: lastYearReportsClosedByNoAction
  val lastYearReportsForwardedToPro = lastYearReportsToProcess ::: lastYearReportsReadByPro
  val lastYearReports = lastYearReportsForwardedToPro ::: lastYearReportsNotForwarded

  val lastMonthReportsToProcess = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.TraitementEnCours)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusMonths(1L)))
  val lastMonthReportsAccepted = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.PromesseAction)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusMonths(1L)))
  val lastMonthReportsRejected = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.Infonde)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusMonths(1L)))
  val lastMonthReportsNotConcerned = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.MalAttribue)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusMonths(1L)))
  val lastMonthReportsClosedByNoAction = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.ConsulteIgnore)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusMonths(1L)))
  val lastMonthReportsNotForwarded = Fixtures
    .genReportsForCompanyWithStatus(company, ReportStatus.NA)
    .sample
    .get
    .map(_.copy(creationDate = OffsetDateTime.now().minusMonths(1L))) :::
    Fixtures
      .genReportsForCompanyWithStatus(company, ReportStatus.LanceurAlerte)
      .sample
      .get
      .map(_.copy(creationDate = OffsetDateTime.now().minusMonths(1L)))

  val lastMonthReportsWithResponse =
    lastMonthReportsAccepted ::: lastMonthReportsRejected ::: lastMonthReportsNotConcerned
  val lastMonthReportsReadByPro = lastMonthReportsWithResponse ::: lastMonthReportsClosedByNoAction
  val lastMonthReportsForwardedToPro = lastMonthReportsToProcess ::: lastMonthReportsReadByPro
  val lastMonthReports = lastMonthReportsForwardedToPro ::: lastMonthReportsNotForwarded

  val currentMonthReportsToProcess =
    Fixtures.genReportsForCompanyWithStatus(company, ReportStatus.TraitementEnCours).sample.get
  val currentMonthReportsSend = Fixtures.genReportsForCompanyWithStatus(company, ReportStatus.Transmis).sample.get
  val currentMonthReportsAccepted =
    Fixtures.genReportsForCompanyWithStatus(company, ReportStatus.PromesseAction).sample.get
  val currentMonthReportsRejected = Fixtures.genReportsForCompanyWithStatus(company, ReportStatus.Infonde).sample.get
  val currentMonthReportsNotConcerned =
    Fixtures.genReportsForCompanyWithStatus(company, ReportStatus.MalAttribue).sample.get
  val currentMonthReportsClosedByNoAction =
    Fixtures.genReportsForCompanyWithStatus(company, ReportStatus.ConsulteIgnore).sample.get
  val currentMonthReportsNotForwarded =
    Fixtures.genReportsForCompanyWithStatus(company, ReportStatus.NA).sample.get ::: Fixtures
      .genReportsForCompanyWithStatus(company, ReportStatus.LanceurAlerte)
      .sample
      .get

  val currentMonthReportsWithResponse =
    currentMonthReportsAccepted ::: currentMonthReportsRejected ::: currentMonthReportsNotConcerned
  val currentMonthReportsReadByPro = currentMonthReportsWithResponse ::: currentMonthReportsClosedByNoAction
  val currentMonthReports =
    currentMonthReportsToProcess ::: currentMonthReportsSend ::: currentMonthReportsReadByPro ::: currentMonthReportsNotForwarded

  val reportsWithResponseCutoff = lastYearReportsWithResponse ::: lastMonthReportsWithResponse
  val reportsReadByProCutoff = lastYearReportsReadByPro ::: lastMonthReportsReadByPro
  val reportsForwardedToProCutoff = lastYearReportsForwardedToPro ::: lastMonthReportsForwardedToPro
  val reportsClosedByNoActionCutoff = lastYearReportsClosedByNoAction ::: lastMonthReportsClosedByNoAction

  val allReports = lastYearReports ::: lastMonthReports ::: currentMonthReports

  override def setupData() = {
    Await.result(companyRepository.getOrCreate(company.siret, company), Duration.Inf)
    for (report <- allReports)
      Await.result(reportRepository.create(report), Duration.Inf)
  }

  def loginInfo(user: User) = LoginInfo(CredentialsProvider.ID, user.email.value)

  implicit val env: FakeEnvironment[AuthEnv] =
    new FakeEnvironment[AuthEnv](Seq(adminUser).map(user => loginInfo(user) -> user))

  val (app, components) = TestApp.buildApp(
    Some(
      env
    )
  )
}
