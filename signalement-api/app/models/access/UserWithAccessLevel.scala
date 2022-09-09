package models.access

import io.scalaland.chimney.dsl.TransformerOps
import models.AccessLevel
import models.User
import play.api.libs.json.Json
import play.api.libs.json.Writes
import utils.EmailAddress

import java.util.UUID

case class UserWithAccessLevel(
    userId: UUID,
    firstName: String,
    lastName: String,
    email: EmailAddress,
    level: String,
    editable: Boolean,
    isHeadOffice: Boolean
)

object UserWithAccessLevel {

  implicit val UserWithAccessLevelWrites: Writes[UserWithAccessLevel] = Json.writes[UserWithAccessLevel]

  def toApi(user: User, accessLevel: AccessLevel, editable: Boolean, isHeadOffice: Boolean): UserWithAccessLevel =
    user
      .into[UserWithAccessLevel]
      .withFieldConst(_.userId, user.id)
      .withFieldConst(_.editable, editable)
      .withFieldConst(_.level, accessLevel.value)
      .withFieldConst(_.isHeadOffice, isHeadOffice)
      .transform
}
