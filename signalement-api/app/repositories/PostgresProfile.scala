package repositories

import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.agg.PgAggFuncSupport
import com.github.tminglei.slickpg.trgm.PgTrgmSupport
import models.report.ReportTag

import java.time.OffsetDateTime

trait PostgresProfile
    extends ExPostgresProfile
    with PgPlayJsonSupport
    with PgArraySupport
    with PgSearchSupport
    with PgDate2Support
    with PgAggFuncSupport
    with PgTrgmSupport {

  def pgjson = "jsonb"

  override val api = MyAPI

  object MyAPI
      extends API
      with ArrayImplicits
      with JsonImplicits
      with DateTimeImplicits
      with PgTrgmImplicits
      with SimpleSearchPlainImplicits {

    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)

    val SubstrSQLFunction = SimpleFunction.ternary[String, Int, Int, String]("substr")

    val DatePartSQLFunction = SimpleFunction.binary[String, OffsetDateTime, Int]("date_part")

    val ArrayToStringSQLFunction = SimpleFunction.ternary[List[String], String, String, String]("array_to_string")
    SimpleFunction.binary[List[ReportTag], Int, Int]("array_length")

    SimpleFunction.binary[Option[Double], Option[Double], Option[Double]]("least")

  }
  override protected def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate
}

object PostgresProfile extends PostgresProfile
