package repositories.reportblockednotification

import models.report.ReportBlockedNotification
import repositories.company.CompanyTable
import repositories.user.UserTable
import repositories.PostgresProfile.api._
import java.time.OffsetDateTime
import java.util.UUID

class ReportBlockedNotificationTable(tag: Tag)
    extends Table[ReportBlockedNotification](tag, "report_notifications_blocked") {
  def userId = column[UUID]("user_id")
  def companyId = column[UUID]("company_id")
  def dateCreation = column[OffsetDateTime]("date_creation")

  def company = foreignKey("fk_report_notification_blocklist_user", companyId, CompanyTable.table)(
    _.id,
    onDelete = ForeignKeyAction.Cascade
  )
  def user = foreignKey("fk_report_notification_blocklist_user", userId, UserTable.table)(
    _.id,
    onDelete = ForeignKeyAction.Cascade
  )

  def * = (
    userId,
    companyId,
    dateCreation
  ) <> ((ReportBlockedNotification.apply _).tupled, ReportBlockedNotification.unapply)
}

object ReportNotificationBlocklistTable {
  val table = TableQuery[ReportBlockedNotificationTable]
}
