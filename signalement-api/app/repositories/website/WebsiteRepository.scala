package repositories.website

import models._
import models.investigation.DepartmentDivision
import models.investigation.InvestigationStatus
import models.investigation.Practice
import models.website.IdentificationStatus.NotIdentified
import models.website.IdentificationStatus
import models.website.Website
import models.website.WebsiteId
import play.api.Logger
import repositories.PostgresProfile.api._
import repositories.TypedCRUDRepository
import repositories.company.CompanyTable
import repositories.report.ReportTable
import repositories.website.WebsiteColumnType._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import utils.URL

import java.time._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class WebsiteRepository(
    override val dbConfig: DatabaseConfig[JdbcProfile]
)(implicit
    override val ec: ExecutionContext
) extends TypedCRUDRepository[WebsiteTable, Website, WebsiteId]
    with WebsiteRepositoryInterface {

  val logger: Logger = Logger(this.getClass())
  override val table: TableQuery[WebsiteTable] = WebsiteTable.table

  import dbConfig._

  override def validateAndCreate(newWebsite: Website): Future[Website] =
    db.run(
      table
        .filter(_.host === newWebsite.host)
        .filter { website =>
          val hasBeenAlreadyIdentifiedByConso =
            (website.companyId === newWebsite.companyId) || (website.companyCountry === newWebsite.companyCountry) || (website.companyCountry.isEmpty && website.companyId.isEmpty)

          val hasBeenIdentifiedByAdmin = website.identificationStatus ===
            IdentificationStatus.values.toList
              .filter(_ != NotIdentified)
              .bind
              .any
          hasBeenIdentifiedByAdmin || hasBeenAlreadyIdentifiedByConso
        }
        .result
        .headOption
    ).flatMap(
      _.map(Future(_))
        .getOrElse(super.create(newWebsite))
    )

  override def searchValidAssociationByHost(host: String): Future[Seq[Website]] =
    db.run(
      table
        .filter(_.host === host)
        .filter(x => x.companyId.nonEmpty || x.companyCountry.nonEmpty)
        .filter(_.identificationStatus inSet List(IdentificationStatus.Identified))
        .result
    )

  override def searchValidWebsiteCountryAssociationByHost(host: String): Future[Seq[Website]] =
    db.run(
      table
        .filter(_.host === host)
        .filter(_.companyId.isEmpty)
        .filter(_.companyCountry.nonEmpty)
        .filter(_.identificationStatus inSet List(IdentificationStatus.Identified))
        .result
    )

  private def searchCompaniesByHost(host: String): Future[Seq[(Website, Company)]] =
    db.run(
      table
        .filter(_.host === host)
        .filter(_.identificationStatus inSet List(IdentificationStatus.Identified))
        .join(CompanyTable.table)
        .on(_.companyId === _.id)
        .result
    )

  override def removeOtherNonIdentifiedWebsitesWithSameHost(website: Website): Future[Int] =
    db.run(
      table
        .filter(_.host === website.host)
        .filterNot(_.id === website.id)
        .filterNot(_.identificationStatus inSet List(IdentificationStatus.Identified))
        .delete
    )

  override def searchCompaniesByUrl(
      url: String
  ): Future[Seq[(Website, Company)]] =
    URL(url).getHost.map(searchCompaniesByHost(_)).getOrElse(Future(Nil))

  override def listWebsitesCompaniesByReportCount(
      maybeHost: Option[String],
      identificationStatusFilter: Option[Seq[IdentificationStatus]],
      maybeOffset: Option[Long],
      maybeLimit: Option[Int],
      investigationStatusFilter: Option[Seq[InvestigationStatus]],
      practiceFilter: Option[Seq[Practice]],
      attributionFilter: Option[Seq[DepartmentDivision]],
      start: Option[OffsetDateTime],
      end: Option[OffsetDateTime],
      hasAssociation: Option[Boolean]
  ): Future[PaginatedResult[((Website, Option[Company]), Int)]] = {

    val baseQuery =
      WebsiteTable.table
        .filterOpt(maybeHost) { case (websiteTable, filterHost) => websiteTable.host like s"%${filterHost}%" }
        .filterOpt(identificationStatusFilter) { case (websiteTable, statusList) =>
          websiteTable.identificationStatus inSet statusList
        }
        .filterOpt(investigationStatusFilter) { case (websiteTable, statusList) =>
          websiteTable.investigationStatus inSet statusList
        }
        .filterOpt(practiceFilter) { case (websiteTable, practice) =>
          websiteTable.practice inSet practice
        }
        .filterOpt(attributionFilter) { case (websiteTable, attribution) =>
          websiteTable.attribution inSet attribution
        }
        .filterOpt(start) { case (table, start) =>
          table.creationDate >= start
        }
        .filterOpt(end) { case (table, end) =>
          table.creationDate <= end
        }
        .filterOpt(hasAssociation) {
          case (table, true) =>
            table.companyCountry.isDefined || table.companyId.isDefined
          case (table, false) =>
            table.companyCountry.isEmpty || table.companyId.isEmpty
        }
        .filter(_.isMarketplace === false)
        .joinLeft(CompanyTable.table)
        .on(_.companyId === _.id)
        .joinLeft(ReportTable.table)
        .on { (tupleTable, reportTable) =>
          val (websiteTable, _) = tupleTable
          websiteTable.host === reportTable.host && reportTable.host.isDefined
        }

    val query = baseQuery
      .groupBy(_._1)
      .map { case (grouped, all) => (grouped, all.map(_._2).size) }
      .sortBy { tupleTable =>
        val ((websiteTable, _), reportCount) = tupleTable
        (reportCount.desc, websiteTable.host.desc, websiteTable.id.desc)
      }
      .to[Seq]

    query.withPagination(db)(maybeOffset, maybeLimit)
  }

  def getUnkonwnReportCountByHost(
      host: Option[String],
      start: Option[LocalDate] = None,
      end: Option[LocalDate] = None
  ): Future[List[(String, Int)]] = db
    .run(
      WebsiteTable.table
        .filter(t => host.fold(true.bind)(h => t.host like s"%${h}%"))
        .filter(x => x.companyId.isEmpty && x.companyCountry.isEmpty)
        .filterOpt(start) { case (table, start) =>
          table.creationDate >= ZonedDateTime.of(start, LocalTime.MIN, ZoneOffset.UTC.normalized()).toOffsetDateTime
        }
        .filterOpt(end) { case (table, end) =>
          table.creationDate < ZonedDateTime.of(end, LocalTime.MAX, ZoneOffset.UTC.normalized()).toOffsetDateTime
        }
        .joinLeft(ReportTable.table)
        .on { (websiteTable, reportTable) =>
          websiteTable.host === reportTable.host && reportTable.host.isDefined
        }
        .groupBy(_._1.host)
        .map { case (host, report) => (host, report.map(_._2).size) }
        .sortBy(_._2.desc)
        .to[List]
        .result
    )

}
