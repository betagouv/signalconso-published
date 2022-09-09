package tasks.report

import cats.data.Validated.Valid
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
import utils.Constants.ActionEvent
import utils.Constants.ActionEvent.ActionEventValue
import utils.AppSpec
import utils.EmailAddress
import utils.Fixtures
import utils.TestApp

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class CloseUnreadNoAccessReport(implicit ee: ExecutionEnv) extends UnreadNoAccessReportClosingTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = onGoingReport.copy(creationDate = OffsetDateTime.now.minus(noAccessReadingDelay).minusDays(1))
    s2"""
       Given a company with no activated accout
       Given a report with status "ReportStatus.TraitementEnCours" and expired reading delay   ${step(
        setupReport(report)
      )}
       When remind task run                                                         ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime),
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
    And outcome is empty ${result mustEqual Valid(List((report.id, CloseUnreadReport)))}
    """
  }
}

class DontCloseUnreadNoAccessReport(implicit ee: ExecutionEnv) extends UnreadNoAccessReportClosingTaskSpec {

  var result: TaskExecutionResults = noTaskProcessed

  override def is = {
    val report = onGoingReport.copy(creationDate = OffsetDateTime.now.minus(noAccessReadingDelay).plusDays(1))
    s2"""
       Given a company with no activated accout
       Given a report with status "ReportStatus.TraitementEnCours" and no expired reading delay    ${step(
        setupReport(report)
      )}
       When remind task run                                                             ${step {
        result = Await.result(
          reminderTask.runTask(runningDateTime),
          Duration.Inf
        )
      }}
       Then no event is created                                                         ${eventMustNotHaveBeenCreated(
        report.id,
        List.empty
      )}
       And the report is not updated                                                    ${reporStatustMustNotHaveBeenUpdated(
        report
      )}
       And no mail is sent                                                              ${mailMustNotHaveBeenSent()}
       And outcome is empty ${result mustEqual noTaskProcessed}
    """
  }
}

abstract class UnreadNoAccessReportClosingTaskSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with Mockito
    with FutureMatchers {

  val (app, components) = TestApp.buildApp(
    None
  )

  implicit lazy val frontRoute = components.frontRoute

  implicit val ec = ee.executionContext

  val noTaskProcessed = Valid(List.empty[Task])
  val runningDateTime = LocalDateTime.now
  val noAccessReadingDelay = taskConfiguration.report.noAccessReadingDelay

  val company = Fixtures.genCompany.sample.get
  val onGoingReport = Fixtures
    .genReportForCompany(company)
    .sample
    .get
    .copy(
      status = ReportStatus.TraitementEnCours
    )

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
    there was no(mailerService)
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

  def reporStatustMustNotHaveBeenUpdated(report: Report) =
    reportRepository.get(report.id).map(_.get.status) must beEqualTo(report.status).await

  lazy val companyRepository = components.companyRepository
  lazy val reportRepository = components.reportRepository
  lazy val eventRepository = components.eventRepository
  lazy val reminderTask = components.reportTask
  lazy val mailerService = components.mailer
  lazy val attachementService = components.attachmentService

  def setupReport(report: Report) =
    Await.result(reportRepository.create(report), Duration.Inf)

  override def setupData(): Unit = {
    Await.result(companyRepository.getOrCreate(company.siret, company), Duration.Inf)
    ()
  }
}
