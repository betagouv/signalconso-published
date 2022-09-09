package models.auth

import play.api.libs.json.Json

case class UserCredentials(
    login: String,
    password: String
)

object UserCredentials {
  implicit val userLoginFormat = Json.format[UserCredentials]
}
