package repositories.user

import models.User
import models.UserRole
import repositories.CRUDRepositoryInterface
import utils.EmailAddress

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

trait UserRepositoryInterface extends CRUDRepositoryInterface[User] {

  def listExpiredDGCCRF(expirationDate: OffsetDateTime): Future[List[User]]

  def list(role: UserRole): Future[Seq[User]]

  def create(user: User): Future[User]

  def updatePassword(userId: UUID, password: String): Future[Int]

  def delete(email: EmailAddress): Future[Int]

  def findByLogin(login: String): Future[Option[User]]
}
