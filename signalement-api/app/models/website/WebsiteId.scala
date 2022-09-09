package models.website

import play.api.libs.json.Format
import play.api.libs.json.Json

import java.util.UUID

case class WebsiteId(value: UUID) extends AnyVal

object WebsiteId {
  implicit val WebsiteIdFormat: Format[WebsiteId] = Json.valueFormat[WebsiteId]

  def generateId() = new WebsiteId(UUID.randomUUID())

}
