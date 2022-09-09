package utils

import models.extractUUID

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class QueryStringMapper(q: Map[String, Seq[String]]) {

  def long(k: String): Option[Long] = string(k).map(_.toLong)

  def int(k: String): Option[Int] = string(k).map(_.toInt)

  def string(k: String): Option[String] = q.get(k).flatMap(_.headOption)

  def UUID(k: String): Option[UUID] = q.get(k).flatMap(_.headOption).map(extractUUID)

  def seq(k: String): Seq[String] = q.getOrElse(k, Nil)

  def localDate(k: String): Option[LocalDate] = DateUtils.parseDate(string(k))

  def time(k: String): Option[OffsetDateTime] = DateUtils.parseTime(string(k))

  private def timeWithLocalDateRetrocompat(k: String, assumedLocalTime: LocalTime): Option[OffsetDateTime] =
    time(k).orElse(localDate(k).map(ld => OffsetDateTime.of(ld, assumedLocalTime, ZoneOffset.UTC)))

  // Allows the legacy format YYYY-MM-DD, assuming it means YYYY-MM-DDT00:00:00.000Z
  def timeWithLocalDateRetrocompatStartOfDay(k: String) =
    timeWithLocalDateRetrocompat(k, LocalTime.MIN)

  // Allows the legacy format YYYY-MM-DD, assuming it means YYYY-MM-DDT23:59:59.999Z
  def timeWithLocalDateRetrocompatEndOfDay(k: String) =
    timeWithLocalDateRetrocompat(k, LocalTime.MAX)

  def boolean(k: String): Option[Boolean] = string(k) match {
    case Some("true")  => Some(true)
    case Some("false") => Some(false)
    case _             => None
  }

  def timeZone(k: String): Option[ZoneId] =
    string(k).map(ZoneId.of)

}
