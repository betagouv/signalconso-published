package controllers.company

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test._
import controllers.routes
import models._
import models.token.TokenKind.CompanyInit
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.matcher.JsonMatchers
import org.specs2.matcher.Matcher
import org.specs2.matcher.TraversableMatchers
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.test.Helpers._
import play.api.test._
import utils.Constants.ActionEvent._
import utils.Constants.EventType._
import utils.silhouette.auth.AuthEnv
import utils.AppSpec
import utils.Fixtures
import utils.TestApp

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class BaseFetchCompaniesToActivateSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with FutureMatchers
    with JsonMatchers {

  implicit val ec = ee.executionContext
  val logger: Logger = Logger(this.getClass)

  lazy val userRepository = components.userRepository
  lazy val companyRepository = components.companyRepository
  lazy val accessTokenRepository = components.accessTokenRepository
  lazy val eventRepository = components.eventRepository

  val tokenDuration = java.time.Period.parse("P60D")
  val reportReminderByPostDelay = java.time.Period.parse("P28D")
  val defaultTokenCreationDate = OffsetDateTime.now.minusMonths(1)

  val adminUser = Fixtures.genAdminUser.sample.get

  var companyCases: Seq[(Company, Option[OffsetDateTime], OffsetDateTime)] = Seq()

  def initCase = {
    val company = Fixtures.genCompany.sample.get
    for {
      c <- companyRepository.getOrCreate(company.siret, company)
      a <- accessTokenRepository.create(
        AccessToken.build(
          CompanyInit,
          f"${Random.nextInt(1000000)}%06d",
          Some(tokenDuration),
          Some(company.id),
          Some(AccessLevel.ADMIN),
          None,
          defaultTokenCreationDate
        )
      )
    } yield (c, a)
  }

  def setupCaseNewCompany =
    for {
      (c, _) <- initCase
    } yield companyCases = companyCases :+ ((c, None, defaultTokenCreationDate))

  def setupCaseCompanyNotifiedOnce =
    for {
      (c, a) <- initCase
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, POST_ACCOUNT_ACTIVATION_DOC)
          .sample
          .get
          .copy(
            creationDate = OffsetDateTime.now.minusDays(1)
          )
      )
    } yield ()

  def setupCaseCompanyNotifiedOnceLongerThanDelay =
    for {
      (c, a) <- initCase
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, POST_ACCOUNT_ACTIVATION_DOC)
          .sample
          .get
          .copy(
            creationDate = OffsetDateTime.now.minus(reportReminderByPostDelay).minusDays(1)
          )
      )
    } yield companyCases = companyCases :+ ((c, None, defaultTokenCreationDate))

  def setupCaseCompanyNotifiedTwice =
    for {
      (c, a) <- initCase
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, POST_ACCOUNT_ACTIVATION_DOC)
          .sample
          .get
          .copy(
            creationDate = OffsetDateTime.now.minus(reportReminderByPostDelay.multipliedBy(2)).minusDays(1)
          )
      )
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, POST_ACCOUNT_ACTIVATION_DOC)
          .sample
          .get
          .copy(
            creationDate = OffsetDateTime.now.minus(reportReminderByPostDelay).minusDays(1)
          )
      )
    } yield ()

  def setupCaseCompanyNotifiedTwiceLongerThanDelay =
    for {
      (c, a) <- initCase
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, POST_ACCOUNT_ACTIVATION_DOC)
          .sample
          .get
          .copy(
            creationDate = OffsetDateTime.now.minus(reportReminderByPostDelay.multipliedBy(2)).minusDays(2)
          )
      )
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, POST_ACCOUNT_ACTIVATION_DOC)
          .sample
          .get
          .copy(
            creationDate = OffsetDateTime.now.minus(reportReminderByPostDelay.multipliedBy(2)).minusDays(1)
          )
      )
    } yield ()

  def setupCaseCompanyNoticeRequired =
    for {
      (c, a) <- initCase
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, POST_ACCOUNT_ACTIVATION_DOC)
          .sample
          .get
          .copy(creationDate = OffsetDateTime.now.minusDays(2))
      )
      _ <- eventRepository.create(
        Fixtures
          .genEventForCompany(c.id, ADMIN, ACTIVATION_DOC_REQUIRED)
          .sample
          .get
          .copy(creationDate = OffsetDateTime.now.minusDays(1))
      )
    } yield companyCases = companyCases :+ ((c, None, defaultTokenCreationDate))

  override def setupData() =
    Await.result(
      for {
        _ <- userRepository.create(adminUser)
        _ <- setupCaseNewCompany
        _ <- setupCaseCompanyNotifiedOnce
        _ <- setupCaseCompanyNotifiedOnceLongerThanDelay
        _ <- setupCaseCompanyNotifiedTwice
        _ <- setupCaseCompanyNoticeRequired

      } yield (),
      Duration.Inf
    )

  def loginInfo(user: User) = LoginInfo(CredentialsProvider.ID, user.email.value)

  implicit val env = new FakeEnvironment[AuthEnv](Seq(adminUser).map(user => loginInfo(user) -> user))

  val (app, components) = TestApp.buildApp(
    Some(
      env
    )
  )

}

class FetchCompaniesToActivateSpec(implicit ee: ExecutionEnv) extends BaseFetchCompaniesToActivateSpec {
  override def is = s2"""

The companies to activate endpoint should
  list companies with activation document to generate $e1
                                                    """

  def e1 = {
    val request = FakeRequest(GET, routes.CompanyController.companiesToActivate().toString)
      .withAuthenticator[AuthEnv](loginInfo(adminUser))
    val result = route(app, request).get
    status(result) must beEqualTo(OK)
    val content = contentAsJson(result).toString
    content must haveCompaniesToActivate(companyCases.map(c => aCompanyToActivate(c._1, c._2, c._3)): _*)
  }

  def aCompanyToActivate(
      company: Company,
      lastNotice: Option[OffsetDateTime],
      tokenCreation: OffsetDateTime
  ): Matcher[String] =
    lastNotice match {
      case Some(lastNotice) =>
        /("company") / ("id" -> company.id.toString) and
          /("lastNotice" -> startWith(lastNotice.format(DateTimeFormatter.ISO_LOCAL_DATE))) and
          /("tokenCreation" -> startWith(tokenCreation.format(DateTimeFormatter.ISO_LOCAL_DATE)))
      case None =>
        /("company") / ("id" -> company.id.toString) and
          /("tokenCreation" -> startWith(tokenCreation.format(DateTimeFormatter.ISO_LOCAL_DATE)))
    }

  def haveCompaniesToActivate(companiesToActivate: Matcher[String]*): Matcher[String] =
    have(TraversableMatchers.exactly(companiesToActivate: _*))

}
