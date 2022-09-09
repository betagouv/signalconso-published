package repositories.rating

import models.Rating
import repositories.CRUDRepository
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class RatingRepository(override val dbConfig: DatabaseConfig[JdbcProfile])(implicit
    override val ec: ExecutionContext
) extends CRUDRepository[RatingTable, Rating]
    with RatingRepositoryInterface {

  override val table = RatingTable.table

}
