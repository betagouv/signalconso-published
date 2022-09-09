package repositories.consumer
import models.Consumer
import repositories.CRUDRepositoryInterface

import scala.concurrent.Future

trait ConsumerRepositoryInterface extends CRUDRepositoryInterface[Consumer] {
  def getAll(): Future[Seq[Consumer]]
}
