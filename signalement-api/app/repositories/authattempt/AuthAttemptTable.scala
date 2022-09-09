package repositories.authattempt

import models.auth.AuthAttempt
import repositories.DatabaseTable
import repositories.PostgresProfile.api._

import java.time.OffsetDateTime

class AuthAttemptTable(tag: Tag) extends DatabaseTable[AuthAttempt](tag, "auth_attempts") {

  def login = column[String]("login")
  def timestamp = column[OffsetDateTime]("timestamp")
  def isSuccess = column[Option[Boolean]]("is_success")
  def failureCause = column[Option[String]]("failure_cause")

  def * = (id, login, timestamp, isSuccess, failureCause) <> ((AuthAttempt.apply _).tupled, AuthAttempt.unapply)
}

object AuthAttemptTable {
  val table = TableQuery[AuthAttemptTable]
}
