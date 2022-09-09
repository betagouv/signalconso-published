package repositories.reportconsumerreview

import models.report.review.ResponseConsumerReview
import models.report.review.ResponseConsumerReviewId
import models.report.review.ResponseEvaluation
import repositories.TypedDatabaseTable

import java.time.OffsetDateTime
import java.util.UUID
import repositories.PostgresProfile.api._
import repositories.reportconsumerreview.ResponseConsumerReviewColumnType._

class ResponseConsumerReviewTable(tag: Tag)
    extends TypedDatabaseTable[ResponseConsumerReview, ResponseConsumerReviewId](tag, "report_consumer_review") {

  def reportId = column[UUID]("report_id")
  def creationDate = column[OffsetDateTime]("creation_date")
  def evaluation = column[ResponseEvaluation]("evaluation")
  def details = column[Option[String]]("details")

  type ResponseConsumerReviewData = (
      ResponseConsumerReviewId,
      UUID,
      ResponseEvaluation,
      OffsetDateTime,
      Option[String]
  )

  def constructResponseConsumerReview: ResponseConsumerReviewData => ResponseConsumerReview = {
    case (id, reportId, evaluation, creationDate, details) =>
      ResponseConsumerReview(id, reportId, evaluation, creationDate, details)
  }

  def extractResponseConsumerReview: PartialFunction[ResponseConsumerReview, ResponseConsumerReviewData] = {
    case ResponseConsumerReview(id, reportId, evaluation, creationDate, details) =>
      (id, reportId, evaluation, creationDate, details)
  }

  def * =
    (
      id,
      reportId,
      evaluation,
      creationDate,
      details
    ) <> (constructResponseConsumerReview, extractResponseConsumerReview.lift)
}

object ResponseConsumerReviewTable {
  val table = TableQuery[ResponseConsumerReviewTable]
}
