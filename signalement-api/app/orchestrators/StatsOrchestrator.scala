package orchestrators

import cats.data.NonEmptyList
import models.CountByDate
import models.CurveTickDuration
import models.ReportReviewStats
import models.UserRole
import models.report.ReportFilter
import models.report.ReportStatus
import models.report.ReportTag
import models.report.review.ResponseEvaluation
import orchestrators.StatsOrchestrator.computeStartingDate
import orchestrators.StatsOrchestrator.formatStatData
import orchestrators.StatsOrchestrator.restrictToReliableDates
import orchestrators.StatsOrchestrator.toPercentage
import repositories.accesstoken.AccessTokenRepositoryInterface
import repositories.event.EventRepositoryInterface
import repositories.report.ReportRepositoryInterface
import repositories.reportconsumerreview.ResponseConsumerReviewRepositoryInterface
import utils.Constants.ActionEvent._
import utils.Constants.ActionEvent
import utils.Constants.Departments

import java.sql.Timestamp
import java.time._
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class StatsOrchestrator(
    reportRepository: ReportRepositoryInterface,
    eventRepository: EventRepositoryInterface,
    reportConsumerReviewRepository: ResponseConsumerReviewRepositoryInterface,
    accessTokenRepository: AccessTokenRepositoryInterface
)(implicit val executionContext: ExecutionContext) {

  def countByDepartments(start: Option[LocalDate], end: Option[LocalDate]): Future[Seq[(String, Int)]] =
    for {
      postalCodeReportCountTuple <- reportRepository.countByDepartments(start, end)
      departmentsReportCountMap = formatCountByDepartments(postalCodeReportCountTuple)
    } yield departmentsReportCountMap

  private[orchestrators] def formatCountByDepartments(
      postalCodeReportCountTuple: Seq[(String, Int)]
  ): Seq[(String, Int)] = {
    val departmentsReportCountTuple: Seq[(String, Int)] =
      postalCodeReportCountTuple.map { case (partialPostalCode, count) =>
        (Departments.fromPostalCode(partialPostalCode).getOrElse(""), count)
      }
    departmentsReportCountTuple
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2).sum)
      .toSeq
      .sortWith(_._2 > _._2)
  }

  def getReportCount(reportFilter: ReportFilter): Future[Int] =
    reportRepository.count(reportFilter)

  def getReportCountPercentage(filter: ReportFilter, basePercentageFilter: ReportFilter): Future[Int] =
    for {
      count <- reportRepository.count(filter)
      baseCount <- reportRepository.count(basePercentageFilter)
    } yield toPercentage(count, baseCount)

  def getReportCountPercentageWithinReliableDates(
      filter: ReportFilter,
      basePercentageFilter: ReportFilter
  ): Future[Int] =
    getReportCountPercentage(
      restrictToReliableDates(filter),
      restrictToReliableDates(basePercentageFilter)
    )

  def getReportsCountCurve(
      reportFilter: ReportFilter,
      ticks: Int = 12,
      tickDuration: CurveTickDuration = CurveTickDuration.Month
  ): Future[Seq[CountByDate]] =
    tickDuration match {
      case CurveTickDuration.Month => reportRepository.getMonthlyCount(reportFilter, ticks)
      case CurveTickDuration.Day   => reportRepository.getDailyCount(reportFilter, ticks)
    }

  def getReportsCountPercentageCurve(
      reportFilter: ReportFilter,
      baseFilter: ReportFilter
  ): Future[Seq[CountByDate]] =
    for {
      rawCurve <- getReportsCountCurve(reportFilter)
      baseCurve <- getReportsCountCurve(baseFilter)
    } yield rawCurve.sortBy(_.date).zip(baseCurve.sortBy(_.date)).map { case (a, b) =>
      CountByDate(
        count = toPercentage(a.count, b.count),
        date = a.date
      )
    }

  def getReportsTagsDistribution(companyId: Option[UUID], userRole: UserRole): Future[Map[ReportTag, Int]] =
    reportRepository.getReportsTagsDistribution(companyId, userRole)

  def getReportsStatusDistribution(companyId: Option[UUID], userRole: UserRole): Future[Map[String, Int]] =
    reportRepository.getReportsStatusDistribution(companyId, userRole)

  def getReportResponseReview(id: Option[UUID]): Future[ReportReviewStats] =
    reportConsumerReviewRepository.findByCompany(id).map { events =>
      events.foldLeft(ReportReviewStats()) { case (acc, event) =>
        ReportReviewStats(
          positive = acc.positive + (if (event.evaluation == ResponseEvaluation.Positive) 1 else 0),
          neutral = acc.neutral + (if (event.evaluation == ResponseEvaluation.Neutral) 1 else 0),
          negative = acc.negative + (if (event.evaluation == ResponseEvaluation.Negative) 1 else 0)
        )
      }
    }

  def getReadAvgDelay(companyId: Option[UUID] = None) =
    eventRepository.getAvgTimeUntilEvent(ActionEvent.REPORT_READING_BY_PRO, companyId)

  def getResponseAvgDelay(companyId: Option[UUID] = None, userRole: UserRole): Future[Option[Duration]] = {
    val (statusFilter, tagFilterNot) = userRole match {
      case UserRole.Admin | UserRole.DGCCRF => (Seq.empty[ReportStatus], Seq.empty[ReportTag])
      case UserRole.Professionnel => (ReportStatus.statusVisibleByPro, ReportTag.ReportTagHiddenToProfessionnel)
    }
    eventRepository.getAvgTimeUntilEvent(
      action = ActionEvent.REPORT_PRO_RESPONSE,
      companyId = companyId,
      status = statusFilter,
      withoutTags = tagFilterNot
    )
  }

  def getProReportTransmittedStat(ticks: Int) =
    eventRepository
      .getProReportStat(
        ticks,
        computeStartingDate(ticks),
        NonEmptyList.of(
          REPORT_READING_BY_PRO,
          REPORT_CLOSED_BY_NO_READING,
          REPORT_CLOSED_BY_NO_ACTION,
          EMAIL_PRO_NEW_REPORT,
          REPORT_PRO_RESPONSE
        )
      )
      .map(formatStatData(_, ticks))

  def dgccrfAccountsCurve(ticks: Int) =
    accessTokenRepository
      .dgccrfAccountsCurve(ticks)
      .map(formatStatData(_, ticks))

  def dgccrfSubscription(ticks: Int) =
    accessTokenRepository
      .dgccrfSubscription(ticks)
      .map(formatStatData(_, ticks))

  def dgccrfActiveAccountsCurve(ticks: Int) =
    accessTokenRepository
      .dgccrfActiveAccountsCurve(ticks)
      .map(formatStatData(_, ticks))

  def dgccrfControlsCurve(ticks: Int) =
    accessTokenRepository
      .dgccrfControlsCurve(ticks)
      .map(formatStatData(_, ticks))
}

