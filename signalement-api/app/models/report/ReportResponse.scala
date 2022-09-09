package models.report

import enumeratum.EnumEntry
import enumeratum.PlayEnum
import models.report.reportfile.ReportFileId
import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class ReportResponse(
    responseType: ReportResponseType,
    consumerDetails: String,
    dgccrfDetails: Option[String],
    fileIds: List[ReportFileId]
)

object ReportResponse {
  implicit val reportResponse: OFormat[ReportResponse] = Json.format[ReportResponse]
}

sealed trait ReportResponseType extends EnumEntry

object ReportResponseType extends PlayEnum[ReportResponseType] {

  final case object ACCEPTED extends ReportResponseType
  final case object REJECTED extends ReportResponseType
  final case object NOT_CONCERNED extends ReportResponseType

  override def values: IndexedSeq[ReportResponseType] = findValues

}
