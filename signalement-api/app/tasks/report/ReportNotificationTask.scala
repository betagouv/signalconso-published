package tasks.report

import akka.actor.ActorSystem
import cats.implicits.toTraverseOps
import config.TaskConfiguration
import models.report.ReportFilter
import play.api.Logger
import repositories.report.ReportRepositoryInterface
import repositories.subscription.SubscriptionRepositoryInterface
import services.Email.DgccrfReportNotification
import services.MailService
import tasks.computeStartingTime
import utils.Constants.Departments

import java.time._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

class ReportNotificationTask(
    actorSystem: ActorSystem,
    reportRepository: ReportRepositoryInterface,
    subscriptionRepository: SubscriptionRepositoryInterface,
    mailService: MailService,
    taskConfiguration: TaskConfiguration
)(implicit executionContext: ExecutionContext) {

  val logger: Logger = Logger(this.getClass)
  implicit val timeout: akka.util.Timeout = 5.seconds

  val startTime = taskConfiguration.subscription.startTime
  val initialDelay: FiniteDuration = computeStartingTime(startTime)

  val departments = Departments.ALL

  actorSystem.scheduler.scheduleWithFixedDelay(initialDelay = initialDelay, 1.days)(runnable = () => {
    logger.debug(s"initialDelay - ${initialDelay}");

    val now = OffsetDateTime.now

    if (LocalDate.now.getDayOfWeek == taskConfiguration.subscription.startDay) {
      runPeriodicNotificationTask(now, Period.ofDays(7))
    }

    runPeriodicNotificationTask(now, Period.ofDays(1))
    ()
  })

  def runPeriodicNotificationTask(now: OffsetDateTime, period: Period): Future[Unit] = {

    val end = now
    val start = end.minus(period)

    logger.debug(s"Traitement de notification des signalements - period $period - $start to $end")
    for {
      subscriptions <- subscriptionRepository.listForFrequency(period)
      reportsWithFiles <- reportRepository.getReportsWithFiles(
        ReportFilter(
          start = Some(start),
          end = Some(end)
        )
      )
      subscriptionsEmailAndReports = subscriptions.map { case (subscription, emailAddress) =>
        val filteredReport = reportsWithFiles
          .filter { case (report, _) =>
            subscription.departments.isEmpty || subscription.departments
              .map(Some(_))
              .contains(report.companyAddress.postalCode.flatMap(Departments.fromPostalCode))
          }
          .filter { case (report, _) =>
            subscription.categories.isEmpty || subscription.categories.map(_.entryName).contains(report.category)
          }
          .filter { case (report, _) =>
            subscription.sirets.isEmpty || subscription.sirets.map(Some(_)).contains(report.companySiret)
          }
          .filter { case (report, _) =>
            subscription.countries.isEmpty || subscription.countries
              .map(Some(_))
              .contains(report.companyAddress.country)
          }
          .filter { case (report, _) =>
            subscription.withTags.isEmpty || subscription.withTags.intersect(report.tags).nonEmpty
          }
          .filter { case (report, _) =>
            subscription.withoutTags.isEmpty || subscription.withoutTags.intersect(report.tags).isEmpty
          }
        (subscription, emailAddress, filteredReport)
      }
      subscriptionEmailAndNonEmptyReports = subscriptionsEmailAndReports.filter(_._3.nonEmpty)
      _ <- subscriptionEmailAndNonEmptyReports.map { case (subscription, emailAddress, filteredReport) =>
        mailService.send(
          DgccrfReportNotification(
            List(emailAddress),
            subscription,
            filteredReport.toList,
            start.toLocalDate()
          )
        )
      }.sequence
    } yield ()
  }
}
