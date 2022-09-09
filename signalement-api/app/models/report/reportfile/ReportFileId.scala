package models.report.reportfile

import play.api.libs.json.Format
import play.api.libs.json.Json

import java.util.UUID

case class ReportFileId(value: UUID) extends AnyVal

object ReportFileId {
  implicit val ReportFileIdFormat: Format[ReportFileId] =
    Json.valueFormat[ReportFileId]

  def generateId() = new ReportFileId(UUID.randomUUID())
}
