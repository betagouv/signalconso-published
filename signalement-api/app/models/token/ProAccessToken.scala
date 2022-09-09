package models.token

import models.AccessLevel
import models.UserRole
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import utils.EmailAddress

import java.time.OffsetDateTime
import java.util.UUID

case class ProAccessToken private (
    id: UUID,
    level: Option[AccessLevel],
    emailedTo: Option[EmailAddress],
    expirationDate: Option[OffsetDateTime],
    token: Option[String]
)

object ProAccessToken {
  def apply(
      id: UUID,
      level: Option[AccessLevel],
      emailedTo: Option[EmailAddress],
      expirationDate: Option[OffsetDateTime],
      token: String,
      userRole: UserRole
  ): ProAccessToken =
    userRole match {
      case UserRole.Admin => new ProAccessToken(id, level, emailedTo, expirationDate, Some(token))
      case _              => new ProAccessToken(id, level, emailedTo, expirationDate, token = None)
    }

  implicit val ProAccessTokenFormat: OFormat[ProAccessToken] = Json.format[ProAccessToken]
}
