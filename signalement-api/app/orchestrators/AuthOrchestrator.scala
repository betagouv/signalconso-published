package orchestrators

import cats.implicits.catsSyntaxEq
import cats.implicits.catsSyntaxMonadError
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.error.AppError.DGCCRFUserEmailValidationExpired
import controllers.error.AppError.InvalidPassword
import controllers.error.AppError.PasswordTokenNotFoundOrInvalid
import controllers.error.AppError.SamePasswordError
import controllers.error.AppError.ServerError
import controllers.error.AppError.TooMuchAuthAttempts
import controllers.error.AppError.UserNotFound
import models.User
import models.UserRole
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.UserService

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import cats.syntax.option._
import cats.instances.future.catsStdInstancesForFuture
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.exceptions.InvalidPasswordException
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import config.TokenConfiguration
import controllers.error.AppError
import models.auth.AuthAttempt
import models.auth.AuthToken
import models.auth.PasswordChange
import models.auth.UserCredentials
import models.auth.UserLogin
import models.auth.UserPassword
import models.auth.UserSession
import orchestrators.AuthOrchestrator.AuthAttemptPeriod
import orchestrators.AuthOrchestrator.MaxAllowedAuthAttempts
import orchestrators.AuthOrchestrator.authTokenExpiration
import orchestrators.AuthOrchestrator.toLoginInfo
import play.api.Logger
import play.api.mvc.Request
import repositories.authattempt.AuthAttemptRepositoryInterface
import repositories.authtoken.AuthTokenRepositoryInterface
import repositories.user.UserRepositoryInterface
import services.Email.ResetPassword
import services.MailService

import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID

