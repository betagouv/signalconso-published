package orchestrators

import akka.Done
import controllers.error.AppError.CannotReportPublicAdministration
import controllers.error.AppError.DuplicateReportCreation
import org.specs2.mutable.Specification
import utils.AppSpec
import utils.Fixtures
import utils.TestApp
import io.scalaland.chimney.dsl.TransformerOps
import models.report.DetailInputValue
import models.report.Report
import models.report.ReportDraft
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ReportOrchestratorTest(implicit ee: ExecutionEnv) extends Specification with AppSpec with FutureMatchers {

  override def afterAll(): Unit = {
    app.stop()
    ()
  }

  val (app, components) = TestApp.buildApp()

  def deriveSameReport(report: Report, creationDate: OffsetDateTime): Report = report.copy(
    id = UUID.randomUUID(),
    details = List(DetailInputValue(UUID.randomUUID().toString + ":", UUID.randomUUID().toString)),
    creationDate = creationDate
  )

  "Create Report Validation" should {

    val aDraftReport = Fixtures.genDraftReport.sample.get

    "fail when identical report has been made twice a day" in {

      val company = Fixtures.genCompany.sample.get
      val report: Report = Fixtures
        .genReportForCompany(company)
        .sample
        .get
        .copy(
          companyActivityCode = Some("90"),
          details = List(DetailInputValue(UUID.randomUUID().toString + ":", UUID.randomUUID().toString))
        )

      val res = for {
        _ <- components.companyRepository.create(company)
        _ <- components.reportRepository.create(report)
        reportDraft = report
          .into[ReportDraft]
          .withFieldComputed(_.websiteURL, _.websiteURL.websiteURL)
          .withFieldComputed(_.details, _.details)
          .withFieldConst(_.fileIds, List.empty)
          .withFieldConst(_.companyIsHeadOffice, Some(company.isHeadOffice))
          .withFieldConst(_.companyIsOpen, Some(company.isOpen))
          .transform
        _ <- components.reportOrchestrator.validateAndCreateReport(reportDraft)
      } yield ()

      res must throwA[DuplicateReportCreation].await
    }

    "fail when report on same company by same user has been made more than twice a day" in {

      val company = Fixtures.genCompany.sample.get
      val report = Fixtures
        .genReportForCompany(company)
        .sample
        .get
        .copy(
          companyActivityCode = Some("90"),
          creationDate = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC)
        )

      val secondReport =
        deriveSameReport(report, creationDate = LocalDate.now().atStartOfDay().plusHours(3).atOffset(ZoneOffset.UTC))

      val res = for {
        _ <- components.companyRepository.create(company)
        _ <- components.reportRepository.create(report)
        _ <- components.reportRepository.create(secondReport)
        reportDraft = report
          .into[ReportDraft]
          .withFieldComputed(_.websiteURL, _.websiteURL.websiteURL)
          .withFieldConst(_.fileIds, List.empty)
          .withFieldConst(_.details, List(DetailInputValue("test", "test")))
          .withFieldConst(_.companyIsHeadOffice, Some(company.isHeadOffice))
          .withFieldConst(_.companyIsOpen, Some(company.isOpen))
          .transform
        _ <- components.reportOrchestrator.validateAndCreateReport(reportDraft)
      } yield ()

      res must throwA[DuplicateReportCreation].await
    }

    "succeed when report on same company by same user has been made less than twice a day" in {

      val company = Fixtures.genCompany.sample.get
      val report = Fixtures
        .genReportForCompany(company)
        .sample
        .get
        .copy(
          companyActivityCode = Some("90")
        )

      val execution = for {
        _ <- components.companyRepository.create(company)
        _ <- components.reportRepository.create(report)
        reportDraft = report
          .into[ReportDraft]
          .withFieldComputed(_.websiteURL, _.websiteURL.websiteURL)
          .withFieldConst(_.fileIds, List.empty)
          .withFieldConst(_.details, List(DetailInputValue("test", "test")))
          .withFieldConst(_.companyIsHeadOffice, Some(company.isHeadOffice))
          .withFieldConst(_.companyIsOpen, Some(company.isOpen))
          .transform
        result <- components.reportOrchestrator.validateSpamSimilarReport(reportDraft)
      } yield result

      val res = Await.result(execution, Duration.Inf)
      res mustEqual ()

    }

    "fail when report on same company by same user has been made more than four time a week" in {

      val company = Fixtures.genCompany.sample.get
      val report = Fixtures
        .genReportForCompany(company)
        .sample
        .get
        .copy(
          companyActivityCode = Some("90")
        )

      val secondReport =
        deriveSameReport(report, creationDate = LocalDate.now().minusDays(3).atStartOfDay().atOffset(ZoneOffset.UTC))
      val thirdReport =
        deriveSameReport(report, creationDate = LocalDate.now().minusDays(4).atStartOfDay().atOffset(ZoneOffset.UTC))
      val fourthReport =
        deriveSameReport(report, creationDate = LocalDate.now().minusDays(2).atStartOfDay().atOffset(ZoneOffset.UTC))

      val res = for {
        _ <- components.companyRepository.create(company)
        _ <- components.reportRepository.create(report)
        _ <- components.reportRepository.create(secondReport)
        _ <- components.reportRepository.create(thirdReport)
        _ <- components.reportRepository.create(fourthReport)
        reportDraft = report
          .into[ReportDraft]
          .withFieldComputed(_.websiteURL, _.websiteURL.websiteURL)
          .withFieldConst(_.fileIds, List.empty)
          .withFieldConst(_.companyIsHeadOffice, Some(company.isHeadOffice))
          .withFieldConst(_.companyIsOpen, Some(company.isOpen))
          .withFieldConst(
            _.details,
            List(DetailInputValue(UUID.randomUUID().toString + ":", UUID.randomUUID().toString))
          )
          .transform
        _ <- components.reportOrchestrator.validateAndCreateReport(reportDraft)
      } yield ()

      res must throwA[DuplicateReportCreation].await
    }

    "success when report on same company by same user has been made less than four time a week" in {

      val company = Fixtures.genCompany.sample.get
      val report = Fixtures
        .genReportForCompany(company)
        .sample
        .get
        .copy(
          companyActivityCode = Some("90")
        )

      val secondReport =
        deriveSameReport(report, creationDate = LocalDate.now().minusDays(3).atStartOfDay().atOffset(ZoneOffset.UTC))
      val thirdReport =
        deriveSameReport(report, creationDate = LocalDate.now().minusDays(4).atStartOfDay().atOffset(ZoneOffset.UTC))

      val execution = for {
        _ <- components.companyRepository.create(company)
        _ <- components.reportRepository.create(report)
        _ <- components.reportRepository.create(secondReport)
        _ <- components.reportRepository.create(thirdReport)
        reportDraft = report
          .into[ReportDraft]
          .withFieldComputed(_.websiteURL, _.websiteURL.websiteURL)
          .withFieldConst(_.fileIds, List.empty)
          .withFieldConst(_.companyIsHeadOffice, Some(company.isHeadOffice))
          .withFieldConst(_.companyIsOpen, Some(company.isOpen))
          .withFieldConst(
            _.details,
            List(DetailInputValue(UUID.randomUUID().toString + ":", UUID.randomUUID().toString))
          )
          .transform
        result <- components.reportOrchestrator.validateSpamSimilarReport(reportDraft)
      } yield result

      val res = Await.result(execution, Duration.Inf)
      res mustEqual ()
    }

    "fail when reporting public company" in {
      val draftReportOnPublicCompany = aDraftReport.copy(
        companyActivityCode = Some("84.10")
      )
      val res =
        components.reportOrchestrator.validateAndCreateReport(draftReportOnPublicCompany)
      res must throwA[CannotReportPublicAdministration.type].await
    }

    "succeed when reporting private company" in {
      val draftReportOnPrivateCompany = aDraftReport.copy(
        companyActivityCode = Some("90.10")
      )
      val res =
        Await.result(components.reportOrchestrator.validateCompany(draftReportOnPrivateCompany), Duration.Inf)

      res mustEqual Done

    }

  }
}
