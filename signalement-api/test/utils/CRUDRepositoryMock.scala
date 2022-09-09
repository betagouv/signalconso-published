package utils

import repositories.CRUDRepositoryInterface

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.Future

class CRUDRepositoryMock[E](database: mutable.Map[UUID, E], getId: E => UUID) extends CRUDRepositoryInterface[E] {

  def create(element: E): Future[E] =
    Future.successful {
      database
        .put(getId(element), element)
      element
    }

  def update(id: UUID, element: E): Future[E] = Future.successful {
    database
      .update(getId(element), element)
    element
  }

  def get(id: UUID): Future[Option[E]] = Future.successful(database.get(id))

  def delete(id: UUID): Future[Int] = Future.successful(database.remove(id).fold(0)(_ => 1))

  def list(): Future[List[E]] = Future.successful(database.view.values.toList)

  override def createOrUpdate(element: E): Future[E] = Future.successful {
    database
      .get(getId(element))
      .map(_ => database.update(getId(element), element))
      .getOrElse {
        database
          .put(getId(element), element)
        ()
      }
    element
  }
}
