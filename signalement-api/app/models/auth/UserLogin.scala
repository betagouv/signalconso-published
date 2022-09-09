package models.auth

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class UserLogin(
    login: String
)

object UserLogin {
  implicit val UserLoginFormat: OFormat[UserLogin] = Json.format[UserLogin]
}
