import models.PaginatedResult

import repositories.PostgresProfile.api._
import slick.jdbc.JdbcBackend

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

package object repositories {

  implicit class PaginateOps[A, B](query: slick.lifted.Query[A, B, Seq])(implicit executionContext: ExecutionContext) {

    def withPagination(
        db: JdbcBackend#DatabaseDef
    )(maybeOffset: Option[Long], maybeLimit: Option[Int]): Future[PaginatedResult[B]] = {

      val offset = maybeOffset.map(Math.max(_, 0)).getOrElse(0L)
      val limit = maybeLimit.map(Math.max(_, 0))

      val queryWithOffset = query.drop(offset)
      val queryWithOffsetAndLimit = limit.map(l => queryWithOffset.take(l)).getOrElse(queryWithOffset)

      val resultF: Future[Seq[B]] = db.run(queryWithOffsetAndLimit.result)
      val countF: Future[Int] = db.run(query.length.result)
      for {
        result <- resultF
        count <- countF
      } yield PaginatedResult(
        totalCount = count,
        entities = result.toList,
        hasNextPage = limit.exists(l => count - (offset + l) > 0)
      )
    }

  }

  def computeTickValues(ticks: Int) = Seq
    .iterate(OffsetDateTime.now().minusMonths(ticks.toLong - 1L).withDayOfMonth(1), ticks)(_.plusMonths(1))
    .map(_.toLocalDate)
    .map(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(_))
    .map(t => s"('$t'::timestamp)")
    .mkString(",")

}
