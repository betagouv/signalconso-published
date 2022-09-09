package repositories.emailvalidation

import models.EmailValidation
import repositories.DatabaseTable
import utils.EmailAddress
import repositories.PostgresProfile.api._

import java.time.OffsetDateTime

class EmailValidationTable(tag: Tag) extends DatabaseTable[EmailValidation](tag, "emails_validation") {
  def creationDate = column[OffsetDateTime]("creation_date")
  def confirmationCode = column[String]("confirmation_code")
  def email = column[EmailAddress]("email")
  def attempts = column[Int]("attempts")
  def lastAttempt = column[Option[OffsetDateTime]]("last_attempt")
  def lastValidationDate = column[Option[OffsetDateTime]]("last_validation_date")
  def * = (
    id,
    creationDate,
    confirmationCode,
    email,
    attempts,
    lastAttempt,
    lastValidationDate
  ) <> ((EmailValidation.apply _).tupled, EmailValidation.unapply)
}

object EmailValidationTable {
  val table = TableQuery[EmailValidationTable]
}
