package models

import models.token.TokenKind
import play.api.libs.json._
import utils.EmailAddress
import utils.SIRET

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

case class AccessToken(
    id: UUID = UUID.randomUUID(),
    creationDate: OffsetDateTime,
    kind: TokenKind,
    token: String,
    valid: Boolean,
    companyId: Option[UUID],
    companyLevel: Option[AccessLevel],
    emailedTo: Option[EmailAddress],
    expirationDate: Option[OffsetDateTime]
)

object AccessToken {

  def build(
      kind: TokenKind,
      token: String,
      validity: Option[java.time.temporal.TemporalAmount],
      companyId: Option[UUID],
      level: Option[AccessLevel],
      emailedTo: Option[EmailAddress] = None,
      creationDate: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
  ): AccessToken = AccessToken(
    creationDate = creationDate,
    kind = kind,
    token = token,
    valid = true,
    companyId = companyId,
    companyLevel = level,
    emailedTo = emailedTo,
    expirationDate = validity.map(OffsetDateTime.now(ZoneOffset.UTC).plus(_))
  )

  def resetExpirationDate(accessToken: AccessToken, validity: Option[java.time.temporal.TemporalAmount]) =
    accessToken.copy(expirationDate = validity.map(OffsetDateTime.now(ZoneOffset.UTC).plus(_)))

}

case class ActivationRequest(
    draftUser: DraftUser,
    token: String,
    companySiret: Option[SIRET]
)
object ActivationRequest {
  implicit val ActivationRequestFormat: OFormat[ActivationRequest] = Json.format[ActivationRequest]
}
