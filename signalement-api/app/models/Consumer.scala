package models

import java.time.OffsetDateTime
import java.util.UUID

case class Consumer(
    id: UUID = UUID.randomUUID(),
    name: String,
    creationDate: OffsetDateTime = OffsetDateTime.now(),
    apiKey: String,
    deleteDate: Option[OffsetDateTime] = None
)
