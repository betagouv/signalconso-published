package controllers.report

import akka.util.Timeout
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.mohiva.play.silhouette.test._
import models._
import models.report.Report
import models.report.ReportStatus
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.matcher.JsonMatchers
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import play.api.test.Helpers._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.mvc.Http.Status
import utils.silhouette.auth.AuthEnv
import utils.AppSpec
import utils.Fixtures
import utils.SIREN
import utils.TestApp

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future

class GetReportsByUnauthenticatedUser(implicit ee: ExecutionEnv) extends GetReportsSpec {
  override def is =
    s2"""
         Given an unauthenticated user                                ${step { someLoginInfo = None }}
         When retrieving reports                                      ${step { someResult = Some(getReports()) }}
         Then user is not authorized                                  ${userMustBeUnauthorized()}
    """
}

class GetReportsByAdminUser(implicit ee: ExecutionEnv) extends GetReportsSpec {
  override def is =
    s2"""
         Given an authenticated admin user                            ${step {
        someLoginInfo = Some(loginInfo(adminUser))
      }}
         When retrieving reports                                      ${step { someResult = Some(getReports()) }}
         Then reports are rendered to the user as a DGCCRF User       ${reportsMustBeRenderedForUser(adminUser)}
    """
}

class GetReportsByDGCCRFUser(implicit ee: ExecutionEnv) extends GetReportsSpec {
  override def is =
    s2"""
         Given an authenticated dgccrf user                           ${step {
        someLoginInfo = Some(loginInfo(dgccrfUser))
      }}
         When retrieving reports                                      ${step { someResult = Some(getReports()) }}
         Then reports are rendered to the user as an Admin            ${reportsMustBeRenderedForUser(dgccrfUser)}
    """
}

class GetReportsByProUserWithAccessToHeadOffice(implicit ee: ExecutionEnv) extends GetReportsSpec {
  override def is =
    s2"""
         Given an authenticated pro user who access to the headOffice               ${step {
        someLoginInfo = Some(loginInfo(proUserWithAccessToHeadOffice))
      }}
         When retrieving reports                                                    ${step {
        someResult = Some(getReports())
      }}
         Then headOffice and subsidiary reports are rendered to the user as a Pro   ${reportsMustBeRenderedForUser(
        proUserWithAccessToHeadOffice
      )}
    """
}

class GetReportsByProUserWithInvalidStatusFilter(implicit ee: ExecutionEnv) extends GetReportsSpec {
  override def is =
    s2"""
         Given an authenticated pro user                                            ${step {
        someLoginInfo = Some(loginInfo(proUserWithAccessToHeadOffice))
      }}
         When retrieving reports                                                    ${step {
        someResult = Some(getReports(Some("badvalue")))
      }}
         Then headOffice and subsidiary reports are rendered to the user as a Pro   ${mustBeBadRequest()}
    """
}

class GetReportsByProWithoutAccessNone(implicit ee: ExecutionEnv) extends GetReportsSpec {
  override def is =
    s2"""
         Given an authenticated pro user who only access to the subsidiary      ${step {
        someLoginInfo = Some(loginInfo(noAccessUser))
      }}
         When retrieving reports                                                ${step {
        someResult = Some(getReports())
      }}
         Then no reports are rendered to the user having no access              ${noReportsMustBeRendered()}
    """
}

