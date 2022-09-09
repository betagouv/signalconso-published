package models.auth

import java.time.OffsetDateTime
import java.util.UUID

case class AuthAttempt(
    id: UUID,
    login: String,
    timestamp: OffsetDateTime,
    isSuccess: Option[Boolean],
    failureCause: Option[String] = None
)
object AuthAttempt {
  def build(login: String, isSuccess: Boolean, failureCause: Option[String] = None) =
    AuthAttempt(UUID.randomUUID, login, OffsetDateTime.now, Some(isSuccess), failureCause)
}
