package models

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import utils.EmailAddress

import java.time.OffsetDateTime

case class DGCCRFUser(
    email: EmailAddress,
    firstName: String,
    lastName: String,
    lastEmailValidation: Option[OffsetDateTime]
)

object DGCCRFUser {
  implicit val DGCCRFUserFormat: OFormat[DGCCRFUser] = Json.format[DGCCRFUser]
}
