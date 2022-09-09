package repositories.subscription

import models.Subscription
import repositories.CRUDRepository
import repositories.PostgresProfile
import repositories.user.UserTable
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import utils.EmailAddress
import utils.EmailAddress.EmailColumnType

import java.time.Period
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import PostgresProfile.api._
import slick.basic.DatabaseConfig

class SubscriptionRepository(override val dbConfig: DatabaseConfig[JdbcProfile])(implicit
    override val ec: ExecutionContext
) extends CRUDRepository[SubscriptionTable, Subscription]
    with SubscriptionRepositoryInterface {

  override val table: TableQuery[SubscriptionTable] = SubscriptionTable.table
  import dbConfig._

  override def list(userId: UUID): Future[List[Subscription]] = db
    .run(
      SubscriptionTable.table
        .filter(_.userId === userId)
        .sortBy(_.creationDate.desc)
        .to[List]
        .result
    )

  override def listForFrequency(frequency: Period): Future[List[(Subscription, EmailAddress)]] = db
    .run(
      SubscriptionTable.table
        .filter(_.frequency === frequency)
        .joinLeft(UserTable.table)
        .on(_.userId === _.id)
        .map(s => (s._1, s._1.email.ifNull(s._2.map(_.email)).get))
        .to[List]
        .result
    )

  override def getDirectionDepartementaleEmail(department: String): Future[Seq[EmailAddress]] =
    db.run(
      SubscriptionTable.table
        .filter(_.email.isDefined)
        .filter(_.userId.isEmpty)
        .filter(x => x.departments @> List(department))
        .filter(x => x.email.map(_.asColumnOf[String]) like s"dd%")
        .result
    ).map(_.map(_.email.get).distinct)
}
