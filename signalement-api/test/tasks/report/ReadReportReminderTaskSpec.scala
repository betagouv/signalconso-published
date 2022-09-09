package tasks.report

import cats.data.Validated.Valid
import models._
import models.event.Event
import models.report.Report
import models.report.ReportStatus
import org.specs2.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mock.Mockito
import play.api.libs.mailer.Attachment
import repositories.event.EventFilter
import tasks.Task
import tasks.TaskExecutionResults
import tasks.model.TaskType.CloseReadReportWithNoAction
import tasks.model.TaskType.RemindReadReportByMail
import utils.Constants.ActionEvent
import utils.Constants.ActionEvent.ActionEventValue
import utils.Constants.ActionEvent.EMAIL_PRO_REMIND_NO_ACTION
import utils.Constants.ActionEvent.REPORT_READING_BY_PRO
import utils.Constants.EventType.PRO
import utils.AppSpec
import utils.EmailAddress
import utils.Fixtures
import utils.TestApp

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class RemindTransmittedReportOutOfTime(implicit ee: ExecutionEnv) extends ReadReportReminderTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val event = transmittedEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    s2"""
         Given a pro with email                                                       ${step(setupUser(proUser))}
         Given a report with status "SIGNALEMENT_TRANSMIS"                            ${step {
        setupReport(transmittedReport)
      }}
         Given an event "REPORT_READING_BY_PRO" created more than 7 days              ${step(setupEvent(event))}
         When remind task run                 ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then an event "EMAIL_PRO_REMIND_NO_ACTION" is created                        ${eventMustHaveBeenCreatedWithAction(
        transmittedReport.id,
        ActionEvent.EMAIL_PRO_REMIND_NO_ACTION
      )}
         And the report is not updated                                                ${reportStatusMustNotHaveBeenUpdated(
        transmittedReport
      )}
         And a mail is sent to the professional                                       ${mailMustHaveBeenSent(
        proUser.email,
        "Signalement en attente de réponse",
        views.html.mails.professional
          .reportTransmittedReminder(transmittedReport, OffsetDateTime.now.plusDays(14))
          .toString
      )}
     And outcome is empty ${result mustEqual Valid(List((transmittedReport.id, RemindReadReportByMail)))}
    """
  }
}

class DontRemindTransmittedReportOnTime(implicit ee: ExecutionEnv) extends ReadReportReminderTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val event = transmittedEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).plusDays(1))
    s2"""
         Given a pro with email                                                       ${step(setupUser(proUser))}
         Given a report with status "SIGNALEMENT_TRANSMIS"                            ${step {
        setupReport(transmittedReport)
      }}
         Given an event "REPORT_READING_BY_PRO" created less than 7 days              ${step(setupEvent(event))}
         When remind task run                                                         ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then no event is created                                                     ${eventMustNotHaveBeenCreated(
        transmittedReport.id,
        List(event)
      )}
         And the report is not updated                                                ${reportStatusMustNotHaveBeenUpdated(
        transmittedReport
      )}
         And no mail is sent                                                          ${mailMustNotHaveBeenSent()}
         And outcome is empty ${result mustEqual noTaskProcessed}
    """
  }
}

class RemindTwiceTransmittedReportOutOfTime(implicit ee: ExecutionEnv) extends ReadReportReminderTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val event = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    s2"""
         Given a pro with email                                                       ${step(setupUser(proUser))}
         Given a report with status "SIGNALEMENT_TRANSMIS"                            ${step {
        setupReport(transmittedReport)
      }}
         Given a previous remind made more than 7 days                                ${step(setupEvent(event))}
         When remind task run                                                         ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then an event "EMAIL_PRO_REMIND_NO_ACTION" is created                        ${eventMustHaveBeenCreatedWithAction(
        transmittedReport.id,
        ActionEvent.EMAIL_PRO_REMIND_NO_ACTION
      )}
         And the report is not updated                                                ${reportStatusMustNotHaveBeenUpdated(
        transmittedReport
      )}
         And a mail is sent to the professional                                       ${mailMustHaveBeenSent(
        proUser.email,
        "Signalement en attente de réponse",
        views.html.mails.professional
          .reportTransmittedReminder(transmittedReport, OffsetDateTime.now.plusDays(7))
          .toString
      )}
    And outcome is empty ${result mustEqual Valid(List((transmittedReport.id, RemindReadReportByMail)))}
    """
  }
}

