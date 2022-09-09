package controllers.company

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test._
import controllers.routes
import models._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.matcher.JsonMatchers
import org.specs2.matcher.Matcher
import org.specs2.matcher.TraversableMatchers
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.test.Helpers._
import play.api.test._
import utils.silhouette.auth.AuthEnv
import utils.AppSpec
import utils.Fixtures
import utils.SIREN
import utils.SIRET
import utils.TestApp

import scala.concurrent.Await
import scala.concurrent.duration._

class BaseVisibleCompaniesSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with FutureMatchers
    with JsonMatchers {

  implicit val ec = ee.executionContext
  val logger: Logger = Logger(this.getClass)

  lazy val userRepository = components.userRepository
  lazy val companyRepository = components.companyRepository
  lazy val companyAccessRepository = components.companyAccessRepository
  lazy val companyDataRepository = components.companyDataRepository
  lazy val companiesVisibilityOrchestrator = components.companiesVisibilityOrchestrator

  val proUserWithAccessToHeadOffice = Fixtures.genProUser.sample.get
  val adminWithAccessToHeadOffice = Fixtures.genProUser.sample.get
  val proUserWithAccessToSubsidiary = Fixtures.genProUser.sample.get
  val adminWithAccessToSubsidiary = Fixtures.genProUser.sample.get

  val headOfficeCompany = Fixtures.genCompany.sample.get
  val subsidiaryCompany =
    Fixtures.genCompany.sample.get.copy(siret = Fixtures.genSiret(Some(SIREN(headOfficeCompany.siret))).sample.get)

  val headOfficeCompanyData =
    Fixtures.genCompanyData(Some(headOfficeCompany)).sample.get.copy(etablissementSiege = Some("true"))
  val subsidiaryCompanyData = Fixtures.genCompanyData(Some(subsidiaryCompany)).sample.get
  val subsidiaryClosedCompanyData = Fixtures
    .genCompanyData()
    .sample
    .get
    .copy(
      siret = SIRET.fromUnsafe(SIREN(headOfficeCompany.siret).value + "00020"),
      siren = SIREN(headOfficeCompany.siret),
      etatAdministratifEtablissement = Some("F")
    )

  val companyWithoutAccess = Fixtures.genCompany.sample.get
  val companyWithoutAccessData = Fixtures.genCompanyData(Some(companyWithoutAccess)).sample.get

  override def setupData() =
    Await.result(
      for {
        _ <- userRepository.create(proUserWithAccessToHeadOffice)
        _ <- userRepository.create(proUserWithAccessToSubsidiary)
        _ <- userRepository.create(adminWithAccessToHeadOffice)
        _ <- userRepository.create(adminWithAccessToSubsidiary)

        _ <- companyRepository.getOrCreate(headOfficeCompany.siret, headOfficeCompany)
        _ <- companyRepository.getOrCreate(subsidiaryCompany.siret, subsidiaryCompany)

        _ <- companyAccessRepository.createUserAccess(
          headOfficeCompany.id,
          proUserWithAccessToHeadOffice.id,
          AccessLevel.MEMBER
        )
        _ <- companyAccessRepository.createUserAccess(
          headOfficeCompany.id,
          adminWithAccessToHeadOffice.id,
          AccessLevel.ADMIN
        )
        _ <- companyAccessRepository.createUserAccess(
          subsidiaryCompany.id,
          proUserWithAccessToSubsidiary.id,
          AccessLevel.MEMBER
        )
        _ <- companyAccessRepository.createUserAccess(
          subsidiaryCompany.id,
          adminWithAccessToSubsidiary.id,
          AccessLevel.MEMBER
        )

        _ <- companyDataRepository.create(headOfficeCompanyData)
        _ <- companyDataRepository.create(subsidiaryCompanyData)
        _ <- companyDataRepository.create(subsidiaryClosedCompanyData)

        _ <- companyRepository.getOrCreate(companyWithoutAccess.siret, companyWithoutAccess)
        _ <- companyDataRepository.create(companyWithoutAccessData)
      } yield (),
      Duration.Inf
    )
  override def cleanupData() =
    Await.result(
      for {
        _ <- companyDataRepository.delete(headOfficeCompanyData.id)
        _ <- companyDataRepository.delete(subsidiaryCompanyData.id)
        _ <- companyDataRepository.delete(subsidiaryClosedCompanyData.id)
        _ <- companyDataRepository.delete(companyWithoutAccessData.id)
      } yield (),
      Duration.Inf
    )

  def loginInfo(user: User) = LoginInfo(CredentialsProvider.ID, user.email.value)

  implicit val env = new FakeEnvironment[AuthEnv](
    Seq(proUserWithAccessToHeadOffice, proUserWithAccessToSubsidiary).map(user => loginInfo(user) -> user)
  )

  val (app, components) = TestApp.buildApp(
    Some(env)
  )

}

