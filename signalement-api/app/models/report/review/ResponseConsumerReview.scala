package models.report.review

import play.api.libs.json.Json
import play.api.libs.json.OFormat

import java.time.OffsetDateTime
import java.util.UUID

case class ResponseConsumerReview(
    id: ResponseConsumerReviewId,
    reportId: UUID,
    evaluation: ResponseEvaluation,
    creationDate: OffsetDateTime,
    details: Option[String]
)

object ResponseConsumerReview {
  implicit val ResponseConsumerReviewFormat: OFormat[ResponseConsumerReview] = Json.format[ResponseConsumerReview]
}
