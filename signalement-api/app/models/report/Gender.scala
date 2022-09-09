package models.report

import enumeratum._

sealed trait Gender extends EnumEntry

object Gender extends PlayEnum[Gender] {
  val values = findValues

  case object Male extends Gender
  case object Female extends Gender
}
