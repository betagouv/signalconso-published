package models

import utils.QueryStringMapper

import scala.util.Try

case class PaginatedSearch(
    offset: Option[Long] = Some(0),
    limit: Option[Int] = None
)

object PaginatedSearch {
  def fromQueryString(queryString: Map[String, Seq[String]]): Try[PaginatedSearch] = Try {
    val mapper = new QueryStringMapper(queryString)
    PaginatedSearch(offset = mapper.long("offset"), limit = mapper.int("limit"))
  }
}
