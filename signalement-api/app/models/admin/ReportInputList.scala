package models.admin

import play.api.libs.json.Json
import play.api.libs.json.OFormat

import java.util.UUID

case class ReportInputList(reportIds: List[UUID])

object ReportInputList {
  implicit val ReportInputListFormat: OFormat[ReportInputList] = Json.format[ReportInputList]
}