class DontRemindTwiceTransmittedReportOnTime(implicit ee: ExecutionEnv) extends ReadReportReminderTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val event = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).plusDays(1))
    s2"""
         Given a pro with email                                                       ${step(setupUser(proUser))}
         Given a report with status "SIGNALEMENT_TRANSMIS"                            ${step {
        setupReport(transmittedReport)
      }}
         Given a previous remind made more than 7 days                                ${step(setupEvent(event))}
         When remind task run                                                         ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then no event is created                                                     ${eventMustNotHaveBeenCreated(
        transmittedReport.id,
        List(reminderEvent)
      )}
         And the report is not updated                                                ${reportStatusMustNotHaveBeenUpdated(
        transmittedReport
      )}
         And no mail is sent                                                          ${mailMustNotHaveBeenSent()}
         And outcome is empty ${result mustEqual noTaskProcessed}
    """
  }
}

class CloseTransmittedReportOutOfTime(implicit ee: ExecutionEnv) extends ReadReportReminderTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val event1 = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(8))
    val event2 = reminderEvent.copy(
      creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1),
      id = UUID.randomUUID
    )
    s2"""
         Given a pro with email                                                       ${step(setupUser(proUser))}
         Given a report with status "SIGNALEMENT_TRANSMIS"                            ${step {
        setupReport(transmittedReport)
      }}
         Given twice previous remind made more than 7 days                            ${step(setupEvent(event1))}
                                                                                      ${step(setupEvent(event2))}
         When remind task run                                                         ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then an event "REPORT_CLOSED_BY_NO_ACTION" is created                        ${eventMustHaveBeenCreatedWithAction(
        transmittedReport.id,
        ActionEvent.REPORT_CLOSED_BY_NO_ACTION
      )}
         And the report status is updated to "SIGNALEMENT_NON_CONSULTE"               ${reportMustHaveBeenUpdatedWithStatus(
        transmittedReport.id,
        ReportStatus.ConsulteIgnore
      )}
         And a mail is sent to the consumer                                           ${mailMustHaveBeenSent(
        transmittedReport.email,
        "L'entreprise n'a pas répondu au signalement",
        views.html.mails.consumer.reportClosedByNoAction(transmittedReport).toString,
        attachementService.attachmentSeqForWorkflowStepN(4)
      )}    
    And outcome is empty ${result mustEqual Valid(List((transmittedReport.id, CloseReadReportWithNoAction)))}
   """
  }
}

class DontCloseTransmittedReportOnTime(implicit ee: ExecutionEnv) extends ReadReportReminderTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val event1 = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(8))
    val event2 = reminderEvent.copy(
      creationDate = runningDateTime.minus(mailReminderDelay).plusDays(1),
      id = UUID.randomUUID
    )
    s2"""
         Given a pro with email                                                       ${step(setupUser(proUser))}
         Given a report with status "SIGNALEMENT_TRANSMIS"                            ${step {
        setupReport(transmittedReport)
      }}
         Given a first remind made more than 7 days                                   ${step(setupEvent(event1))}
         Given a second remind made less than 7 days                                  ${step(setupEvent(event2))}
         When remind task run                                                         ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then no event is created                                                     ${eventMustNotHaveBeenCreated(
        transmittedReport.id,
        List(event1, event2)
      )}
         And the report is not updated                                                ${reportStatusMustNotHaveBeenUpdated(
        transmittedReport
      )}
         And no mail is sent                                                          ${mailMustNotHaveBeenSent()}
         And outcome is empty ${result mustEqual noTaskProcessed}
   """
  }
}

