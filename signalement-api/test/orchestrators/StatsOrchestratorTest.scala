package orchestrators

import models.CountByDate
import orchestrators.StatsOrchestrator.formatStatData
import org.specs2.mutable.Specification

import java.sql.Timestamp
import java.time.LocalDate

class StatsOrchestratorTest extends Specification {

  "StatsOrchestratorTest" should {

    "handle missing data correctly when data are missing on both boundaries" in {

      val now = LocalDate.now().withDayOfMonth(1).atStartOfDay()

      val data = Vector(
        (Timestamp.valueOf(now.minusMonths(3L)), 1234),
        (Timestamp.valueOf(now.minusMonths(2L)), 1234)
      )

      val tick = 5
      val expected = Seq(
        CountByDate(0, now.minusMonths(4L).toLocalDate),
        CountByDate(1234, now.minusMonths(3L).toLocalDate),
        CountByDate(1234, now.minusMonths(2L).toLocalDate),
        CountByDate(0, now.minusMonths(1L).toLocalDate),
        CountByDate(0, now.minusMonths(0L).toLocalDate)
      )

      formatStatData(data, tick) shouldEqual expected

    }

    "handle missing data correctly when data are missing on lower boundary" in {

      val now = LocalDate.now().withDayOfMonth(1).atStartOfDay()

      val data = Vector(
        (Timestamp.valueOf(now.minusMonths(3L)), 1234),
        (Timestamp.valueOf(now.minusMonths(2L)), 1234)
      )

      val tick = 4
      val expected = Seq(
        CountByDate(1234, now.minusMonths(3L).toLocalDate),
        CountByDate(1234, now.minusMonths(2L).toLocalDate),
        CountByDate(0, now.minusMonths(1L).toLocalDate),
        CountByDate(0, now.minusMonths(0L).toLocalDate)
      )

      formatStatData(data, tick) shouldEqual expected

    }

    "handle missing data correctly when data are missing on upper boundary" in {

      val now = LocalDate.now().withDayOfMonth(1).atStartOfDay()

      val data = Vector(
        (Timestamp.valueOf(now.minusMonths(1L)), 1234),
        (Timestamp.valueOf(now.minusMonths(0L)), 1234)
      )

      val tick = 3
      val expected = Seq(
        CountByDate(0, now.minusMonths(2L).toLocalDate),
        CountByDate(1234, now.minusMonths(1L).toLocalDate),
        CountByDate(1234, now.minusMonths(0L).toLocalDate)
      )

      formatStatData(data, tick) shouldEqual expected

    }

    "handle missing data correctly when data are available for all ticks" in {

      val now = LocalDate.now().withDayOfMonth(1).atStartOfDay()

      val data = Vector(
        (Timestamp.valueOf(now.minusMonths(1L)), 1234),
        (Timestamp.valueOf(now.minusMonths(0L)), 1234)
      )

      val tick = 2
      val expected = Seq(
        CountByDate(1234, now.minusMonths(1L).toLocalDate),
        CountByDate(1234, now.minusMonths(0L).toLocalDate)
      )

      formatStatData(data, tick) shouldEqual expected

    }

    "handle missing data correctly when no data are returned" in {

      val now = LocalDate.now().withDayOfMonth(1).atStartOfDay()

      val data = Vector(
      )

      val tick = 3
      val expected = Seq(
        CountByDate(0, now.minusMonths(2L).toLocalDate),
        CountByDate(0, now.minusMonths(1L).toLocalDate),
        CountByDate(0, now.minusMonths(0L).toLocalDate)
      )

      formatStatData(data, tick) shouldEqual expected

    }

  }

}
