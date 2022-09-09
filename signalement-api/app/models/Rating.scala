package models

import play.api.libs.json.Json
import play.api.libs.json.OFormat

import java.time.OffsetDateTime
import java.util.UUID

case class Rating(
    id: Option[UUID],
    creationDate: Option[OffsetDateTime],
    category: String,
    subcategories: List[String],
    positive: Boolean
)

object Rating {

  implicit val ratingFormat: OFormat[Rating] = Json.format[Rating]

}