object StatsOrchestrator {

  private[orchestrators] val reliableStatsStartDate = OffsetDateTime.parse("2019-01-01T00:00:00Z")

  private[orchestrators] def restrictToReliableDates(reportFilter: ReportFilter): ReportFilter =
    // Percentages would be messed up if we look at really old data or really fresh one
    reportFilter.copy(start = Some(reliableStatsStartDate), end = Some(OffsetDateTime.now.minusDays(30)))

  private[orchestrators] def toPercentage(numerator: Int, denominator: Int): Int =
    if (denominator == 0) 0
    else Math.max(0, Math.min(100, numerator * 100 / denominator))

  private[orchestrators] def computeStartingDate(ticks: Int): OffsetDateTime =
    OffsetDateTime.now(ZoneOffset.UTC).minusMonths(ticks.toLong - 1L).withDayOfMonth(1)

  /** Fill data with default value when there missing data in database
    */
  private[orchestrators] def formatStatData(data: Vector[(Timestamp, Int)], ticks: Int): Seq[CountByDate] = {

    val countByDateList = data.map { case (date, count) => CountByDate(count, date.toLocalDateTime.toLocalDate) }

    if (ticks - data.length > 0) {
      val upperBound = LocalDate.now().withDayOfMonth(1)
      val lowerBound = upperBound.minusMonths(ticks.toLong - 1L)

      val minAvailableDatabaseDataDate = countByDateList.map(_.date).minOption.getOrElse(lowerBound)
      val maxAvailableDatabaseDataDate = countByDateList.map(_.date).maxOption.getOrElse(upperBound)

      val missingMonthsLowerBound = Period.between(lowerBound, minAvailableDatabaseDataDate)
      val missingMonthsUpperBound = Period.between(maxAvailableDatabaseDataDate, upperBound)

      if (missingMonthsLowerBound.getMonths == 0 && missingMonthsUpperBound.getMonths == 0) {
        // No data , filling the data with default value
        Seq
          .iterate(lowerBound, ticks)(_.plusMonths(1))
          .map(CountByDate(0, _))
      } else {
        // Missing data , filling the data with default value
        val missingLowerBoundData =
          Seq.iterate(lowerBound, missingMonthsLowerBound.getMonths)(_.minusMonths(1L)).map(CountByDate(0, _))

        val missingUpperBoundData =
          Seq
            .iterate(maxAvailableDatabaseDataDate.plusMonths(1), missingMonthsUpperBound.getMonths)(_.plusMonths(1))
            .map(CountByDate(0, _))

        missingLowerBoundData ++ countByDateList ++ missingUpperBoundData
      }
    } else countByDateList

  }

}
