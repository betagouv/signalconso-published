package controllers

import actors.ReportsExtractActor
import akka.actor.ActorRef
import com.mohiva.play.silhouette.api.Silhouette
import controllers.error.AppError.MalformedQueryParams
import models._
import models.report.ReportFilter
import orchestrators.ReportOrchestrator
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import repositories.asyncfiles.AsyncFileRepositoryInterface
import utils.silhouette.api.APIKeyEnv
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithPermission
import cats.implicits.catsSyntaxOption
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import utils.QueryStringMapper
import java.time.ZoneId

class ReportListController(
    reportOrchestrator: ReportOrchestrator,
    asyncFileRepository: AsyncFileRepositoryInterface,
    reportsExtractActor: ActorRef,
    val silhouette: Silhouette[AuthEnv],
    val silhouetteAPIKey: Silhouette[APIKeyEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  implicit val timeout: akka.util.Timeout = 5.seconds
  val logger: Logger = Logger(this.getClass)

  def getReports() = SecuredAction.async { implicit request =>
    ReportFilter
      .fromQueryString(request.queryString, request.identity.userRole)
      .flatMap(filters => PaginatedSearch.fromQueryString(request.queryString).map((filters, _)))
      .fold(
        error => {
          logger.error("Cannot parse querystring" + request.queryString, error)
          Future.failed(MalformedQueryParams)
        },
        filters =>
          for {
            paginatedReports <- reportOrchestrator.getReportsForUser(
              connectedUser = request.identity,
              filter = filters._1,
              offset = filters._2.offset,
              limit = filters._2.limit
            )
          } yield Ok(Json.toJson(paginatedReports))
      )
  }

  def extractReports = SecuredAction(WithPermission(UserPermission.listReports)).async { implicit request =>
    for {
      reportFilter <- ReportFilter
        .fromQueryString(request.queryString, request.identity.userRole)
        .toOption
        .liftTo[Future] {
          logger.warn(s"Failed to parse ReportFilter query params")
          throw MalformedQueryParams
        }
      _ = logger.debug(s"Parsing zone query param")
      zone <- (new QueryStringMapper(request.queryString))
        .timeZone("zone")
        // temporary retrocompat, so we can mep the API safely
        .orElse(Some(ZoneId.of("Europe/Paris")))
        .liftTo[Future] {
          logger.warn(s"Failed to parse zone query param")
          throw MalformedQueryParams
        }
      _ = logger.debug(s"Requesting report for user ${request.identity.email}")
      file <- asyncFileRepository
        .create(AsyncFile.build(request.identity, kind = AsyncFileKind.Reports))
      _ = reportsExtractActor ! ReportsExtractActor.ExtractRequest(file.id, request.identity, reportFilter, zone)
    } yield Ok
  }
}
