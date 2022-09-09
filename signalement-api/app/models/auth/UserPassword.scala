package models.auth

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class UserPassword(
    password: String
)
object UserPassword {
  implicit val UserPasswordFormat: OFormat[UserPassword] = Json.format[UserPassword]
}
