package orchestrators
import cats.implicits.catsSyntaxOption

import controllers.error.AppError.CompanySiretNotFound
import controllers.error.AppError.ReportNotFound
import io.scalaland.chimney.dsl.TransformerOps
import models.User
import models.UserRole
import models.event.Event
import models.event.EventUser
import models.event.EventWithUser
import play.api.Logger
import repositories.company.CompanyRepositoryInterface
import repositories.event.EventFilter
import repositories.event.EventRepositoryInterface
import repositories.report.ReportRepositoryInterface
import utils.Constants.ActionEvent.REPORT_PRO_RESPONSE
import utils.Constants.ActionEvent.REPORT_READING_BY_PRO
import utils.Constants.EventType
import utils.SIRET

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait EventsOrchestratorInterface {

  def getReportsEvents(
      reportId: UUID,
      eventType: Option[String],
      userRole: UserRole
  ): Future[List[EventWithUser]]

  def getCompanyEvents(
      siret: SIRET,
      eventType: Option[String],
      userRole: UserRole
  ): Future[List[EventWithUser]]
}

class EventsOrchestrator(
    eventRepository: EventRepositoryInterface,
    reportRepository: ReportRepositoryInterface,
    companyRepository: CompanyRepositoryInterface
)(implicit
    val ec: ExecutionContext
) extends EventsOrchestratorInterface {

  private val logger = Logger(this.getClass)

  override def getReportsEvents(
      reportId: UUID,
      eventType: Option[String],
      userRole: UserRole
  ): Future[List[EventWithUser]] =
    for {
      maybeReport <- reportRepository.get(reportId)
      _ = logger.debug("Checking if report exists")
      _ <- maybeReport.liftTo[Future](ReportNotFound(reportId))
      _ = logger.debug("Found report")
      filter = buildEventFilter(eventType)
      _ = logger.debug("Fetching events")
      events <- eventRepository.getEventsWithUsers(reportId, filter)
      _ = logger.debug(s" ${events.length} reports events found")
      reportEvents = filterAndTransformEvents(userRole, events)
    } yield reportEvents

  override def getCompanyEvents(
      siret: SIRET,
      eventType: Option[String],
      userRole: UserRole
  ): Future[List[EventWithUser]] =
    for {
      maybeCompany <- companyRepository.findBySiret(siret)
      _ = logger.debug("Checking if company exists")
      company <- maybeCompany.liftTo[Future](CompanySiretNotFound(siret))
      _ = logger.debug("Found company")
      filter = buildEventFilter(eventType)
      _ = logger.debug("Fetching events")
      events <- eventRepository.getCompanyEventsWithUsers(company.id, filter)
      _ = logger.debug(s" ${events.length} company events found")
      companyEvents = filterAndTransformEvents(userRole, events)
    } yield companyEvents

  private def filterAndTransformEvents(userRole: UserRole, events: List[(Event, Option[User])]): List[EventWithUser] =
    filterOnUserRole(userRole, events).map { case (event, maybeUser) =>
      val maybeEventUser = maybeUser
        // Do not return event user if requesting user is a PRO user
        .filterNot(_.userRole == UserRole.Professionnel)
        .map(
          _.into[EventUser]
            .withFieldComputed(_.role, _.userRole)
            .transform
        )
      EventWithUser(event, maybeEventUser)
    }

  private def filterOnUserRole(userRole: UserRole, events: List[(Event, Option[User])]): List[(Event, Option[User])] =
    events.filter { case (event, _) =>
      userRole match {
        case UserRole.Professionnel =>
          List(REPORT_PRO_RESPONSE, REPORT_READING_BY_PRO) contains event.action
        case _ => true
      }
    }

  private def buildEventFilter(eventType: Option[String]) =
    eventType match {
      case Some(_) => EventFilter(eventType = Some(EventType.fromValue(eventType.get)))
      case None    => EventFilter()
    }

}
