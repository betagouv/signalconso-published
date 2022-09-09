package repositories.asyncfiles
import models.AsyncFile
import models.AsyncFileKind
import models.User
import repositories.CRUDRepositoryInterface

import java.util.UUID
import scala.concurrent.Future

trait AsyncFileRepositoryInterface extends CRUDRepositoryInterface[AsyncFile] {

  def update(uuid: UUID, filename: String, storageFilename: String): Future[Int]

  def list(user: User, kind: Option[AsyncFileKind] = None): Future[List[AsyncFile]]

  def deleteByUserId(userId: UUID): Future[Int]
}
