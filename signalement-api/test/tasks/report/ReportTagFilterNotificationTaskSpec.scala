package tasks.report

import models._
import models.report.ReportTag
import org.specs2.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import utils._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.time.OffsetDateTime
import java.time.Period

class DailyReporFilterWithTagNotification(implicit ee: ExecutionEnv) extends ReportTagFilterNotificationTaskSpec {

  override def is =
    s2"""
         When daily reportNotificationTask task run                                      ${step {
        Await.result(reportNotificationTask.runPeriodicNotificationTask(runningTime, Period.ofDays(1)), Duration.Inf)
      }}
         And a mail is sent to the user subscribed by tag                                ${mailMustHaveBeenSent(
        Seq(tagEmail),
        "[SignalConso] [Produits dangereux] Un nouveau signalement",
        views.html.mails.dgccrf
          .reportNotification(tagSubscription, Seq((reportProduitDangereux, List.empty)), runningDate.minusDays(1))
          .toString
      )}
    """
}

class DailyReportFilterWithoutTagNotification(implicit ee: ExecutionEnv) extends ReportTagFilterNotificationTaskSpec {

  override def is =
    s2"""
         When daily reportNotificationTask task run                                      ${step {
        Await.result(reportNotificationTask.runPeriodicNotificationTask(runningTime, Period.ofDays(1)), Duration.Inf)
      }}
         And a mail is sent to the user subscribed without tag                                ${mailMustHaveBeenSent(
        Seq(noTagEmail),
        "[SignalConso] Un nouveau signalement",
        views.html.mails.dgccrf
          .reportNotification(noTagSubscription, Seq((reportNoTag, List.empty)), runningDate.minusDays(1))
          .toString
      )}
    """
}

abstract class ReportTagFilterNotificationTaskSpec(implicit ee: ExecutionEnv)
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
  val tagDept = "02"

  val tagEmail = Fixtures.genEmailAddress("tag", "abo").sample.get
  val noTagEmail = Fixtures.genEmailAddress("notag", "abo").sample.get

  val tagSubscription = Subscription(
    userId = None,
    email = Some(tagEmail),
    departments = List(tagDept),
    withTags = List(ReportTag.ProduitDangereux),
    frequency = Period.ofDays(1)
  )
  val noTagSubscription = Subscription(
    userId = None,
    email = Some(noTagEmail),
    departments = List(tagDept),
    withoutTags = List(ReportTag.ProduitDangereux),
    frequency = Period.ofDays(1)
  )

  val company = Fixtures.genCompany.sample.get

  val reportProduitDangereux = Fixtures
    .genReportForCompany(company)
    .sample
    .get
    .copy(companyAddress = Address(postalCode = Some(tagDept + "000")), tags = List(ReportTag.ProduitDangereux))

  val reportNoTag = Fixtures
    .genReportForCompany(company)
    .sample
    .get
    .copy(companyAddress = Address(postalCode = Some(tagDept + "000")), tags = List())

  override def setupData() =
    Await.result(
      for {
        _ <- companyRepository.getOrCreate(company.siret, company)
        _ <- reportRepository.create(reportProduitDangereux)
        _ <- reportRepository.create(reportNoTag)
        _ <- subscriptionRepository.create(tagSubscription)
        _ <- subscriptionRepository.create(noTagSubscription)

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
