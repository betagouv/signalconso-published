package repositories

import repositories.PostgresProfile.api._
import slick.ast.BaseTypedType
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait TypedCRUDRepositoryInterface[E, ID] {

  def create(element: E): Future[E]

  def update(id: ID, element: E): Future[E]

  def createOrUpdate(element: E): Future[E]

  def get(id: ID): Future[Option[E]]

  def delete(id: ID): Future[Int]

  def list(): Future[List[E]]

}

trait CRUDRepositoryInterface[E] extends TypedCRUDRepositoryInterface[E, UUID]

abstract class TypedCRUDRepository[T <: TypedDatabaseTable[E, ID], E, ID](implicit tt: BaseTypedType[ID])
    extends TypedCRUDRepositoryInterface[E, ID] {

  val table: TableQuery[T]
  implicit val ec: ExecutionContext
  val dbConfig: DatabaseConfig[JdbcProfile]

  import dbConfig._

  def create(element: E): Future[E] = db
    .run(
      table returning table += element
    )
    .map(_ => element)

  def createOrUpdate(element: E): Future[E] = db
    .run(
      table.insertOrUpdate(element)
    )
    .map(_ => element)

  def update(id: ID, element: E): Future[E] =
    db.run(
      table.filter(_.id === id).update(element)
    ).map(_ => element)

  def get(id: ID): Future[Option[E]] = db.run(
    table.filter(_.id === id).result.headOption
  )

  def delete(id: ID): Future[Int] = db.run(
    table.filter(_.id === id).delete
  )

  def list(): Future[List[E]] = db.run(table.to[List].result)

}

abstract class CRUDRepository[T <: DatabaseTable[E], E] extends TypedCRUDRepository[T, E, UUID]