abstract class GetReportsSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with FutureMatchers
    with JsonMatchers {

  implicit val timeout: Timeout = 30.seconds

  lazy val userRepository = components.userRepository
  lazy val companyRepository = components.companyRepository
  lazy val companyAccessRepository = components.companyAccessRepository
  lazy val companyDataRepository = components.companyDataRepository
  lazy val accessTokenRepository = components.accessTokenRepository
  lazy val reportRepository = components.reportRepository

  val noAccessUser = Fixtures.genProUser.sample.get
  val adminUser = Fixtures.genAdminUser.sample.get
  val dgccrfUser = Fixtures.genDgccrfUser.sample.get
  val proUserWithAccessToHeadOffice = Fixtures.genProUser.sample.get
  val proUserWithAccessToSubsidiary = Fixtures.genProUser.sample.get

  val standaloneCompany = Fixtures.genCompany.sample.get
  val headOfficeCompany = Fixtures.genCompany.sample.get
  val subsidiaryCompany =
    Fixtures.genCompany.sample.get.copy(siret = Fixtures.genSiret(Some(SIREN(headOfficeCompany.siret))).sample.get)

  val standaloneCompanyData =
    Fixtures.genCompanyData(Some(standaloneCompany)).sample.get.copy(etablissementSiege = Some("true"))
  val headOfficeCompanyData =
    Fixtures.genCompanyData(Some(headOfficeCompany)).sample.get.copy(etablissementSiege = Some("true"))
  val subsidiaryCompanyData = Fixtures.genCompanyData(Some(subsidiaryCompany)).sample.get

  val reportToStandaloneCompany = Fixtures
    .genReportForCompany(standaloneCompany)
    .sample
    .get
    .copy(employeeConsumer = false, status = ReportStatus.TraitementEnCours)
  val reportToProcessOnHeadOffice = Fixtures
    .genReportForCompany(headOfficeCompany)
    .sample
    .get
    .copy(employeeConsumer = false, status = ReportStatus.TraitementEnCours)
  val reportToProcessOnSubsidiary = Fixtures
    .genReportForCompany(subsidiaryCompany)
    .sample
    .get
    .copy(employeeConsumer = false, status = ReportStatus.TraitementEnCours)
  val reportFromEmployeeOnHeadOffice = Fixtures
    .genReportForCompany(headOfficeCompany)
    .sample
    .get
    .copy(employeeConsumer = true, status = ReportStatus.LanceurAlerte)
  val reportNAOnHeadOffice = Fixtures
    .genReportForCompany(headOfficeCompany)
    .sample
    .get
    .copy(employeeConsumer = false, status = ReportStatus.NA)
  val allReports = Seq(
    reportToStandaloneCompany,
    reportToProcessOnHeadOffice,
    reportToProcessOnSubsidiary,
    reportFromEmployeeOnHeadOffice,
    reportNAOnHeadOffice
  )

  var someResult: Option[Result] = None
  var someLoginInfo: Option[LoginInfo] = None

  override def setupData() =
    Await.result(
      for {
        _ <- userRepository.create(noAccessUser)
        _ <- userRepository.create(adminUser)
        _ <- userRepository.create(dgccrfUser)
        _ <- userRepository.create(proUserWithAccessToHeadOffice)
        _ <- userRepository.create(proUserWithAccessToSubsidiary)

        _ <- companyRepository.getOrCreate(standaloneCompany.siret, standaloneCompany)
        _ <- companyRepository.getOrCreate(headOfficeCompany.siret, headOfficeCompany)
        _ <- companyRepository.getOrCreate(subsidiaryCompany.siret, subsidiaryCompany)

        _ <- companyAccessRepository.createUserAccess(
          standaloneCompany.id,
          noAccessUser.id,
          AccessLevel.NONE
        )
        _ <- companyAccessRepository.createUserAccess(
          headOfficeCompany.id,
          proUserWithAccessToHeadOffice.id,
          AccessLevel.MEMBER
        )
        _ <- companyAccessRepository.createUserAccess(
          subsidiaryCompany.id,
          proUserWithAccessToSubsidiary.id,
          AccessLevel.MEMBER
        )

        _ <- companyDataRepository.create(standaloneCompanyData)
        _ <- companyDataRepository.create(headOfficeCompanyData)
        _ <- companyDataRepository.create(subsidiaryCompanyData)

        _ <- reportRepository.create(reportToStandaloneCompany)
        _ <- reportRepository.create(reportToProcessOnHeadOffice)
        _ <- reportRepository.create(reportToProcessOnSubsidiary)
        _ <- reportRepository.create(reportFromEmployeeOnHeadOffice)
        _ <- reportRepository.create(reportNAOnHeadOffice)
      } yield (),
      Duration.Inf
    )

  override def cleanupData() =
    Await.result(
      for {
        _ <- companyDataRepository.delete(headOfficeCompanyData.id)
        _ <- companyDataRepository.delete(subsidiaryCompanyData.id)
      } yield (),
      Duration.Inf
    )

  def loginInfo(user: User) = LoginInfo(CredentialsProvider.ID, user.email.value)

  implicit val env = new FakeEnvironment[AuthEnv](
    Seq(adminUser, dgccrfUser, proUserWithAccessToHeadOffice, proUserWithAccessToSubsidiary, noAccessUser).map(user =>
      loginInfo(user) -> user
    )
  )

  val (app, components) = TestApp.buildApp(
    Some(
      env
    )
  )

  def getReports(status: Option[String] = None) = {
    val request = FakeRequest(
      play.api.http.HttpVerbs.GET,
      controllers.routes.ReportListController.getReports().toString + status.map(x => s"?status=$x").getOrElse("")
    )
    val loggedRequest = someLoginInfo.map(request.withAuthenticator[AuthEnv](_)).getOrElse(request)
    val result = route(app, loggedRequest).get

    Await.result(
      result,
      Duration.Inf
    )
  }

  def userMustBeUnauthorized() =
    someResult must beSome and someResult.get.header.status === Status.UNAUTHORIZED

  def aReport(report: Report): Matcher[String] =
    /("report") / "id" andHave (report.id.toString)

  def haveReports(reports: Matcher[String]*): Matcher[String] =
    /("entities").andHave(allOf(reports: _*))

  def reportsMustBeRenderedForUser(user: User) =
//    implicit val someUserRole = Some(user.userRole)
    (user.userRole, user) match {
      case (UserRole.Admin, _) =>
        contentAsJson(Future(someResult.get))(timeout).toString must
          /("totalCount" -> allReports.length) and
          haveReports(allReports.map(report => aReport(report)): _*)
      case (UserRole.DGCCRF, _) =>
        contentAsJson(Future(someResult.get))(timeout).toString must
          /("totalCount" -> allReports.length) and
          haveReports(allReports.map(report => aReport(report)): _*)
      case (UserRole.Professionnel, pro) if pro == proUserWithAccessToHeadOffice =>
        contentAsJson(Future(someResult.get))(timeout).toString must
          /("totalCount" -> 2) and
          haveReports(aReport(reportToProcessOnHeadOffice), aReport(reportToProcessOnSubsidiary)) and
          not(haveReports(aReport(reportFromEmployeeOnHeadOffice), aReport(reportNAOnHeadOffice)))
      case (UserRole.Professionnel, pro) if pro == proUserWithAccessToSubsidiary =>
        contentAsJson(Future(someResult.get))(timeout).toString must
          /("totalCount" -> 1) and
          haveReports(aReport(reportToProcessOnSubsidiary)) and
          not(
            haveReports(
              aReport(reportFromEmployeeOnHeadOffice),
              aReport(reportToProcessOnHeadOffice),
              aReport(reportNAOnHeadOffice)
            )
          )
      case _ =>
        someResult must beSome and someResult.get.header.status === Status.UNAUTHORIZED
    }

  def noReportsMustBeRendered() =
    Helpers.contentAsJson(Future(someResult.get))(timeout).toString must
      /("totalCount" -> 0) and
      not(haveReports(allReports.map(report => aReport(report)): _*))

  def mustBeBadRequest() =
    someResult must beSome and someResult.get.header.status === Status.BAD_REQUEST
}
