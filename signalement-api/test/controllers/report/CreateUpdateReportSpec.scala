package controllers.report

import java.time.OffsetDateTime
import java.util.UUID
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.mohiva.play.silhouette.test._
import models._
import models.event.Event
import models.report.Report
import models.report.ReportCompany
import models.report.ReportConsumerUpdate
import models.report.ReportStatus
import models.report.ReportTag
import org.specs2.Specification
import org.specs2.matcher._
import play.api.libs.json.Json
import play.api.libs.mailer.Attachment
import play.api.test._
import repositories.event.EventFilter
import utils.Constants.ActionEvent.ActionEventValue
import utils.Constants.ActionEvent
import utils.Constants.Departments
import utils.AppSpec
import utils.EmailAddress
import utils.Fixtures
import utils.TestApp
import utils.silhouette.auth.AuthEnv

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object CreateReportFromDomTom extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a draft report which concerns
          a dom tom department                                              ${step {
        draftReport =
          draftReport.copy(companyAddress = Some(Address(postalCode = Some(Departments.CollectivitesOutreMer(0)))))
      }}
         When create the report                                             ${step(createReport())}
         Then create the report with reportStatusList "ReportStatus.TraitementEnCours" ${reportMustHaveBeenCreatedWithStatus(
        ReportStatus.TraitementEnCours
      )}
         And send an acknowledgment mail to the consumer                    ${mailMustHaveBeenSent(
        draftReport.email,
        "Votre signalement",
        views.html.mails.consumer.reportAcknowledgment(report, Nil).toString,
        attachmentService.attachmentSeqForWorkflowStepN(2)
      )}
    """
}
object CreateReportForEmployeeConsumer extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a draft report which concerns
          an experimentation department                                   ${step {
        draftReport = draftReport.copy(companyAddress = Some(Address(postalCode = Some(Departments.ALL(0)))))
      }}
          an employee consumer                                            ${step {
        draftReport = draftReport.copy(employeeConsumer = true)
      }}
         When create the report                                           ${step(createReport())}
         Then create the report with reportStatusList "EMPLOYEE_CONSUMER" ${reportMustHaveBeenCreatedWithStatus(
        ReportStatus.LanceurAlerte
      )}
         And send an acknowledgment mail to the consumer                  ${mailMustHaveBeenSent(
        draftReport.email,
        "Votre signalement",
        views.html.mails.consumer.reportAcknowledgment(report, Nil).toString
      )}
    """
}

object CreateReportForProWithoutAccount extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a draft report which concerns
          a professional who has no account                                   ${step {
        draftReport = draftReport.copy(companySiret = Some(anotherCompany.siret))
      }}
         When create the report                                               ${step(createReport())}
         Then create the report with reportStatusList "ReportStatus.TraitementEnCours"   ${reportMustHaveBeenCreatedWithStatus(
        ReportStatus.TraitementEnCours
      )}
         And create an event "EMAIL_CONSUMER_ACKNOWLEDGMENT"                  ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_CONSUMER_ACKNOWLEDGMENT
      )}
         And send an acknowledgment mail to the consumer                      ${mailMustHaveBeenSent(
        draftReport.email,
        "Votre signalement",
        views.html.mails.consumer.reportAcknowledgment(report, Nil).toString,
        attachmentService.attachmentSeqForWorkflowStepN(2)
      )}
    """
}

object CreateReportForProWithActivatedAccount extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a draft report which concerns
          a professional who has an activated account                   ${step {
        draftReport = draftReport.copy(companySiret = Some(existingCompany.siret))
      }}
         When create the report                                         ${step(createReport())}
         Then create the report with status "ReportStatus.TraitementEnCours"       ${reportMustHaveBeenCreatedWithStatus(
        ReportStatus.TraitementEnCours
      )}
         And send an acknowledgment mail to the consumer                ${mailMustHaveBeenSent(
        draftReport.email,
        "Votre signalement",
        views.html.mails.consumer.reportAcknowledgment(report, Nil).toString,
        attachmentService.attachmentSeqForWorkflowStepN(2)
      )}
         And create an event "EMAIL_CONSUMER_ACKNOWLEDGMENT"            ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_CONSUMER_ACKNOWLEDGMENT
      )}
         And create an event "EMAIL_PRO_NEW_REPORT"                     ${eventMustHaveBeenCreatedWithAction(
        ActionEvent.EMAIL_PRO_NEW_REPORT
      )}
         And send a mail to the pro                                     ${mailMustHaveBeenSent(
        proUser.email,
        "Nouveau signalement",
        views.html.mails.professional.reportNotification(report).toString
      )}
    """
}

