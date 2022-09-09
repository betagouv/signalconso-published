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
import tasks.model.TaskType.CloseUnreadReport
import tasks.model.TaskType.RemindUnreadReportsByEmail
import utils.Constants.ActionEvent
import utils.Constants.ActionEvent.ActionEventValue
import utils.Constants.ActionEvent.EMAIL_PRO_REMIND_NO_READING
import utils.Constants.EventType.PRO
import utils.AppSpec
import utils.EmailAddress
import utils.Fixtures
import utils.TestApp

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class RemindOnceUnreadWithAccessReport(implicit ee: ExecutionEnv) extends UnreadWithAccessReportTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = notReadReport.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    s2"""
         Given a pro user with activated account                                      ${step(setupUser(proUser))}
         Given a report with status "ReportStatus.TraitementEnCours" created more than 7 days    ${step(
        setupReport(report)
      )}
         When remind task run                                                         ${step {
        result = Await.result(
          reportTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then an event "RELANCE" is created                                           ${eventMustHaveBeenCreatedWithAction(
        report.id,
        ActionEvent.EMAIL_PRO_REMIND_NO_READING
      )}
         And the report is not updated                                                ${reporStatustMustNotHaveBeenUpdated(
        report
      )}
         And a mail is sent to the professional                                       ${mailMustHaveBeenSent(
        proUser.email,
        "Nouveau signalement",
        views.html.mails.professional
          .reportUnreadReminder(report, runningDateTime.plus(mailReminderDelay.multipliedBy(2)))
          .toString
      )}
     And outcome is empty ${result mustEqual Valid(List((report.id, RemindUnreadReportsByEmail)))}
    """
  }
}

class DontRemindUnreadWithAccessReport(implicit ee: ExecutionEnv) extends UnreadWithAccessReportTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = notReadReport.copy(creationDate = runningDateTime.minus(mailReminderDelay).plusDays(1))
    s2"""
         Given a pro user with activated account                                      ${step(setupUser(proUser))}
         Given a report with status "ReportStatus.TraitementEnCours" created less than 7 days    ${step(
        setupReport(report)
      )}
         When remind task run                                                        ${step {
        result = Await.result(
          reportTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then no event is created                                                     ${eventMustNotHaveBeenCreated(
        report.id,
        List.empty
      )}
         And the report is not updated                                                ${reporStatustMustNotHaveBeenUpdated(
        report
      )}
         And no mail is sent                                                          ${mailMustNotHaveBeenSent()}
         And outcome is empty ${result mustEqual noTaskProcessed}
    """
  }
}

class RemindTwiceUnreadWithAccessReport(implicit ee: ExecutionEnv) extends UnreadWithAccessReportTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = notReadReport.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    val event = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    s2"""
         Given a pro user with activated account                                      ${step(setupUser(proUser))}
         Given a report with status "ReportStatus.TraitementEnCours"                             ${step(
        setupReport(report)
      )}
         Given a previous remind made more than 7 days                                ${step(setupEvent(event))}
         When remind task run                                                         ${step {
        result = Await.result(
          reportTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then an event "RELANCE" is created                                           ${eventMustHaveBeenCreatedWithAction(
        report.id,
        ActionEvent.EMAIL_PRO_REMIND_NO_READING
      )}
         And the report is not updated                                                ${reporStatustMustNotHaveBeenUpdated(
        report
      )}
         And a mail is sent to the professional                                       ${mailMustHaveBeenSent(
        proUser.email,
        "Nouveau signalement",
        views.html.mails.professional.reportUnreadReminder(report, runningDateTime.plus(mailReminderDelay)).toString
      )}
    And outcome is successful RemindReportByMail reminder ${result mustEqual Valid(
        List(
          (report.id, RemindUnreadReportsByEmail)
        )
      )}
    """
  }
}

class DontRemindTwiceUnreadWithAccessReport(implicit ee: ExecutionEnv) extends UnreadWithAccessReportTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = notReadReport.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    val event = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).plusDays(1))
    s2"""
         Given a pro user with activated account                                      ${step(setupUser(proUser))}
         Given a report with status "ReportStatus.TraitementEnCours"                             ${step(
        setupReport(report)
      )}
         Given a previous remind made more than 7 days                                ${step(setupEvent(event))}
         When remind task run                                                         ${step {
        result = Await.result(
          reportTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then no event is created                                                     ${eventMustNotHaveBeenCreated(
        report.id,
        List(event)
      )}
         And the report is not updated                                                ${reporStatustMustNotHaveBeenUpdated(
        report
      )}
         And no mail is sent                                                          ${mailMustNotHaveBeenSent()}
         And outcome is empty                                         ${result mustEqual noTaskProcessed}
    """
  }
}

