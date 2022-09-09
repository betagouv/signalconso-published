package models.investigation

import models.website.Website
import models.website.WebsiteId
import play.api.libs.json.Json
import play.api.libs.json.OFormat

import java.time.OffsetDateTime
import java.time.ZoneOffset

case class WebsiteInvestigationApi(
    id: WebsiteId,
    practice: Option[Practice],
    investigationStatus: Option[InvestigationStatus],
    attribution: Option[DepartmentDivision],
    lastUpdated: Option[OffsetDateTime]
) {

  def copyToDomain(website: Website): Website =
    website.copy(
      practice = this.practice,
      investigationStatus = this.investigationStatus.getOrElse(InvestigationStatus.NotProcessed),
      attribution = this.attribution,
      lastUpdated = OffsetDateTime.now(ZoneOffset.UTC)
    )

}

object WebsiteInvestigationApi {

  implicit val WebsiteInvestigationAPIFormat: OFormat[WebsiteInvestigationApi] = Json.format[WebsiteInvestigationApi]

}
