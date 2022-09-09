package tasks.report

import akka.actor.ActorSystem
import cats.implicits._
import config.SignalConsoConfiguration
import config.TaskConfiguration
import models._
import models.event.Event
import models.report.Report
import models.report.ReportStatus
import orchestrators.CompaniesVisibilityOrchestrator
import play.api.Logger
import repositories.event.EventRepositoryInterface
import repositories.report.ReportRepositoryInterface
import tasks.computeStartingTime
import utils.Constants.ActionEvent._

import java.time._
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

class ReportTask(
    actorSystem: ActorSystem,
    reportRepository: ReportRepositoryInterface,
    eventRepository: EventRepositoryInterface,
    companiesVisibilityOrchestrator: CompaniesVisibilityOrchestrator,
    signalConsoConfiguration: SignalConsoConfiguration,
    unreadReportsReminderTask: UnreadReportsReminderTask,
    unreadReportsCloseTask: UnreadReportsCloseTask,
    readReportsReminderTask: ReadReportsReminderTask,
    noActionReportsCloseTask: NoActionReportsCloseTask,
    taskConfiguration: TaskConfiguration
)(implicit val executionContext: ExecutionContext) {

  val logger: Logger = Logger(this.getClass)

  implicit val websiteUrl = signalConsoConfiguration.websiteURL
  implicit val timeout: akka.util.Timeout = 5.seconds

  val startTime: LocalTime = taskConfiguration.report.startTime
  val initialDelay: FiniteDuration = computeStartingTime(startTime)

  val interval: FiniteDuration = taskConfiguration.report.intervalInHours

  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = initialDelay, interval = interval) { () =>
    logger.debug(s"initialDelay - ${initialDelay}");
    if (taskConfiguration.active) {
      runTask(LocalDate.now.atStartOfDay())
    }
    ()
  }

  def runTask(now: LocalDateTime) = {

    logger.info("Traitement de relance automatique")
    logger.info(s"taskDate - ${now}")

    val executedTasksOrError = for {

      unreadReportsWithAdmins <- getReportsWithAdminsByStatus(ReportStatus.TraitementEnCours)
      readReportsWithAdmins <- getReportsWithAdminsByStatus(ReportStatus.Transmis)

      reportEventsMap <- eventRepository.prefetchReportsEvents(
        (unreadReportsWithAdmins ::: readReportsWithAdmins).map(_._1)
      )
      _ = logger.info("Processing unread events")
      closedUnreadNoAccessReports <-
        unreadReportsCloseTask.closeUnread(unreadReportsWithAdmins, now)

      unreadReportsMailReminders <- unreadReportsReminderTask.sendReminder(
        unreadReportsWithAdmins,
        reportEventsMap,
        now
      )

      closedUnreadWithAccessReports <- unreadReportsCloseTask.closeUnreadWithMaxReminderEventsSent(
        unreadReportsWithAdmins,
        reportEventsMap,
        now
      )

      transmittedReportsMailReminders <- readReportsReminderTask.sendReminder(
        readReportsWithAdmins,
        reportEventsMap,
        now
      )
      closedByNoAction <- noActionReportsCloseTask.closeNoAction(readReportsWithAdmins, reportEventsMap, now)

      reminders = closedUnreadNoAccessReports.sequence combine
        closedUnreadWithAccessReports.sequence combine
        unreadReportsMailReminders.sequence combine
        transmittedReportsMailReminders.sequence combine
        closedByNoAction.sequence

      _ = logger.info("Successful reminders :")
      _ = reminders
        .map(reminder => logger.debug(s"Relance pour [${reminder.mkString(",")}]"))

      _ = logger.info("Failed reminders :")
      _ = reminders
        .leftMap(_.map(reminder => logger.warn(s"Failed report tasks [${reminder._1} - ${reminder._2}]")))

    } yield reminders

    executedTasksOrError.recoverWith { case err =>
      logger.error(
        s"Unexpected failure, cannot run report task ( task date : $now, initialDelay : $initialDelay )",
        err
      )
      Future.failed(err)
    }
  }

  private[this] def getReportsWithAdminsByStatus(status: ReportStatus): Future[List[(Report, List[User])]] =
    for {
      reports <- reportRepository.getByStatus(status)
      mapAdminsByCompanyId <- companiesVisibilityOrchestrator.fetchAdminsWithHeadOffices(
        reports.flatMap(c =>
          for {
            siret <- c.companySiret
            id <- c.companyId
          } yield (siret, id)
        )
      )
    } yield reports.flatMap(r => r.companyId.map(companyId => (r, mapAdminsByCompanyId.getOrElse(companyId, Nil))))

}

object ReportTask {

  /** Max Reminder count on same reminder type
    */
  val MaxReminderCount = 2

  /** Compute the report expiration date. The time after the pro will not be able to respond to the report anymore
    * Dependending on the number of occurence of $action event already sent. For example if pro user h
    * @param reportId
    *   Report ID
    * @param reportEventsMap
    *   List of report events linked to provided report ID
    * @param action
    *   Event already sent for that report used to compute report expiration date
    * @return
    *   Report expiration date
    */
  private[tasks] def computeReportExpirationDate(
      mailReminderDelay: Period,
      reportId: UUID,
      reportEventsMap: Map[UUID, List[Event]],
      action: ActionEventValue
  ): OffsetDateTime =
    OffsetDateTime.now.plus(
      mailReminderDelay.multipliedBy(
        MaxReminderCount - extractEventsWithAction(reportId, reportEventsMap, action).length
      )
    )

  /** Extracts event from reportEventsMap depending on provided action & report ID
    * @param reportId
    *   Report ID
    * @param reportEventsMap
    *   List of report events linked to provided report ID
    * @param action
    *   Event action
    * @return
    *   Filtered events
    */
  def extractEventsWithAction(
      reportId: UUID,
      reportEventsMap: Map[UUID, List[Event]],
      action: ActionEventValue
  ): List[Event] =
    reportEventsMap.getOrElse(reportId, List.empty).filter(_.action == action)

}
