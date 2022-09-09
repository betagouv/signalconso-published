package repositories.authtoken
import models.auth.AuthToken
import repositories.CRUDRepositoryInterface

import java.util.UUID
import scala.concurrent.Future

trait AuthTokenRepositoryInterface extends CRUDRepositoryInterface[AuthToken] {

  def findValid(id: UUID): Future[Option[AuthToken]]

  def deleteForUserId(userId: UUID): Future[Int]

  def findForUserId(userId: UUID): Future[Seq[AuthToken]]
}
