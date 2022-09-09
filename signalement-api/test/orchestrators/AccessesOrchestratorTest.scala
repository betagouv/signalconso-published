package orchestrators

import org.specs2.mutable.Specification
import utils.AppSpec
import utils.EmailAddress
import utils.Fixtures
import utils.TestApp
import controllers.error.AppError._
import models.AccessToken
import models.User
import models.token.TokenKind.DGCCRFAccount
import models.token.TokenKind.ValidateEmail
import org.specs2.concurrent.ExecutionEnv

import java.time.Period
import java.util.UUID
import scala.concurrent.Future

class AccessesOrchestratorTest extends Specification with AppSpec {

  override def afterAll(): Unit = {
    app.stop()
    ()
  }

  val (app, components) = TestApp.buildApp()

  implicit val ec = components.executionContext
  implicit val ee = ExecutionEnv.fromExecutionContext(ec)

  "AccessesOrchestratorTest" should {

    "extend DGCCRF account" in {

      "it should fail when user is not found" >> {
        val unknownUser = EmailAddress("unknown@gmail.com")
        components.accessesOrchestrator
          .resetLastEmailValidation(unknownUser) must throwA[UserNotFound].await
      }

      "it should fail when user is not a DGCCRF user" >> {

        val adminUser: User = Fixtures.genAdminUser.sample.get

        val result = for {
          _ <- components.userRepository.create(adminUser)
          res <- components.accessesOrchestrator
            .resetLastEmailValidation(adminUser.email)
        } yield res

        result must throwA[CantPerformAction.type].await
      }

      "it should extends user validity and reset current validate email token" >> {

        val dgccrfUser: User = Fixtures.genDgccrfUser.sample.get.copy(lastEmailValidation = None)

        val validationEmailToken = AccessToken.build(
          kind = ValidateEmail,
          token = UUID.randomUUID.toString,
          validity = Some(Period.ofDays(1)),
          companyId = None,
          level = None,
          emailedTo = Some(dgccrfUser.email)
        )

        val result: Future[(User, Option[AccessToken])] = for {
          savedToken <- components.accessTokenRepository.create(validationEmailToken)
          _ <- components.userRepository.create(dgccrfUser)
          user <- components.accessesOrchestrator.resetLastEmailValidation(dgccrfUser.email)
          updatedToken <- components.accessTokenRepository.get(savedToken.id)
        } yield (user, updatedToken)

        result.map { case (user, _) => user.lastEmailValidation.isDefined } must beTrue.await
        result.map { case (_, maybeToken) => maybeToken.map(_.valid) } must beSome(false).await
      }

      "it should extends user validity when not validation email token has been created" >> {

        val dgccrfUser: User = Fixtures.genDgccrfUser.sample.get.copy(lastEmailValidation = None)

        val result: Future[User] = for {
          _ <- components.userRepository.create(dgccrfUser)
          user <- components.accessesOrchestrator.resetLastEmailValidation(dgccrfUser.email)
        } yield user

        result.map(_.lastEmailValidation.isDefined) must beTrue.await
      }

    }

    "validate DGCCRF email" in {

      "email validation should fail when token not found" >> {
        val unknownToken = ""
        components.accessesOrchestrator
          .validateDGCCRFEmail(unknownToken) must throwA[DGCCRFActivationTokenNotFound].await
      }

      "email validation should fail when no validation email token  found" >> {
        val nonRelevantToken = AccessToken.build(
          kind = DGCCRFAccount,
          token = UUID.randomUUID.toString,
          validity = Some(Period.ofDays(1)),
          companyId = None,
          level = None,
          emailedTo = Some(EmailAddress("email@signal.conso.com"))
        )
        val result = for {
          _ <- components.accessTokenRepository.create(nonRelevantToken)
          res <- components.accessesOrchestrator.validateDGCCRFEmail(nonRelevantToken.token)

        } yield res

        result must throwA[DGCCRFActivationTokenNotFound].await
      }

      "email validation should fail when validation email token found not link to any email" >> {
        val invalidValidationEmailToken = AccessToken.build(
          kind = ValidateEmail,
          token = UUID.randomUUID.toString,
          validity = Some(Period.ofDays(1)),
          companyId = None,
          level = None,
          emailedTo = None
        )

        val result = for {
          _ <- components.accessTokenRepository.create(invalidValidationEmailToken)
          res <- components.accessesOrchestrator.validateDGCCRFEmail(invalidValidationEmailToken.token)

        } yield res

        result must throwA[ServerError].await
      }

      "email validation should fail when token email not linked to any users" >> {
        val validationEmailToken = AccessToken.build(
          kind = ValidateEmail,
          token = UUID.randomUUID.toString,
          validity = Some(Period.ofDays(1)),
          companyId = None,
          level = None,
          emailedTo = Some(EmailAddress("email@signal.conso.com"))
        )

        val result = for {
          _ <- components.accessTokenRepository.create(validationEmailToken)
          res <- components.accessesOrchestrator.validateDGCCRFEmail(validationEmailToken.token)

        } yield res

        result must throwA[UserNotFound].await
      }

      "email validation should be a success" >> {

        val dgccrfUser: User = Fixtures.genDgccrfUser.sample.get.copy(lastEmailValidation = None)

        val validationEmailToken = AccessToken.build(
          kind = ValidateEmail,
          token = UUID.randomUUID.toString,
          validity = Some(Period.ofDays(1)),
          companyId = None,
          level = None,
          emailedTo = Some(dgccrfUser.email)
        )

        val result = for {
          _ <- components.accessTokenRepository.create(validationEmailToken)
          _ <- components.userRepository.create(dgccrfUser)
          res <- components.accessesOrchestrator.validateDGCCRFEmail(validationEmailToken.token)

        } yield res

        result.map(_.lastEmailValidation.isDefined) must beTrue.await
      }
    }
  }
}
