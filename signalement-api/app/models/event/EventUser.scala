package models.event

import models.UserRole
import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class EventUser(firstName: String, lastName: String, role: UserRole)

object EventUser {
  implicit val EventUserFormat: OFormat[EventUser] = Json.format[EventUser]
}
