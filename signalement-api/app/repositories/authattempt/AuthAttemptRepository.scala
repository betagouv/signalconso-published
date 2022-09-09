package repositories.authattempt

import models.auth.AuthAttempt
import play.api.Logger
import repositories.CRUDRepository
import repositories.PostgresProfile.api._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.time.OffsetDateTime
import java.time.ZoneOffset
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class AuthAttemptRepository(
    override val dbConfig: DatabaseConfig[JdbcProfile]
)(implicit override val ec: ExecutionContext)
    extends CRUDRepository[AuthAttemptTable, AuthAttempt]
    with AuthAttemptRepositoryInterface {

  override val table: TableQuery[AuthAttemptTable] = AuthAttemptTable.table
  import dbConfig._
  val logger: Logger = Logger(this.getClass)

  override def countAuthAttempts(login: String, delay: Duration): Future[Int] = db
    .run(
      table
        .filter(_.login === login)
        .filter(_.timestamp >= OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(delay.toMinutes))
        .length
        .result
    )

  override def listAuthAttempts(login: String): Future[Seq[AuthAttempt]] = db
    .run(
      table
        .filter(_.login === login)
        .result
    )

}