class AuthOrchestrator(
    userService: UserService,
    authAttemptRepository: AuthAttemptRepositoryInterface,
    userRepository: UserRepositoryInterface,
    accessesOrchestrator: AccessesOrchestrator,
    authTokenRepository: AuthTokenRepositoryInterface,
    tokenConfiguration: TokenConfiguration,
    credentialsProvider: CredentialsProvider,
    mailService: MailService,
    val silhouette: Silhouette[AuthEnv]
)(implicit
    ec: ExecutionContext
) {

  private val logger: Logger = Logger(this.getClass)
  private val dgccrfDelayBeforeRevalidation: Period = tokenConfiguration.dgccrfDelayBeforeRevalidation

  def login(userLogin: UserCredentials, request: Request[_]): Future[UserSession] = {
    val eventualUserSession: Future[UserSession] = for {
      maybeUser <- userService.retrieve(toLoginInfo(userLogin.login))
      user <- maybeUser.liftTo[Future](UserNotFound(userLogin.login))
      _ = logger.debug(s"Found user")
      _ = logger.debug(s"Validate auth attempts count")
      _ <- validateAuthenticationAttempts(user)
      _ = logger.debug(s"Check last validation email for DGCCRF users")
      _ <- validateDGCCRFAccountLastEmailValidation(user)
      _ = logger.debug(s"Successful login for user")
      token <- getToken(userLogin)(request)
      _ = logger.debug(s"Successful generated token for user")
    } yield UserSession(token, user)

    eventualUserSession
      .flatMap { session =>
        logger.debug(s"Saving auth attempts for user")
        authAttemptRepository.create(AuthAttempt.build(userLogin.login, isSuccess = true)).map(_ => session)
      }
      .recoverWith {
        case error: AppError =>
          logger.debug(s"Saving failed auth attempt for user")
          authAttemptRepository
            .create(AuthAttempt.build(userLogin.login, isSuccess = false, failureCause = Some(error.details)))
            .flatMap(_ => Future.failed(error))
        case error =>
          logger.debug(s"Saving failed auth attempt for user")
          authAttemptRepository
            .create(
              AuthAttempt.build(
                userLogin.login,
                isSuccess = false,
                failureCause = Some(s"Unexpected error : ${error.getMessage}")
              )
            )
            .flatMap(_ => Future.failed(error))

      }

  }

  def forgotPassword(resetPasswordLogin: UserLogin): Future[Unit] =
    userService.retrieve(toLoginInfo(resetPasswordLogin.login)).flatMap {
      case Some(user) =>
        for {
          _ <- authTokenRepository.deleteForUserId(user.id)
          _ = logger.debug(s"Creating auth token for ${user.id}")
          authToken <- authTokenRepository.create(AuthToken(UUID.randomUUID(), user.id, authTokenExpiration))
          _ = logger.debug(s"Sending reset email to ${user.id}")
          _ <- mailService.send(ResetPassword(user, authToken))
        } yield ()
      case _ =>
        logger.warn("Unable to reset password for user")
        Future.successful(())
    }

  def resetPassword(token: UUID, userPassword: UserPassword): Future[Unit] =
    authTokenRepository.findValid(token).flatMap {
      case Some(authToken) =>
        logger.debug(s"Found token for user id ${authToken.userID}")
        for {
          _ <- userRepository.updatePassword(authToken.userID, userPassword.password)
          _ = logger.debug(s"Password updated successfully for user id ${authToken.userID}")
          _ <- authTokenRepository.deleteForUserId(authToken.userID)
          _ = logger.debug(s"Token deleted successfully for user id ${authToken.userID}")
        } yield ()
      case None =>
        val error = PasswordTokenNotFoundOrInvalid(token)
        logger.warn(error.title)
        Future.failed(error)
    }

  def changePassword(user: User, passwordChange: PasswordChange) = for {
    _ <-
      if (passwordChange.oldPassword === passwordChange.newPassword) {
        Future.failed(SamePasswordError)
      } else {
        Future.unit
      }
    _ <- authenticate(user.email.value, passwordChange.oldPassword)
    _ = logger.debug(s"Successfully checking old password  user id ${user.id}, updating password")
    _ <- userRepository.updatePassword(user.id, passwordChange.newPassword)
    _ = logger.debug(s"Password updated for user id ${user.id}")
  } yield ()

  private def getToken(userLogin: UserCredentials)(implicit req: Request[_]): Future[String] =
    for {
      loginInfo <- authenticate(userLogin.login, userLogin.password)
      authenticator <- silhouette.env.authenticatorService.create(loginInfo)
      token <- silhouette.env.authenticatorService.init(authenticator)
    } yield token

  private def authenticate(login: String, password: String) = {
    val passwordHasherRegistry: PasswordHasherRegistry = PasswordHasherRegistry(
      new BCryptPasswordHasher()
    )

    passwordHasherRegistry.current.hash(password)
    credentialsProvider
      .authenticate(Credentials(login, password))
      .recoverWith {
        case e: InvalidPasswordException =>
          logger.warn("Invalid password ", e)
          Future.failed(InvalidPassword(login))
        case _: IdentityNotFoundException => Future.failed(UserNotFound(login))
        case err => Future.failed(ServerError("Unexpected error when authenticating user", Some(err)))
      }
  }

  private def validateDGCCRFAccountLastEmailValidation(user: User): Future[User] = user.userRole match {
    case UserRole.DGCCRF if needsEmailRevalidation(user) =>
      accessesOrchestrator
        .sendEmailValidation(user)
        .flatMap(_ => throw DGCCRFUserEmailValidationExpired(user.email.value))
    case _ =>
      logger.debug(s"No periodic email revalidation needed for the user")
      Future.successful(user)
  }

  private def needsEmailRevalidation(user: User) =
    user.lastEmailValidation
      .exists(
        _.isBefore(
          OffsetDateTime.now
            .minus(dgccrfDelayBeforeRevalidation)
        )
      )

  private def validateAuthenticationAttempts(user: User): Future[User] = for {
    _ <- authAttemptRepository
      .countAuthAttempts(user.email.value, AuthAttemptPeriod)
      .ensure(TooMuchAuthAttempts(user.id))(attempts => attempts < MaxAllowedAuthAttempts)
    _ = logger.debug(s"Auth attempts count check successful")
  } yield user

}

object AuthOrchestrator {
  val AuthAttemptPeriod: Duration = 30 minutes
  val MaxAllowedAuthAttempts: Int = 5
  def authTokenExpiration: OffsetDateTime = OffsetDateTime.now.plusDays(1)
  def toLoginInfo(login: String): LoginInfo = LoginInfo(CredentialsProvider.ID, login)
}
