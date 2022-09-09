package repositories.accesstoken

import models._
import models.token.TokenKind
import repositories.DatabaseTable
import repositories.companyaccess.CompanyAccessColumnType._
import repositories.accesstoken.AccessTokenColumnType._
import repositories.PostgresProfile.api._
import utils._

import java.time._
import java.util.UUID

class AccessTokenTable(tag: Tag) extends DatabaseTable[AccessToken](tag, "access_tokens") {
  def creationDate = column[OffsetDateTime]("creation_date")
  def kind = column[TokenKind]("kind")
  def token = column[String]("token")
  def valid = column[Boolean]("valid")
  def companyId = column[Option[UUID]]("company_id")
  def level = column[Option[AccessLevel]]("level")
  def emailedTo = column[Option[EmailAddress]]("emailed_to")
  def expirationDate = column[Option[OffsetDateTime]]("expiration_date")
  def * = (
    id,
    creationDate,
    kind,
    token,
    valid,
    companyId,
    level,
    emailedTo,
    expirationDate
  ) <> ((AccessToken.apply _).tupled, AccessToken.unapply)
}

object AccessTokenTable {
  val table = TableQuery[AccessTokenTable]

}