abstract class ReadReportReminderTaskSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with Mockito
    with FutureMatchers {

  val (app, components) = TestApp.buildApp(None)

  implicit val ec = ee.executionContext
  val mailReminderDelay = taskConfiguration.report.mailReminderDelay

  val runningDateTime = OffsetDateTime.now

  val proUser = Fixtures.genProUser.sample.get

  val company = Fixtures.genCompany.sample.get
  val transmittedReport = Fixtures
    .genReportForCompany(company)
    .sample
    .get
    .copy(
      status = ReportStatus.Transmis
    )

  val noTaskProcessed = Valid(List.empty[Task])

  val reminderEvent = Fixtures.genEventForReport(transmittedReport.id, PRO, EMAIL_PRO_REMIND_NO_ACTION).sample.get
  val transmittedEvent = Fixtures.genEventForReport(transmittedReport.id, PRO, REPORT_READING_BY_PRO).sample.get

  def mailMustHaveBeenSent(
      recipient: EmailAddress,
      subject: String,
      bodyHtml: String,
      attachments: Seq[Attachment] = attachementService.defaultAttachments
  ) =
    there was one(mailerService)
      .sendEmail(
        emailConfiguration.from,
        Seq(recipient),
        Nil,
        subject,
        bodyHtml,
        attachments
      )

  def mailMustNotHaveBeenSent() =
    there was no(components.mailer)
      .sendEmail(
        any[EmailAddress],
        any[Seq[EmailAddress]],
        any[Seq[EmailAddress]],
        anyString,
        anyString,
        any[Seq[Attachment]]
      )

  def eventMustHaveBeenCreatedWithAction(reportUUID: UUID, action: ActionEventValue) =
    eventRepository.getEvents(reportUUID, EventFilter(action = Some(action))).map(_.head) must eventActionMatcher(
      action
    ).await

  def eventActionMatcher(action: ActionEventValue): org.specs2.matcher.Matcher[Event] = { event: Event =>
    (action == event.action, s"action doesn't match ${action}")
  }

  def eventMustNotHaveBeenCreated(reportUUID: UUID, existingEvents: List[Event]) =
    eventRepository.getEvents(reportUUID, EventFilter()).map(_.length) must beEqualTo(existingEvents.length).await

  def reportMustHaveBeenUpdatedWithStatus(reportUUID: UUID, status: ReportStatus) =
    reportRepository.get(reportUUID) must reportStatusMatcher(status).await

  def reportStatusMatcher(status: ReportStatus): org.specs2.matcher.Matcher[Option[Report]] = {
    report: Option[Report] =>
      (report.exists(report => status == report.status), s"status doesn't match ${status}")
  }

  def reportStatusMustNotHaveBeenUpdated(report: Report) =
    reportRepository.get(report.id).map(_.get.status) must beEqualTo(report.status).await

  lazy val userRepository = components.userRepository
  lazy val reportRepository = components.reportRepository
  lazy val eventRepository = components.eventRepository
  lazy val reminderTask = components.reportTask
  lazy val companyRepository = components.companyRepository
  lazy val companyAccessRepository = components.companyAccessRepository
  lazy val accessTokenRepository = components.accessTokenRepository
  lazy val mailerService = components.mailer
  lazy val attachementService = components.attachmentService

  implicit lazy val frontRoute = components.frontRoute
  implicit lazy val contactAddress = emailConfiguration.contactAddress

  def setupUser(user: User) =
    Await.result(
      for {
        company <- companyRepository.getOrCreate(company.siret, company)
        admin <- userRepository.create(user)
        _ <- companyAccessRepository.createUserAccess(company.id, admin.id, AccessLevel.ADMIN)
      } yield (),
      Duration.Inf
    )
  def setupReport(report: Report) =
    Await.result(reportRepository.create(report), Duration.Inf)
  def setupEvent(event: Event) =
    Await.result(eventRepository.create(event), Duration.Inf)
  override def setupData() = {}
}