object CreateReportOnDangerousProduct extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a draft report which concerns
          a dangerous product                                           ${step {
        draftReport =
          draftReport.copy(companySiret = Some(existingCompany.siret), tags = List(ReportTag.ProduitDangereux))
      }}
         When create the report                                         ${step(createReport())}
         Then create the report with status "NA"                        ${reportMustHaveBeenCreatedWithStatus(
        ReportStatus.NA
      )}
         And send an acknowledgment mail to the consumer                ${mailMustHaveBeenSent(
        draftReport.email,
        "Votre signalement",
        views.html.mails.consumer.reportAcknowledgment(report, Nil).toString
      )}
    """
}

object UpdateReportConsumer extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a preexisting report                                     ${step { report = existingReport }}
         When the report consumer is updated                            ${step {
        updateReportConsumer(report.id, reportConsumerUpdate)
      }}
         Then the report contains updated info                          ${checkReport(
        report.copy(
          firstName = reportConsumerUpdate.firstName,
          lastName = reportConsumerUpdate.lastName,
          email = reportConsumerUpdate.email,
          contactAgreement = reportConsumerUpdate.contactAgreement,
          consumerReferenceNumber = reportConsumerUpdate.consumerReferenceNumber
        )
      )}
    """
}

object UpdateReportCompanySameSiret extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a preexisting report                                     ${step { report = existingReport }}
         When the report company is updated with same Siret             ${step {
        updateReportCompany(report.id, reportCompanySameSiret)
      }}
         Then the report contains updated info                          ${checkReport(
        report.copy(
          companyName = Some(reportCompanySameSiret.name),
          companyAddress = reportCompanySameSiret.address,
          companySiret = Some(reportCompanySameSiret.siret)
        )
      )}
    """
}

object UpdateReportCompanyAnotherSiret extends CreateUpdateReportSpec {
  override def is =
    s2"""
         Given a preexisting report                                     ${step { report = existingReport }}
         When the report company is updated with same Siret             ${step {
        updateReportCompany(report.id, reportCompanyAnotherSiret)
      }}
         Then the report contains updated info and the status is reset  ${checkReport(
        report.copy(
          companyId = Some(anotherCompany.id),
          companyName = Some(reportCompanyAnotherSiret.name),
          companyAddress = reportCompanyAnotherSiret.address,
          companySiret = Some(reportCompanyAnotherSiret.siret),
          status = ReportStatus.TraitementEnCours
        )
      )}
    """
}

trait CreateUpdateReportSpec extends Specification with AppSpec with FutureMatchers {

  implicit val ec = ExecutionContext.global

  lazy val reportRepository = components.reportRepository
  lazy val eventRepository = components.eventRepository
  lazy val userRepository = components.userRepository
  lazy val companyRepository = components.companyRepository
  lazy val companyAccessRepository = components.companyAccessRepository
  lazy val mailerService = components.mailer
  lazy val attachmentService = components.attachmentService
  lazy val emailValidationRepository = components.emailValidationRepository
  lazy val companyDataRepository = components.companyDataRepository

  implicit lazy val frontRoute = components.frontRoute
  implicit lazy val contactAddress = emailConfiguration.contactAddress

  val contactEmail = EmailAddress("contact@signal.conso.gouv.fr")

  val existingCompany = Fixtures.genCompany.sample.get
  val anotherCompany = Fixtures.genCompany.sample.get

  val existingCompanyData =
    Fixtures.genCompanyData(Some(existingCompany)).sample.get.copy(etablissementSiege = Some("true"))
  val anotherCompanyData =
    Fixtures.genCompanyData(Some(anotherCompany)).sample.get.copy(etablissementSiege = Some("true"))

  val existingReport = Fixtures.genReportForCompany(existingCompany).sample.get.copy(status = ReportStatus.NA)

  var draftReport = Fixtures.genDraftReport.sample.get
  var report = draftReport.generateReport(None)
  val proUser = Fixtures.genProUser.sample.get

  val concernedAdminUser = Fixtures.genAdminUser.sample.get
  val concernedAdminLoginInfo = LoginInfo(CredentialsProvider.ID, concernedAdminUser.email.value)

  val reportConsumerUpdate = Fixtures.genReportConsumerUpdate.sample.get
  val reportCompanySameSiret = Fixtures.genReportCompany.sample.get.copy(siret = existingCompany.siret)
  val reportCompanyAnotherSiret = Fixtures.genReportCompany.sample.get
    .copy(siret = anotherCompany.siret, address = Address(postalCode = Some("45000")))

  override def setupData() =
    Await.result(
      for {
        u <- userRepository.create(proUser)
        _ <- userRepository.create(concernedAdminUser)
        c <- companyRepository.getOrCreate(existingCompany.siret, existingCompany)
        _ <- companyRepository.getOrCreate(anotherCompany.siret, anotherCompany)
        _ <- companyDataRepository.create(existingCompanyData)
        _ <- companyDataRepository.create(anotherCompanyData)
        _ <- reportRepository.create(existingReport)
        _ <- Future.sequence(
          Seq(
            existingReport.email,
            draftReport.email,
            report.email
          ).distinct.map(email =>
            emailValidationRepository.create(
              EmailValidation(email = email, lastValidationDate = Some(OffsetDateTime.now()))
            )
          )
        )
        _ <- companyAccessRepository.createUserAccess(c.id, u.id, AccessLevel.ADMIN)
      } yield (),
      Duration.Inf
    )

  implicit val env: Environment[AuthEnv] = new FakeEnvironment[AuthEnv](
    Seq(
      concernedAdminLoginInfo -> concernedAdminUser
    )
  )

  val (app, components) = TestApp.buildApp(
    Some(
      env
    )
  )

  def createReport() =
    Await.result(
      components.reportController.createReport().apply(FakeRequest().withBody(Json.toJson(draftReport))),
      Duration.Inf
    )

  def updateReportCompany(reportId: UUID, reportCompany: ReportCompany) =
    Await.result(
      components.reportController
        .updateReportCompany(reportId)
        .apply(
          FakeRequest()
            .withAuthenticator[AuthEnv](concernedAdminLoginInfo)
            .withBody(Json.toJson(reportCompany))
        ),
      Duration.Inf
    )

  def updateReportConsumer(reportId: UUID, reportConsumer: ReportConsumerUpdate) =
    Await.result(
      components.reportController
        .updateReportConsumer(reportId)
        .apply(
          FakeRequest()
            .withAuthenticator[AuthEnv](concernedAdminLoginInfo)
            .withBody(Json.toJson(reportConsumer))
        ),
      Duration.Inf
    )

  def checkReport(reportData: Report) = {
    val dbReport = Await.result(reportRepository.get(reportData.id), Duration.Inf)
    dbReport.get must beEqualTo(reportData)
  }

  def mailMustHaveBeenSent(
      recipient: EmailAddress,
      subject: String,
      bodyHtml: String,
      attachments: Seq[Attachment] = attachmentService.defaultAttachments
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

  def reportMustHaveBeenCreatedWithStatus(status: ReportStatus) = {
    val reports = Await.result(reportRepository.list(), Duration.Inf).filter(_.id != existingReport.id)
    val expectedReport = draftReport
      .generateReport(reports.head.companyId)
      .copy(
        id = reports.head.id,
        creationDate = reports.head.creationDate,
        status = status
      )
    report = reports.head
    reports.length must beEqualTo(1) and
      (report.companyId must beSome) and
      (report must beEqualTo(expectedReport))
  }

  def eventMustHaveBeenCreatedWithAction(action: ActionEventValue) = {
    val events = Await.result(eventRepository.list(), Duration.Inf).toList
    events.map(_.action) must contain(action)
  }

  def eventMustNotHaveBeenCreated(reportUUID: UUID, existingEvents: List[Event]) = {
    val events = Await.result(eventRepository.getEvents(reportUUID, EventFilter()), Duration.Inf)
    events.length must beEqualTo(existingEvents.length)
  }
}
