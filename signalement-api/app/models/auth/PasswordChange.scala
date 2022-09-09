package models.auth

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class PasswordChange(
    newPassword: String,
    oldPassword: String
)

object PasswordChange {
  implicit val UserLoginFormat: OFormat[PasswordChange] = Json.format[PasswordChange]
}
