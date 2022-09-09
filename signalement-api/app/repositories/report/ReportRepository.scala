package repositories.report

import com.github.tminglei.slickpg.TsVector
import models._
import models.report._
import repositories.PostgresProfile.api._
import repositories.report.ReportColumnType._
import repositories.reportfile.ReportFileTable
import repositories.report.ReportRepository.ReportFileOrdering
import slick.jdbc.JdbcProfile
import utils.Constants.Departments.toPostalCode
import utils._

import java.time._
import java.util.UUID
import scala.collection.SortedMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import repositories.report.ReportRepository.queryFilter
import repositories.CRUDRepository
import slick.basic.DatabaseConfig

class ReportRepository(override val dbConfig: DatabaseConfig[JdbcProfile])(implicit
    override val ec: ExecutionContext
) extends CRUDRepository[ReportTable, Report]
    with ReportRepositoryInterface {

  override val table: TableQuery[ReportTable] = ReportTable.table
  import dbConfig._

  def findSimilarReportList(report: ReportDraft, after: OffsetDateTime): Future[List[Report]] = {

    val similarReportQuery = table
      .filter(_.email === report.email)
      .filterOpt(report.companyAddress.flatMap(_.postalCode))(_.companyPostalCode === _)
      .filterIf(report.companyAddress.flatMap(_.postalCode).isEmpty)(_.companyPostalCode.isEmpty)
      .filterOpt(report.companyAddress.flatMap(_.number))(_.companyStreetNumber === _)
      .filterIf(report.companyAddress.flatMap(_.number).isEmpty)(_.companyStreetNumber.isEmpty)
      .filterOpt(report.companyAddress.flatMap(_.street))(_.companyStreet === _)
      .filterIf(report.companyAddress.flatMap(_.street).isEmpty)(_.companyStreet.isEmpty)
      .filterOpt(report.companyAddress.flatMap(_.addressSupplement))(_.companyAddressSupplement === _)
      .filterIf(report.companyAddress.flatMap(_.addressSupplement).isEmpty)(_.companyAddressSupplement.isEmpty)
      .filterOpt(report.companyAddress.flatMap(_.city))(_.companyCity === _)
      .filterIf(report.companyAddress.flatMap(_.city).isEmpty)(_.companyCity.isEmpty)
      .filter(_.firstName === report.firstName)
      .filter(_.lastName === report.lastName)
      .filter(_.creationDate >= after)

    db.run(similarReportQuery.result).map(_.toList)
  }

  def findByEmail(email: EmailAddress): Future[Seq[Report]] =
    db.run(table.filter(_.email === email).result)

  def countByDepartments(start: Option[LocalDate], end: Option[LocalDate]): Future[Seq[(String, Int)]] =
    db.run(
      table
        .filterOpt(start) { case (table, s) =>
          table.creationDate >= ZonedDateTime.of(s, LocalTime.MIN, ZoneOffset.UTC.normalized()).toOffsetDateTime
        }
        .filterOpt(end) { case (table, e) =>
          table.creationDate < ZonedDateTime.of(e, LocalTime.MAX, ZoneOffset.UTC.normalized()).toOffsetDateTime
        }
        .groupBy(_.companyPostalCode.getOrElse(""))
        .map { case (department, group) => (department, group.length) }
        .result
    )

  def count(filter: ReportFilter): Future[Int] = db.run(queryFilter(filter).length.result)

  def getMonthlyCount(filter: ReportFilter, ticks: Int = 7): Future[Seq[CountByDate]] =
    db
      .run(
        queryFilter(filter)
          .filter(report =>
            report.creationDate > OffsetDateTime.now(ZoneOffset.UTC).minusMonths(ticks.toLong).withDayOfMonth(1)
          )
          .groupBy(report =>
            (DatePartSQLFunction("month", report.creationDate), DatePartSQLFunction("year", report.creationDate))
          )
          .map { case ((month, year), group) => (month, year, group.length) }
          .result
      )
      .map(_.map { case (month, year, length) => CountByDate(length, LocalDate.of(year, month, 1)) })
      .map(fillFullPeriod(ticks, (x, i) => x.minusMonths(i.toLong).withDayOfMonth(1)))

  def getDailyCount(
      filter: ReportFilter,
      ticks: Int
  ): Future[Seq[CountByDate]] = db
    .run(
      queryFilter(filter)
        .filter(report => report.creationDate > OffsetDateTime.now(ZoneOffset.UTC).minusDays(11))
        .groupBy(report =>
          (
            DatePartSQLFunction("day", report.creationDate),
            DatePartSQLFunction("month", report.creationDate),
            DatePartSQLFunction("year", report.creationDate)
          )
        )
        .map { case ((day, month, year), group) =>
          (day, month, year, group.length)
        }
        .result
    )
    .map(_.map { case (day, month, year, length) => CountByDate(length, LocalDate.of(year, month, day)) })
    .map(fillFullPeriod(ticks, (x, i) => x.minusDays(i.toLong)))

  private[this] def fillFullPeriod(
      ticks: Int,
      dateOperator: (LocalDate, Int) => LocalDate
  )(
      fetchedData: Seq[CountByDate]
  ): Seq[CountByDate] = {
    val start = dateOperator(LocalDate.now(), ticks).atStartOfDay().toLocalDate
    val res = (1 to ticks).map { i =>
      val date = dateOperator(start, -i)
      val count = fetchedData
        .find(_.date.equals(date))
        .map(_.count)
        .getOrElse(0)
      CountByDate(count, date)
    }
    res
  }

  def getReports(companyId: UUID): Future[List[Report]] = db.run {
    table
      .filter(_.companyId === companyId)
      .to[List]
      .result
  }

  def getWithWebsites(): Future[List[Report]] = db.run {
    table
      .filter(_.websiteURL.isDefined)
      .to[List]
      .result
  }

  def getWithPhones(): Future[List[Report]] = db.run {
    table
      .filter(_.phone.isDefined)
      .to[List]
      .result
  }

  def getReportsStatusDistribution(companyId: Option[UUID], userRole: UserRole): Future[Map[String, Int]] =
    db.run(
      ReportTable
        .table(userRole)
        .filterOpt(companyId)(_.companyId === _)
        .groupBy(_.status)
        .map { case (status, report) => status -> report.size }
        .result
    ).map(_.toMap)

  def getReportsTagsDistribution(companyId: Option[UUID], userRole: UserRole): Future[Map[ReportTag, Int]] = {
    def spreadListOfTags(map: Seq[(List[ReportTag], Int)]): Map[ReportTag, Int] =
      map.foldLeft(Map.empty[ReportTag, Int]) { case (acc, (tags, count)) =>
        acc ++ Map(tags.map(tag => tag -> (count + acc.getOrElse(tag, 0))): _*)
      }

    db.run(
      ReportTable
        .table(userRole)
        .filterOpt(companyId)(_.companyId === _)
        .groupBy(_.tags)
        .map { case (status, report) => (status, report.size) }
        .sortBy(_._2.desc)
        .result
    ).map(spreadListOfTags)
  }

  def getHostsByCompany(companyId: UUID): Future[Seq[String]] =
    db.run(
      table
        .filter(_.companyId === companyId)
        .filter(_.host.isDefined)
        .map(_.host)
        .distinct
        .result
    ).map(_.map(_.getOrElse("")))

  def getReportsWithFiles(
      filter: ReportFilter
  ): Future[SortedMap[Report, List[ReportFile]]] =
    for {
      queryResult <- queryFilter(filter)
        .joinLeft(ReportFileTable.table)
        .on(_.id === _.reportId)
        .sortBy(_._1.creationDate.desc)
        .withPagination(db)(maybeOffset = Some(0), maybeLimit = Some(50000))
      filesGroupedByReports =
        SortedMap(
          queryResult.entities
            .groupBy(a => a._1)
            .view
            .mapValues(_.flatMap(_._2))
            .toSeq: _*
        )(ReportFileOrdering)

    } yield filesGroupedByReports

  def getReports(
      filter: ReportFilter,
      offset: Option[Long] = None,
      limit: Option[Int] = None
  ): Future[PaginatedResult[Report]] = for {
    res <- queryFilter(filter)
      .sortBy(_.creationDate.desc)
      .withPagination(db)(offset, limit)
  } yield res

  def getReportsByIds(ids: List[UUID]): Future[List[Report]] = db.run(
    table
      .filter(_.id inSet ids)
      .to[List]
      .result
  )

  def getByStatus(status: ReportStatus): Future[List[Report]] =
    db.run(table.filter(_.status === status.entryName).to[List].result)

  def getPendingReports(companiesIds: List[UUID]): Future[List[Report]] = db
    .run(
      table
        .filter(_.status === ReportStatus.TraitementEnCours.entryName)
        .filter(_.companyId inSet companiesIds)
        .to[List]
        .result
    )

  override def cloudWord(companyId: UUID): Future[List[ReportWordOccurrence]] =
    db.run(
      sql"""
        SELECT to_tsvector('french', STRING_AGG(replace(reportDetail.detailField,'Description : ',''), ''))
        FROM (
            SELECT unnest(details) as detailField
            FROM reports
            WHERE company_id = '#${companyId.toString}') as reportDetail
        WHERE reportDetail.detailField like 'Description%';
        """.as[TsVector]
    ).map { c =>
      val tsVector = c.headOption.filterNot(_ == null).getOrElse(TsVector.apply(""))
      tsVector.value.split(' ').toList.flatMap { arrayOfOccurences =>
        arrayOfOccurences.split(":").toList match {
          case word :: occurrences :: Nil =>
            List(
              ReportWordOccurrence(
                value = word.replace("\'", ""),
                count = occurrences.split(",").length
              )
            )
          case _ => List.empty[ReportWordOccurrence]
        }
      }
    }

  def getPhoneReports(start: Option[LocalDate], end: Option[LocalDate]): Future[List[Report]] =
    db
      .run(
        table
          .filter(_.phone.isDefined)
          .filterOpt(start) { case (table, start) =>
            table.creationDate >= ZonedDateTime.of(start, LocalTime.MIN, ZoneOffset.UTC.normalized()).toOffsetDateTime
          }
          .filterOpt(end) { case (table, end) =>
            table.creationDate < ZonedDateTime.of(end, LocalTime.MAX, ZoneOffset.UTC.normalized()).toOffsetDateTime
          }
          .to[List]
          .result
      )
}