class CloseUnreadWithAccessReport(implicit ee: ExecutionEnv) extends UnreadWithAccessReportTaskSpec {
  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = notReadReport.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    val event1 = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(8))
    val event2 = reminderEvent.copy(
      creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1),
      id = UUID.randomUUID
    )
    s2"""
         Given a pro user with activated account                                      ${step(setupUser(proUser))}
         Given a report with status "ReportStatus.TraitementEnCours"                             ${step(
        setupReport(report)
      )}
         Given twice previous remind made more than 7 days                            ${step(setupEvent(event1))}
                                                                                      ${step(setupEvent(event2))}
         When remind task run                                                         ${step {
        result = Await.result(
          reportTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then an event "NON_CONSULTE" is created                                      ${eventMustHaveBeenCreatedWithAction(
        report.id,
        ActionEvent.REPORT_CLOSED_BY_NO_READING
      )}
         And the report status is updated to "SIGNALEMENT_NON_CONSULTE"               ${reportMustHaveBeenUpdatedWithStatus(
        report.id,
        ReportStatus.NonConsulte
      )}
         And a mail is sent to the consumer                                           ${mailMustHaveBeenSent(
        report.email,
        "L'entreprise n'a pas souhaité consulter votre signalement",
        views.html.mails.consumer.reportClosedByNoReading(report).toString,
        attachementService.attachmentSeqForWorkflowStepN(3)
      )}
    And outcome is successful CloseUnreadReport                                    ${result mustEqual Valid(
        List(
          (report.id, CloseUnreadReport)
        )
      )}
   """
  }
}

class DontCloseUnreadWithAccessReport(implicit ee: ExecutionEnv) extends UnreadWithAccessReportTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = notReadReport.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(1))
    val event1 = reminderEvent.copy(creationDate = runningDateTime.minus(mailReminderDelay).minusDays(8))
    val event2 = reminderEvent.copy(
      creationDate = runningDateTime.minus(mailReminderDelay).plusDays(1),
      id = UUID.randomUUID
    )
    s2"""
         Given a pro user with activated account                                      ${step(setupUser(proUser))}
         Given a report with status "ReportStatus.TraitementEnCours"                             ${step(
        setupReport(report)
      )}
         Given a first remind made more than 7 days                                   ${step(setupEvent(event1))}
         Given a second remind made less than 7 days                                  ${step(setupEvent(event2))}
         When remind task run                                                         ${step {
        result = Await.result(
          reportTask.runTask(runningDateTime.toLocalDateTime),
          Duration.Inf
        )
      }}
         Then no event is created                                                     ${eventMustNotHaveBeenCreated(
        report.id,
        List(event1, event2)
      )}
         And the report is not updated                                                ${reporStatustMustNotHaveBeenUpdated(
        report
      )}
         And no mail is sent                                                          ${mailMustNotHaveBeenSent()}
         And outcome empty ${result mustEqual noTaskProcessed}
   """
  }
}

abstract class UnreadWithAccessReportTaskSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with Mockito
    with FutureMatchers {

  val (app, components) = TestApp.buildApp(
    None
  )

  implicit val ec = ee.executionContext
  val mailReminderDelay = taskConfiguration.report.mailReminderDelay

  val runningDateTime = OffsetDateTime.now

  val proUser = Fixtures.genProUser.sample.get

  val company = Fixtures.genCompany.sample.get
  val notReadReport = Fixtures
    .genReportForCompany(company)
    .sample
    .get
    .copy(
      status = ReportStatus.TraitementEnCours
    )

  val reminderEvent = Fixtures.genEventForReport(notReadReport.id, PRO, EMAIL_PRO_REMIND_NO_READING).sample.get

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

  val noTaskProcessed = Valid(List.empty[Task])

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

  def reporStatustMustNotHaveBeenUpdated(report: Report) =
    reportRepository.get(report.id).map(_.get.status) must beEqualTo(report.status).await

  lazy val userRepository = components.userRepository
  lazy val reportRepository = components.reportRepository
  lazy val eventRepository = components.eventRepository
  lazy val reportTask = components.reportTask
  lazy val companyRepository = components.companyRepository
  lazy val companyAccessRepository = components.companyAccessRepository
  lazy val AccessTokenRepositoryInterface = components.accessTokenRepository
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
}
