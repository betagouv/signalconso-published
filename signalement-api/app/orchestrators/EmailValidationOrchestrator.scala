package orchestrators

import cats.implicits.catsSyntaxOption
import config.EmailConfiguration
import controllers.error.AppError
import eu.timepit.refined.api.RefType
import models.EmailApi.EmailString
import models.EmailValidation
import models.EmailValidationFilter
import models.PaginatedResult
import models.PaginatedSearch
import services.MailService
import utils.EmailAddress
import models.email.EmailValidationResult
import models.email.ValidateEmailCode
import play.api.Logger
import repositories.emailvalidation.EmailValidationRepositoryInterface
import services.Email.ConsumerValidateEmail

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class EmailValidationOrchestrator(
    mailService: MailService,
    emailValidationRepository: EmailValidationRepositoryInterface,
    emailConfiguration: EmailConfiguration
)(implicit
    executionContext: ExecutionContext
) {

  private[this] val logger = Logger(this.getClass)

  def isEmailValid(email: EmailAddress): Future[Boolean] =
    for {
      emailValidation <- emailValidationRepository.findByEmail(email)
    } yield emailValidation.exists(_.lastValidationDate.isDefined)

  def checkCodeAndValidateEmail(emailValidationBody: ValidateEmailCode) =
    for {
      maybeEmailValidation <- emailValidationRepository.findByEmail(emailValidationBody.email)
      emailValidation <- maybeEmailValidation.liftTo[Future] {
        logger.warn(s"Email ${emailValidationBody.email.value} to validate not found")
        AppError.EmailOrCodeIncorrect(emailValidationBody.email)
      }
      _ = logger.debug("validating code")
      result <- checkCodeAndValidate(emailValidationBody, emailValidation)
    } yield result

  def checkEmail(email: EmailAddress): Future[EmailValidationResult] = for {
    _ <- validateProvider(email)
    result <-
      if (emailConfiguration.skipReportEmailValidation) {
        validateFormat(email)
      } else sendValidationEmailIfNeeded(email)
  } yield result

  def validateEmail(email: EmailAddress): Future[EmailValidationResult] =
    emailValidationRepository.validate(email).map { _ =>
      logger.debug("Email validated")
      EmailValidationResult.success
    }

  private[this] def checkCodeAndValidate(emailValidationBody: ValidateEmailCode, emailValidation: EmailValidation) =
    if (emailValidation.confirmationCode == emailValidationBody.confirmationCode) {
      validateEmail(emailValidationBody.email)
    } else
      emailValidationRepository
        .update(
          emailValidation.copy(
            attempts = emailValidation.attempts + 1,
            lastAttempt = Some(OffsetDateTime.now)
          )
        )
        .map { _ =>
          logger.debug("Invalid code")
          EmailValidationResult.invalidCode
        }

  private[this] def sendValidationEmailIfNeeded(email: EmailAddress): Future[EmailValidationResult] = for {
    emailValidation <- findOrCreate(email)
    res <-
      if (emailValidation.lastValidationDate.isEmpty) {
        logger.debug(s"Email ${emailValidation.email} not validated, sending email")
        mailService.send(ConsumerValidateEmail(emailValidation)).map(_ => EmailValidationResult.failure)
      } else {
        logger.debug(s"Email validated")
        Future.successful(EmailValidationResult.success)
      }
  } yield res

  private[this] def validateProvider(email: EmailAddress): Future[Unit] =
    if (emailConfiguration.emailProvidersBlocklist.exists(email.value.contains(_))) {
      Future.failed(AppError.InvalidEmailProvider)
    } else {
      Future.successful(())
    }

  private[this] def validateFormat(emailAddress: EmailAddress): Future[EmailValidationResult] = {
    logger.debug(s"Checking if email match EmailStringRegex.type regexp ")
    RefType
      .applyRef[EmailString](emailAddress.value)
      .fold(
        _ => Future.failed(AppError.InvalidEmail(emailAddress.value)),
        _ => Future.successful(EmailValidationResult.success)
      )
  }

  private[this] def findOrCreate(email: EmailAddress): Future[EmailValidation] =
    emailValidationRepository.findByEmail(email).flatMap {
      case None =>
        logger.debug(s"Unknown email , creating validation entry")
        emailValidationRepository.create(EmailValidation(email = email))
      case Some(foundEmail) =>
        logger.debug(s"Found email in validation email table ")
        Future(foundEmail)
    }

  def search(search: EmailValidationFilter, paginate: PaginatedSearch): Future[PaginatedResult[EmailValidation]] =
    emailValidationRepository.search(search, paginate)
}
