package models.report.review

import play.api.libs.json.Format
import play.api.libs.json.Json

import java.util.UUID

case class ResponseConsumerReviewId(value: UUID) extends AnyVal

object ResponseConsumerReviewId {
  implicit val ResponseConsumerReviewIdFormat: Format[ResponseConsumerReviewId] =
    Json.valueFormat[ResponseConsumerReviewId]

  def generateId() = new ResponseConsumerReviewId(UUID.randomUUID())
}
