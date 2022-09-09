package tasks.report

import models._
import models.report.ReportCategory
import org.specs2.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import models.report.ReportTag
import utils._

import java.time.Period
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.time.OffsetDateTime

class DailyReportNotification(implicit ee: ExecutionEnv) extends DailyReportNotificationTaskSpec {
  override def is =
    s2"""
         When daily reportNotificationTask task run                                      ${step {
        Await.result(reportNotificationTask.runPeriodicNotificationTask(runningTime, Period.ofDays(1)), Duration.Inf)
      }}
         And a mail is sent to the user subscribed by category                           ${mailMustHaveBeenSent(
        Seq(covidEmail),
        s"[SignalConso] Un nouveau signalement",
        views.html.mails.dgccrf
          .reportNotification(covidSubscription, Seq((covidReport, List.empty)), runningDate.minusDays(1))
          .toString
      )}
         And a mail is sent to the user subscribed by tag                                ${mailMustHaveBeenSent(
        Seq(tagEmail),
        s"[SignalConso] [Produits dangereux] Un nouveau signalement",
        views.html.mails.dgccrf
          .reportNotification(tagSubscription, Seq((tagReport, List.empty)), runningDate.minusDays(1))
          .toString
      )}
         And a mail is sent to the user subscribed by country                            ${mailMustHaveBeenSent(
        Seq(countryEmail),
        s"[SignalConso] Un nouveau signalement",
        views.html.mails.dgccrf
          .reportNotification(countrySubscription, Seq((countryReport, List.empty)), runningDate.minusDays(1))
          .toString
      )}
    """
}

abstract class DailyReportNotificationTaskSpec(implicit ee: ExecutionEnv)
    extends Specification
    with AppSpec
    with FutureMatchers {

  val (app, components) = TestApp.buildApp(
    None
  )

  lazy val subscriptionRepository = components.subscriptionRepository
  lazy val reportRepository = components.reportRepository
  lazy val companyRepository = components.companyRepository
  lazy val reportNotificationTask = components.reportNotificationTask
  lazy val mailerService = components.mailer
  lazy val attachementService = components.attachmentService

  implicit lazy val frontRoute = components.frontRoute
  implicit lazy val contactAddress = emailConfiguration.contactAddress

  implicit val ec = ee.executionContext

  val runningTime = OffsetDateTime.now.plusDays(1)
  val runningDate = runningTime.toLocalDate()

  val covidDept = "01"
  val tagDept = "02"

  val covidEmail = Fixtures.genEmailAddress("covid", "abo").sample.get
  val tagEmail = Fixtures.genEmailAddress("tag", "abo").sample.get
  val countryEmail = Fixtures.genEmailAddress("tag", "abo").sample.get

  val covidSubscription = Subscription(
    userId = None,
    email = Some(covidEmail),
    departments = List(covidDept),
    categories = List(ReportCategory.Coronavirus),
    frequency = Period.ofDays(1)
  )

  val tagSubscription = Subscription(
    userId = None,
    email = Some(tagEmail),
    departments = List(tagDept),
    withTags = List(ReportTag.ProduitDangereux),
    frequency = Period.ofDays(1)
  )

  val countrySubscription = Subscription(
    userId = None,
    email = Some(countryEmail),
    countries = List(Country.Suisse),
    frequency = Period.ofDays(1)
  )

  val company = Fixtures.genCompany.sample.get
  val covidReport = Fixtures
    .genReportForCompany(company)
    .sample
    .get
    .copy(
      companyAddress = Address(postalCode = Some(covidDept + "000")),
      category = ReportCategory.Coronavirus.entryName
    )
  val tagReport = Fixtures
    .genReportForCompany(company)
    .sample
    .get
    .copy(companyAddress = Address(postalCode = Some(tagDept + "000")), tags = List(ReportTag.ProduitDangereux))
  val countryReport =
    Fixtures.genReportForCompany(company).sample.get.copy(companyAddress = Address(country = Some(Country.Suisse)))

  override def setupData() =
    Await.result(
      for {
        _ <- companyRepository.getOrCreate(company.siret, company)
        _ <- reportRepository.create(covidReport)
        _ <- reportRepository.create(tagReport)
        _ <- reportRepository.create(countryReport)
        _ <- subscriptionRepository.create(covidSubscription)
        _ <- subscriptionRepository.create(tagSubscription)
        _ <- subscriptionRepository.create(countrySubscription)
      } yield (),
      Duration.Inf
    )

  def mailMustHaveBeenSent(recipients: Seq[EmailAddress], subject: String, bodyHtml: String) =
    there was one(mailerService)
      .sendEmail(
        emailConfiguration.from,
        recipients,
        Nil,
        subject,
        bodyHtml,
        attachementService.defaultAttachments
      )
}
