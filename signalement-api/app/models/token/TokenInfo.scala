package models.token

import play.api.libs.json.Json
import utils.EmailAddress
import utils.SIRET

sealed trait TokenInfo {
  val token: String
  val emailedTo: EmailAddress
  val kind: TokenKind
}
final case class DGCCRFUserActivationToken(
    token: String,
    kind: TokenKind,
    emailedTo: EmailAddress
) extends TokenInfo

final case class CompanyUserActivationToken(
    token: String,
    kind: TokenKind,
    companySiret: SIRET,
    emailedTo: EmailAddress
) extends TokenInfo

object TokenInfo {

  implicit val DGCCRFUserActivationTokenWrites = Json.writes[DGCCRFUserActivationToken]
  implicit val CompanyUserActivationTokenWrites = Json.writes[CompanyUserActivationToken]
  implicit val TokenInfoWrites = Json.writes[TokenInfo]

}
