package models

import enumeratum.EnumEntry
import enumeratum.PlayEnum
import models.report.ReportStatus.statusReadByPro
import models.report.ReportStatus.statusWithProResponse
import models.report.ReportFilter
import models.report.ReportFilter.allReportsFilter
import models.report.ReportFilter.transmittedReportsFilter
import models.report.ReportStatus

import java.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class StatsValue(value: Option[Int])

object StatsValue {
  implicit val format: OFormat[StatsValue] = Json.format[StatsValue]
}

case class CountByDate(
    count: Int,
    date: LocalDate
)

object CountByDate {
  implicit val format: OFormat[CountByDate] = Json.format[CountByDate]
}

case class ReportReviewStats(
    positive: Int = 0,
    neutral: Int = 0,
    negative: Int = 0
)

object ReportReviewStats {
  implicit val format: OFormat[ReportReviewStats] = Json.format[ReportReviewStats]
}

sealed abstract class PublicStat(val filter: ReportFilter, val percentageBaseFilter: Option[ReportFilter] = None)
    extends EnumEntry

object PublicStat extends PlayEnum[PublicStat] {
  lazy val values = findValues
  case object PromesseAction extends PublicStat(ReportFilter(status = Seq(ReportStatus.PromesseAction)))
  case object Reports extends PublicStat(allReportsFilter)
  case object TransmittedPercentage
      extends PublicStat(
        transmittedReportsFilter,
        Some(allReportsFilter)
      )
  case object ReadPercentage
      extends PublicStat(
        ReportFilter(status = statusReadByPro),
        Some(transmittedReportsFilter)
      )
  case object ResponsePercentage
      extends PublicStat(
        ReportFilter(status = statusWithProResponse),
        Some(ReadPercentage.filter)
      )
  case object WebsitePercentage
      extends PublicStat(
        ReportFilter(hasWebsite = Some(true)),
        Some(allReportsFilter)
      )
}