class VisibleCompaniesSpec(implicit ee: ExecutionEnv) extends BaseVisibleCompaniesSpec {
  override def is =
    s2"""

The get visible companies endpoint should
  list headOffice and subsidiary companies for a user who access to the headOffice $e1
  list only the subsidiary company for a user who only access to the subsidiary $e2
  list admins and member having direct access to the headOffice $e3
  list admins and member having access to the subsidiary including headOffices admins and members $e4
"""

  def e1 = {
    val request = FakeRequest(GET, routes.CompanyController.visibleCompanies().toString)
      .withAuthenticator[AuthEnv](loginInfo(proUserWithAccessToHeadOffice))
    val result = route(app, request).get
    status(result) must beEqualTo(OK)
    val content = contentAsJson(result).toString
    content must haveVisibleCompanies(
      aVisibleCompany(headOfficeCompany.siret),
      aVisibleCompany(subsidiaryCompanyData.siret)
    )
  }

  def e2 = {
    val request = FakeRequest(GET, routes.CompanyController.visibleCompanies().toString)
      .withAuthenticator[AuthEnv](loginInfo(proUserWithAccessToSubsidiary))
    val result = route(app, request).get
    status(result) must beEqualTo(OK)
    val content = contentAsJson(result).toString
    content must haveVisibleCompanies(
      aVisibleCompany(subsidiaryCompanyData.siret)
    )
  }

  def e3 = {
    val headOfficeViewersList = Await.result(
      companiesVisibilityOrchestrator.fetchAdminsWithHeadOffices(List((headOfficeCompany.siret, headOfficeCompany.id))),
      Duration.Inf
    )
    Await.result(
      companiesVisibilityOrchestrator.fetchAdminsWithHeadOffice(headOfficeCompany.siret),
      Duration.Inf
    )
    headOfficeViewersList(headOfficeCompany.id).map(_.id).sorted must beEqualTo(
      List(
        adminWithAccessToHeadOffice,
        proUserWithAccessToHeadOffice
      ).map(_.id).sorted
    )
  }

  def e4 = {
    val subsidiaryViewersList = Await.result(
      companiesVisibilityOrchestrator.fetchAdminsWithHeadOffices(List((subsidiaryCompany.siret, subsidiaryCompany.id))),
      Duration.Inf
    )
    val subsidiaryViewers = Await.result(
      companiesVisibilityOrchestrator.fetchAdminsWithHeadOffice(subsidiaryCompany.siret),
      Duration.Inf
    )
    subsidiaryViewersList(subsidiaryCompany.id).map(_.id).sorted must beEqualTo(subsidiaryViewers.map(_.id).sorted)
    subsidiaryViewersList(subsidiaryCompany.id).map(_.id).sorted must beEqualTo(
      List(
        proUserWithAccessToHeadOffice,
        proUserWithAccessToSubsidiary,
        adminWithAccessToHeadOffice,
        adminWithAccessToSubsidiary
      ).map(_.id).sorted
    )
  }

  def aVisibleCompany(siret: SIRET): Matcher[String] =
    /("siret" -> siret.value)

  def haveVisibleCompanies(visibleCompanies: Matcher[String]*): Matcher[String] =
    have(TraversableMatchers.exactly(visibleCompanies: _*))
}
