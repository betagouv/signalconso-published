package models.report.review

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class ResponseConsumerReviewApi(
    evaluation: ResponseEvaluation,
    details: Option[String]
)

object ResponseConsumerReviewApi {
  implicit val reviewOnReportResponse: OFormat[ResponseConsumerReviewApi] = Json.format[ResponseConsumerReviewApi]
}
