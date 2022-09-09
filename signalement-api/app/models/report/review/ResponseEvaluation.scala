package models.report.review

import enumeratum._

sealed trait ResponseEvaluation extends EnumEntry

object ResponseEvaluation extends PlayEnum[ResponseEvaluation] {
  val values = findValues

  case object Positive extends ResponseEvaluation
  case object Negative extends ResponseEvaluation
  case object Neutral extends ResponseEvaluation
}
