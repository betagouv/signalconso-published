package orchestrators

import cats.implicits.catsSyntaxMonadError
import controllers.error.AppError.EmailAlreadyExist
import controllers.error.AppError.UserNotFound
import models.AccessToken
import models.DraftUser
import models.User
import models.UserRole
import models.UserUpdate
import play.api.Logger
import repositories.user.UserRepositoryInterface
import utils.EmailAddress

import java.time.OffsetDateTime
import cats.syntax.option._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait UserOrchestratorInterface {
  def createUser(draftUser: DraftUser, accessToken: AccessToken, role: UserRole): Future[User]

  def findOrError(emailAddress: EmailAddress): Future[User]

  def find(emailAddress: EmailAddress): Future[Option[User]]

  def edit(userId: UUID, update: UserUpdate): Future[Option[User]]
}

class UserOrchestrator(userRepository: UserRepositoryInterface)(implicit ec: ExecutionContext)
    extends UserOrchestratorInterface {
  val logger: Logger = Logger(this.getClass)

  override def edit(id: UUID, update: UserUpdate): Future[Option[User]] =
    for {
      userOpt <- userRepository.get(id)
      updatedUser <- userOpt
        .map(user => userRepository.update(user.id, update.mergeToUser(user)).map(Some(_)))
        .getOrElse(Future(None))
    } yield updatedUser

  override def createUser(draftUser: DraftUser, accessToken: AccessToken, role: UserRole): Future[User] = {
    val email: EmailAddress = accessToken.emailedTo.getOrElse(draftUser.email)
    val user = User(
      id = UUID.randomUUID,
      password = draftUser.password,
      email = email,
      firstName = draftUser.firstName,
      lastName = draftUser.lastName,
      userRole = role,
      lastEmailValidation = Some(OffsetDateTime.now)
    )
    for {
      _ <- userRepository.findByLogin(draftUser.email.value).ensure(EmailAlreadyExist)(user => user.isEmpty)
      _ <- userRepository.create(user)
    } yield user
  }

  override def findOrError(emailAddress: EmailAddress): Future[User] =
    userRepository
      .findByLogin(emailAddress.value)
      .flatMap(_.liftTo[Future](UserNotFound(emailAddress.value)))

  override def find(emailAddress: EmailAddress): Future[Option[User]] =
    userRepository
      .findByLogin(emailAddress.value)
}