object ReportRepository {
  object ReportFileOrdering extends Ordering[Report] {
    def compare(a: Report, b: Report) =
      b.creationDate compareTo (a.creationDate)
  }

  implicit class RegexLikeOps(s: Rep[String]) {
    def regexLike(p: Rep[String]): Rep[Boolean] = {
      val expr = SimpleExpression.binary[String, String, Boolean] { (s, p, qb) =>
        qb.expr(s)
        qb.sqlBuilder += " ~* "
        qb.expr(p)
      }
      expr.apply(s, p)
    }
  }

  def queryFilter(filter: ReportFilter): Query[ReportTable, Report, Seq] =
    ReportTable.table
      .filterOpt(filter.email) { case (table, email) =>
        table.email === EmailAddress(email)
      }
      .filterOpt(filter.websiteURL) { case (table, websiteURL) =>
        table.websiteURL.map(_.asColumnOf[String]) like s"%$websiteURL%"
      }
      .filterOpt(filter.phone) { case (table, reportedPhone) =>
        table.phone.map(_.asColumnOf[String]) like s"%$reportedPhone%"
      }
      .filterOpt(filter.hasWebsite) { case (table, websiteRequired) =>
        table.websiteURL.isDefined === websiteRequired
      }
      .filterOpt(filter.hasPhone) { case (table, phoneRequired) =>
        table.phone.isDefined === phoneRequired
      }
      .filterOpt(filter.hasCompany) { case (table, hasCompany) =>
        table.companyId.isDefined === hasCompany
      }
      .filterOpt(filter.hasForeignCountry) { case (table, hasForeignCountry) =>
        table.companyCountry.isDefined === hasForeignCountry
      }
      .filterIf(filter.companyIds.nonEmpty)(_.companyId.map(_.inSetBind(filter.companyIds)).getOrElse(false))
      .filterIf(filter.siretSirenList.nonEmpty) { case table =>
        table.companySiret
          .map(siret =>
            (siret inSetBind filter.siretSirenList
              .filter(_.matches(SIRET.pattern))
              .map(SIRET.fromUnsafe(_))
              .distinct) ||
              (SubstrSQLFunction(siret.asColumnOf[String], 0.bind, 10.bind) inSetBind filter.siretSirenList
                .filter(_.matches(SIREN.pattern))
                .distinct)
          )
          .getOrElse(false)
      }
      .filterOpt(filter.siretSirenDefined) { case (table, siretSirenDefined) =>
        if (siretSirenDefined) table.companySiret.nonEmpty else table.companySiret.isEmpty
      }
      .filterOpt(filter.companyName) { case (table, companyName) =>
        table.companyName like s"${companyName}%"
      }
      .filterIf(filter.companyCountries.nonEmpty) { case table =>
        table.companyCountry
          .map(country => country.inSet(filter.companyCountries.map(Country.fromCode)))
          .getOrElse(false)
      }
      .filterOpt(filter.start) { case (table, start) =>
        table.creationDate >= start
      }
      .filterOpt(filter.end) { case (table, end) =>
        table.creationDate <= end
      }
      .filterOpt(filter.category) { case (table, category) =>
        table.category === category
      }
      .filterIf(filter.status.nonEmpty) { case table =>
        table.status.inSet(filter.status.map(_.entryName))
      }
      .filterIf(filter.withTags.nonEmpty) { table =>
        table.tags @& filter.withTags.toList.bind
      }
      .filterNot { table =>
        table.tags @& filter.withoutTags.toList.bind
      }
      .filterOpt(filter.details) { case (table, details) =>
        ArrayToStringSQLFunction(table.subcategories, ",", "") ++ ArrayToStringSQLFunction(
          table.details,
          ",",
          ""
        ) regexLike s"${details}"
      }
      .filterOpt(filter.employeeConsumer) { case (table, employeeConsumer) =>
        table.employeeConsumer === employeeConsumer
      }
      .filterOpt(filter.contactAgreement) { case (table, contactAgreement) =>
        table.contactAgreement === contactAgreement
      }
      .filterOpt(filter.hasAttachment) { case (table, hasAttachment) =>
        val exists = ReportFileTable.table
          .filter(x => x.reportId === table.id)
          .map(_.reportId)
          .exists
        if (hasAttachment) exists else !exists
      }
      .filterIf(filter.departments.nonEmpty) { case (table) =>
        filter.departments
          .flatMap(toPostalCode)
          .map(dep => table.companyPostalCode.asColumnOf[String] like s"${dep}%")
          .reduceLeft(_ || _)
      }
      .filterIf(filter.activityCodes.nonEmpty) { case (table) =>
        table.companyActivityCode.inSetBind(filter.activityCodes).getOrElse(false)
      }

}
