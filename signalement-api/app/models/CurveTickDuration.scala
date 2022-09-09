package models

import enumeratum.EnumEntry
import enumeratum.PlayEnum

sealed abstract trait CurveTickDuration extends EnumEntry

object CurveTickDuration extends PlayEnum[CurveTickDuration] {

  val values = findValues

  case object Day extends CurveTickDuration
  case object Month extends CurveTickDuration
}
