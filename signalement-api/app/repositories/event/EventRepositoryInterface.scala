package repositories.event

import cats.data.NonEmptyList
import models.User
import models.event.Event
import models.report.Report
import models.report.ReportStatus
import models.report.ReportTag
import repositories.CRUDRepositoryInterface
import utils.Constants.ActionEvent

import java.sql.Timestamp
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Future

trait EventRepositoryInterface extends CRUDRepositoryInterface[Event] {

  def deleteByUserId(userId: UUID): Future[Int]

  def deleteByReportId(uuidReport: UUID): Future[Int]

  def getEvents(reportId: UUID, filter: EventFilter): Future[List[Event]]

  def getEventsWithUsers(reportId: UUID, filter: EventFilter): Future[List[(Event, Option[User])]]

  def getCompanyEventsWithUsers(companyId: UUID, filter: EventFilter): Future[List[(Event, Option[User])]]

  def getReportResponseReviews(companyId: Option[UUID]): Future[Seq[Event]]

  def prefetchReportsEvents(reports: List[Report]): Future[Map[UUID, List[Event]]]

  def fetchEvents(companyIds: List[UUID]): Future[Map[UUID, List[Event]]]

  def getAvgTimeUntilEvent(
      action: ActionEvent.ActionEventValue,
      companyId: Option[UUID] = None,
      status: Seq[ReportStatus] = Seq.empty,
      withoutTags: Seq[ReportTag] = Seq.empty
  ): Future[Option[Duration]]

  def getReportCountHavingEvent(action: ActionEvent.ActionEventValue, companyId: Option[UUID] = None): Future[Int]

  def getProReportStat(
      ticks: Int,
      startingDate: OffsetDateTime,
      actions: NonEmptyList[ActionEvent.ActionEventValue]
  ): Future[Vector[(Timestamp, Int)]]

}
