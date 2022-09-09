package models.auth

import models.User
import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class UserSession(token: String, user: User)

object UserSession {
  implicit val UserSessionFormat: OFormat[UserSession] = Json.format[UserSession]
}
