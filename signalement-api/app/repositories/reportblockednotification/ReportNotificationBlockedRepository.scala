package repositories.reportblockednotification

import models.report.ReportBlockedNotification
import repositories.PostgresProfile.api._
import repositories.user.UserTable
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import utils.EmailAddress

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ReportNotificationBlockedRepository(val dbConfig: DatabaseConfig[JdbcProfile])(implicit
    ec: ExecutionContext
) extends ReportNotificationBlockedRepositoryInterface {

  import dbConfig._

  override def findByUserId(userId: UUID): Future[Seq[ReportBlockedNotification]] =
    db.run(ReportNotificationBlocklistTable.table.filter(_.userId === userId).result)

  override def filterBlockedEmails(email: Seq[EmailAddress], companyId: UUID): Future[Seq[EmailAddress]] =
    db.run(
      UserTable.table
        .filter(_.id in (ReportNotificationBlocklistTable.table.filter(_.companyId === companyId).map(_.userId)))
        .map(_.email)
        .to[List]
        .result
    ).map { blockedEmails =>
      email.diff(blockedEmails)
    }

  override def create(userId: UUID, companyIds: Seq[UUID]): Future[Seq[ReportBlockedNotification]] = {
    val entities = companyIds.map(companyId => ReportBlockedNotification(userId = userId, companyId = companyId))
    db.run(ReportNotificationBlocklistTable.table ++= entities).map(_ => entities)
  }

  override def delete(userId: UUID, companyIds: Seq[UUID]): Future[Int] =
    db.run(
      ReportNotificationBlocklistTable.table.filter(_.userId === userId).filter(_.companyId inSet companyIds).delete
    )
}
