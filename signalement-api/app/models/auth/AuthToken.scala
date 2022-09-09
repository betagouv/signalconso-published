package models.auth

import java.time.OffsetDateTime
import java.util.UUID

case class AuthToken(
    id: UUID,
    userID: UUID,
    expiry: OffsetDateTime
)
