package models.website

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class WebsiteHostCount(host: String, count: Int)

object WebsiteHostCount {
  implicit val WebsiteHostCountFormat: OFormat[WebsiteHostCount] = Json.format[WebsiteHostCount]
}
