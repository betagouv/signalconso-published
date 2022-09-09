package models

import com.mohiva.play.silhouette.api.Identity
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.EmailAddress
import utils.EnumUtils

import java.time.OffsetDateTime
import java.util.UUID

case class DraftUser(
    email: EmailAddress,
    firstName: String,
    lastName: String,
    password: String
)

object DraftUser {
  implicit val draftUserFormat = Json.format[DraftUser]
}

case class User(
    id: UUID,
    password: String,
    email: EmailAddress,
    firstName: String,
    lastName: String,
    userRole: UserRole,
    lastEmailValidation: Option[OffsetDateTime]
) extends Identity {
  def fullName: String = s"${firstName} ${lastName}"
  def isAdmin: Boolean = this.userRole == UserRole.Admin
}

object User {
  implicit val userWrites = new Writes[User] {
    def writes(user: User) = Json.obj(
      "id" -> user.id,
      "email" -> user.email,
      "firstName" -> user.firstName,
      "lastName" -> user.lastName,
      "role" -> user.userRole.entryName,
      "permissions" -> user.userRole.permissions,
      "lastEmailValidation" -> user.lastEmailValidation
    )
  }

  implicit val userReads: Reads[User] = (
    (JsPath \ "id").read[UUID] and
      (JsPath \ "password").read[String] and
      (JsPath \ "email").read[EmailAddress] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      ((JsPath \ "role").read[String]).map(UserRole.withName) and
      (JsPath \ "lastEmailValidation").readNullable[OffsetDateTime]
  )(User.apply _)
}

object UserPermission extends Enumeration {
  val listReports, updateReport, deleteReport, deleteFile, createReportAction, activateAccount, updateCompany,
      editDocuments, subscribeReports, inviteDGCCRF = Value

  implicit val enumReads: Reads[UserPermission.Value] = EnumUtils.enumReads(UserPermission)

  implicit def enumWrites: Writes[UserPermission.Value] = EnumUtils.enumWrites
}

case class UserUpdate(
    firstName: Option[String],
    lastName: Option[String]
) {
  def mergeToUser(user: User) =
    user.copy(
      firstName = firstName.getOrElse(user.firstName),
      lastName = lastName.getOrElse(user.lastName)
    )
}

object UserUpdate {
  implicit val userUpdateFormat: OFormat[UserUpdate] = Json.format[UserUpdate]

}
