package repositories.reportconsumerreview

import models.report.review.ResponseConsumerReviewId
import models.report.review.ResponseEvaluation
import play.api.Logger
import repositories.PostgresProfile.api._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import java.util.UUID

object ResponseConsumerReviewColumnType {

  val logger: Logger = Logger(this.getClass)

  implicit val ResponseEvaluationColumnType =
    MappedColumnType.base[ResponseEvaluation, String](
      _.entryName,
      ResponseEvaluation.namesToValuesMap
    )

  implicit val ResponseConsumerReviewIdColumnType
      : JdbcType[ResponseConsumerReviewId] with BaseTypedType[ResponseConsumerReviewId] =
    MappedColumnType.base[ResponseConsumerReviewId, UUID](
      _.value,
      ResponseConsumerReviewId(_)
    )

}
