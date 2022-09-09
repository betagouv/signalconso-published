package models.token

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.EnumEntry
import enumeratum.PlayEnum

sealed trait TokenKind extends EnumEntry with UpperSnakecase

object TokenKind extends PlayEnum[TokenKind] {

  val values = findValues

  case object CompanyInit extends TokenKind
  case object CompanyJoin extends TokenKind
  case object DGCCRFAccount extends TokenKind
  case object ValidateEmail extends TokenKind

}
