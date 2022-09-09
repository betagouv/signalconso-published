package models.access

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import utils.EmailAddress

case class ActivationLinkRequest(token: String, email: EmailAddress)

object ActivationLinkRequest {
  implicit val ActivationLinkRequestFormat: OFormat[ActivationLinkRequest] = Json.format[ActivationLinkRequest]
}
