package controllers

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.mohiva.play.silhouette.test._
import controllers.error.AppError.CompanySiretNotFound
import controllers.error.AppError.EmailAlreadyExist
import controllers.error.ErrorPayload
import models._
import models.token.TokenKind.CompanyJoin
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import utils.AppSpec
import utils.EmailAddress
import utils.TestApp
import utils.Fixtures
import utils.SIRET
import utils.silhouette.auth.AuthEnv

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AccountControllerSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with Results
    with FutureMatchers {

  val identity = Fixtures.genAdminUser.sample.get
  val identLoginInfo = LoginInfo(CredentialsProvider.ID, identity.email.value)

  val (app, components) = TestApp.buildApp(
    Some(
      new FakeEnvironment[AuthEnv](Seq(identLoginInfo -> identity))
    )
  )

  val userRepository = components.userRepository
  val companyRepository = components.companyRepository
  val companyAccessRepository = components.companyAccessRepository
  val accessTokenRepository = components.accessTokenRepository

  implicit val authEnv = components.authEnv

  val proUser = Fixtures.genProUser.sample.get
  val company = Fixtures.genCompany.sample.get

  override def setupData() =
    Await.result(
      for {
        _ <- userRepository.create(proUser)
        _ <- companyRepository.getOrCreate(company.siret, company)
        _ <- accessTokenRepository
          .create(AccessToken.build(CompanyJoin, "123456", None, Some(company.id), Some(AccessLevel.ADMIN), None))
      } yield (),
      Duration.Inf
    )

  "AccountController" should {

    "activateAccount" should {

      "raise a 409 in case of duplicate email addresse" in {
        val request = FakeRequest(POST, routes.AccountController.activateAccount().toString)
          .withJsonBody(
            Json.obj(
              "draftUser" -> Json.obj(
                "email" -> proUser.email,
                "firstName" -> proUser.firstName,
                "lastName" -> proUser.lastName,
                "password" -> proUser.password
              ),
              "token" -> "123456",
              "companySiret" -> company.siret
            )
          )

        val result = route(app, request).get

        Helpers.status(result) must beEqualTo(409)
        Helpers.contentAsJson(result) must beEqualTo(
          Json.toJson(ErrorPayload(EmailAlreadyExist))
        )
      }

      "fail on unknown siret" in {
        val siret = "XXXXXXXXXXXXXX"
        val request = FakeRequest(POST, routes.AccountController.activateAccount().toString)
          .withJsonBody(
            Json.obj(
              "draftUser" -> Json.obj(
                "email" -> proUser.email,
                "firstName" -> proUser.firstName,
                "lastName" -> proUser.lastName,
                "password" -> proUser.password
              ),
              "token" -> "123456",
              "companySiret" -> "XXXXXXXXXXXXXX"
            )
          )

        val result = route(app, request).get

        Helpers.status(result) must beEqualTo(404)
        Helpers.contentAsJson(result) must beEqualTo(
          Json.toJson(ErrorPayload(CompanySiretNotFound(SIRET.fromUnsafe(siret))))
        )
      }

      "use preexisting tokens with same email, if any" in {
        val newUser = Fixtures.genUser.sample.get
        val otherCompany = Fixtures.genCompany.sample.get
        Await.result(
          for {
            _ <- companyRepository.getOrCreate(otherCompany.siret, otherCompany)
            _ <- accessTokenRepository.create(
              AccessToken.build(
                CompanyJoin,
                "000000",
                None,
                Some(company.id),
                Some(AccessLevel.ADMIN),
                Some(newUser.email)
              )
            )
            token <- accessTokenRepository.create(
              AccessToken.build(
                CompanyJoin,
                "whatever",
                None,
                Some(otherCompany.id),
                Some(AccessLevel.ADMIN),
                Some(newUser.email)
              )
            )
          } yield token,
          Duration.Inf
        )
        val request = FakeRequest(POST, routes.AccountController.activateAccount().toString)
          .withJsonBody(
            Json.obj(
              "draftUser" -> Json.obj(
                "email" -> newUser.email,
                "firstName" -> newUser.firstName,
                "lastName" -> newUser.lastName,
                "password" -> newUser.password
              ),
              "token" -> "000000",
              "companySiret" -> company.siret
            )
          )

        val result = route(app, request).get
        Helpers.status(result) must beEqualTo(204)

        companyAccessRepository.fetchAdmins(company.id).map(_.length) must beEqualTo(1).await
        companyAccessRepository.fetchAdmins(otherCompany.id).map(_.length) must beEqualTo(1).await
      }

      "send an invalid DGCCRF invitation" in {
        val request = FakeRequest(POST, routes.AccountController.sendDGCCRFInvitation().toString)
          .withAuthenticator[AuthEnv](identLoginInfo)
          .withJsonBody(Json.obj("email" -> "user@example.com"))

        val result = route(app, request).get
        Helpers.status(result) must beEqualTo(403)
      }

      "send a DGCCRF invitation" in {
        val request = FakeRequest(POST, routes.AccountController.sendDGCCRFInvitation().toString)
          .withAuthenticator[AuthEnv](identLoginInfo)
          .withJsonBody(Json.obj("email" -> "user@dgccrf.gouv.fr"))

        val result = route(app, request).get
        Helpers.status(result) must beEqualTo(200)
      }

      "activate the DGCCF user" in {
        val ccrfUser = Fixtures.genUser.sample.get
        val ccrfToken =
          Await.result(accessTokenRepository.fetchPendingTokens(EmailAddress("user@dgccrf.gouv.fr")), Duration.Inf).head
        val request = FakeRequest(POST, routes.AccountController.activateAccount().toString)
          .withJsonBody(
            Json.obj(
              "draftUser" -> Json.obj(
                "email" -> "user@dgccrf.gouv.fr",
                "firstName" -> ccrfUser.firstName,
                "lastName" -> ccrfUser.lastName,
                "password" -> ccrfUser.password
              ),
              "token" -> ccrfToken.token
            )
          )
        val result = route(app, request).get
        Helpers.status(result) must beEqualTo(204)

        val createdUser = Await.result(userRepository.findByLogin("user@dgccrf.gouv.fr"), Duration.Inf)
        createdUser.get.userRole must beEqualTo(UserRole.DGCCRF)
      }
    }
  }

  override def afterAll(): Unit = {
    app.stop()
    ()
  }
}
