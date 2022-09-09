package models.investigation

import enumeratum._

sealed trait Practice extends EnumEntry

object Practice extends PlayEnum[Practice] {
  val values = findValues

  case object DropShipping extends Practice
}
