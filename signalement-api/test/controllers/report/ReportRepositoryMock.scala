package controllers.report

import models.CountByDate
import models.PaginatedResult
import models.UserRole
import models.report.Report
import models.report.ReportDraft
import models.report.ReportFile
import models.report.ReportFilter
import models.report.ReportStatus
import models.report.ReportTag
import models.report.ReportWordOccurrence
import repositories.report.ReportRepositoryInterface
import utils.CRUDRepositoryMock
import utils.EmailAddress

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import scala.collection.SortedMap
import scala.collection.mutable
import scala.concurrent.Future

class ReportRepositoryMock(database: mutable.Map[UUID, Report] = mutable.Map.empty[UUID, Report])
    extends CRUDRepositoryMock[Report](database, _.id)
    with ReportRepositoryInterface {

  def findSimilarReportList(report: ReportDraft, after: OffsetDateTime): Future[List[Report]] =
    ???

  override def findByEmail(email: EmailAddress): Future[Seq[Report]] = ???

  override def countByDepartments(start: Option[LocalDate], end: Option[LocalDate]): Future[Seq[(String, Int)]] = ???

  override def count(filter: ReportFilter): Future[Int] = ???

  override def getMonthlyCount(filter: ReportFilter, ticks: Int): Future[Seq[CountByDate]] = ???

  override def getDailyCount(filter: ReportFilter, ticks: Int): Future[Seq[CountByDate]] = ???

  override def getReports(companyId: UUID): Future[List[Report]] = ???

  override def getWithWebsites(): Future[List[Report]] = ???

  override def getWithPhones(): Future[List[Report]] = ???

  override def getReportsStatusDistribution(companyId: Option[UUID], userRole: UserRole): Future[Map[String, Int]] = ???

  override def getReportsTagsDistribution(companyId: Option[UUID], userRole: UserRole): Future[Map[ReportTag, Int]] =
    ???

  override def getHostsByCompany(companyId: UUID): Future[Seq[String]] = ???

  override def getReportsWithFiles(filter: ReportFilter): Future[SortedMap[Report, List[ReportFile]]] = ???

  override def getReports(
      filter: ReportFilter,
      offset: Option[Long],
      limit: Option[Int]
  ): Future[PaginatedResult[Report]] = ???

  override def getReportsByIds(ids: List[UUID]): Future[List[Report]] = ???

  override def getByStatus(status: ReportStatus): Future[List[Report]] = ???

  override def getPendingReports(companiesIds: List[UUID]): Future[List[Report]] = ???

  override def getPhoneReports(start: Option[LocalDate], end: Option[LocalDate]): Future[List[Report]] = ???

  override def cloudWord(companyId: UUID): Future[List[ReportWordOccurrence]] = ???
}
