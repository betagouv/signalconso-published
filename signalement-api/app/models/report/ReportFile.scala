package models.report

import models.report.reportfile.ReportFileId
import play.api.libs.json._

import java.time.OffsetDateTime
import java.util.UUID

sealed case class ReportFileOrigin(value: String)

object ReportFileOrigin {
  val CONSUMER = ReportFileOrigin("consumer")
  val PROFESSIONAL = ReportFileOrigin("professional")

  implicit def reportFileOriginWrites = new Writes[ReportFileOrigin] {
    def writes(reportFileOrigin: ReportFileOrigin) = Json.toJson(reportFileOrigin.value)
  }
  implicit val reportFileOriginReads: Reads[ReportFileOrigin] = JsPath.read[String].map(ReportFileOrigin(_))
}

case class ReportFile(
    id: ReportFileId,
    reportId: Option[UUID],
    creationDate: OffsetDateTime,
    filename: String,
    storageFilename: String,
    origin: ReportFileOrigin,
    avOutput: Option[String]
)
object ReportFile {
  implicit val fileFormat: OFormat[ReportFile] = Json.format[ReportFile]
}
