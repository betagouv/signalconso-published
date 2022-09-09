package company.entrepriseimportinfo

import company.EnterpriseImportInfo
import repositories.PostgresProfile.api._
import java.time.OffsetDateTime
import java.util.UUID

class EnterpriseImportInfoTable(tag: Tag) extends Table[EnterpriseImportInfo](tag, "etablissements_import_info") {
  def id = column[UUID]("id", O.PrimaryKey)
  def fileName = column[String]("file_name")
  def fileUrl = column[String]("file_url")
  def linesCount = column[Double]("lines_count")
  def linesDone = column[Double]("lines_done")
  def startedAt = column[OffsetDateTime]("started_at")
  def endedAt = column[Option[OffsetDateTime]]("ended_at")
  def errors = column[Option[String]]("errors")

  def * = (
    id,
    fileName,
    fileUrl,
    linesCount,
    linesDone,
    startedAt,
    endedAt,
    errors
  ) <> ((EnterpriseImportInfo.apply _).tupled, EnterpriseImportInfo.unapply)
}

object EnterpriseImportInfoTable {
  val table = TableQuery[EnterpriseImportInfoTable]
}
