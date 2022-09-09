package repositories.consumer

import models._
import repositories.CRUDRepository
import repositories.PostgresProfile.api._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ConsumerRepository(
    override val dbConfig: DatabaseConfig[JdbcProfile]
)(implicit override val ec: ExecutionContext)
    extends CRUDRepository[ConsumerTable, Consumer]
    with ConsumerRepositoryInterface {

  import dbConfig._

  override val table = ConsumerTable.table

  override def getAll(): Future[Seq[Consumer]] =
    db.run(table.filter(_.deleteDate.isEmpty).result)

}
