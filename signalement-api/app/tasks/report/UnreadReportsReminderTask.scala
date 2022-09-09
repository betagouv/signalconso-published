package tasks.report

import config.TaskConfiguration
import models.event.Event.stringToDetailsJsValue
import models.User
import models.event.Event
import models.report.Report
import play.api.Logger
import repositories.event.EventRepositoryInterface
import services.Email.ProReportUnreadReminder
import services.MailService
import tasks.model.TaskType
import tasks.report.ReportTask.extractEventsWithAction
import tasks.TaskExecutionResult
import tasks.toValidated
import utils.Constants.ActionEvent.EMAIL_PRO_NEW_REPORT
import utils.Constants.ActionEvent.EMAIL_PRO_REMIND_NO_READING
import utils.Constants.EventType.SYSTEM
import utils.EmailAddress

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UnreadReportsReminderTask(
    taskConfiguration: TaskConfiguration,
    eventRepository: EventRepositoryInterface,
    emailService: MailService
)(implicit
    ec: ExecutionContext
) {

  val logger: Logger = Logger(this.getClass)

  val noAccessReadingDelay = taskConfiguration.report.noAccessReadingDelay
  val mailReminderDelay = taskConfiguration.report.mailReminderDelay

  def sendReminder(
      onGoingReportsWithAdmins: List[(Report, List[User])],
      reportEventsMap: Map[UUID, List[Event]],
      startingPoint: LocalDateTime
  ): Future[List[TaskExecutionResult]] = Future.sequence(
    extractUnreadReportsToRemindByMail(onGoingReportsWithAdmins, reportEventsMap, startingPoint)
      .map { case (report, users) =>
        remindUnreadReportByMail(report, users.map(_.email), reportEventsMap)
      }
  )

  private def extractUnreadReportsToRemindByMail(
      reportsWithAdmins: List[(Report, List[User])],
      reportEventsMap: Map[UUID, List[Event]],
      now: LocalDateTime
  ): List[(Report, List[User])] = {

    val reportWithNoRemind: List[(Report, List[User])] = reportsWithAdmins
      .filter(reportWithAdmins =>
        extractEventsWithAction(reportWithAdmins._1.id, reportEventsMap, EMAIL_PRO_REMIND_NO_READING).isEmpty
      )
      .filter(reportWithAdmins => reportWithAdmins._2.exists(_.email.nonEmpty))
      .filter(reportWithAdmins =>
        extractEventsWithAction(reportWithAdmins._1.id, reportEventsMap, EMAIL_PRO_NEW_REPORT).headOption
          .map(_.creationDate)
          .getOrElse(reportWithAdmins._1.creationDate)
          .toLocalDateTime
          .isBefore(now.minusDays(7))
      )

    val reportWithUniqueRemind: List[(Report, List[User])] = reportsWithAdmins
      .filter(reportWithAdmins =>
        extractEventsWithAction(reportWithAdmins._1.id, reportEventsMap, EMAIL_PRO_REMIND_NO_READING).length == 1
      )
      .filter(reportWithAdmins => reportWithAdmins._2.exists(_.email.nonEmpty))
      .filter(reportWithAdmins =>
        extractEventsWithAction(
          reportWithAdmins._1.id,
          reportEventsMap,
          EMAIL_PRO_REMIND_NO_READING
        ).head.creationDate.toLocalDateTime.isBefore(now.minusDays(7))
      )

    reportWithNoRemind ::: reportWithUniqueRemind
  }

  private def remindUnreadReportByMail(
      report: Report,
      adminMails: List[EmailAddress],
      reportEventsMap: Map[UUID, List[Event]]
  ) = {

    val reportExpirationDate = ReportTask.computeReportExpirationDate(
      mailReminderDelay,
      report.id,
      reportEventsMap,
      EMAIL_PRO_REMIND_NO_READING
    )
    val taskExecution: Future[Unit] = {
      logger.debug(s"Sending email")
      for {
        _ <- emailService.send(ProReportUnreadReminder(adminMails, report, reportExpirationDate))
        _ <- eventRepository.create(
          Event(
            UUID.randomUUID(),
            Some(report.id),
            report.companyId,
            None,
            OffsetDateTime.now(),
            SYSTEM,
            EMAIL_PRO_REMIND_NO_READING,
            stringToDetailsJsValue(s"Relance envoyée à ${adminMails.mkString(", ")}")
          )
        )
      } yield ()
    }

    toValidated(taskExecution, report.id, TaskType.RemindUnreadReportsByEmail)
  }

}
