package controllers

import actors.ReportedPhonesExtractActor
import actors.ReportedPhonesExtractActor.RawFilters
import akka.actor.ActorRef
import com.mohiva.play.silhouette.api.Silhouette
import models._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import repositories.asyncfiles.AsyncFileRepositoryInterface
import repositories.company.CompanyRepositoryInterface
import repositories.report.ReportRepositoryInterface
import utils.DateUtils
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithRole

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ReportedPhoneController(
    val reportRepository: ReportRepositoryInterface,
    val companyRepository: CompanyRepositoryInterface,
    asyncFileRepository: AsyncFileRepositoryInterface,
    reportedPhonesExtractActor: ActorRef,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  implicit val timeout: akka.util.Timeout = 5.seconds
  val logger: Logger = Logger(this.getClass)

  def fetchGrouped(q: Option[String], start: Option[String], end: Option[String]) =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { _ =>
      reportRepository
        .getPhoneReports(DateUtils.parseDate(start), DateUtils.parseDate(end))
        .map(reports =>
          Ok(
            Json.toJson(
              reports
                .groupBy(report => (report.phone, report.companySiret, report.companyName, report.category))
                .collect {
                  case ((Some(phone), siretOpt, companyNameOpt, category), reports)
                      if q.map(phone.contains(_)).getOrElse(true) =>
                    ((phone, siretOpt, companyNameOpt, category), reports.length)
                }
                .map { case ((phone, siretOpt, companyNameOpt, category), count) =>
                  Json.obj(
                    "phone" -> phone,
                    "siret" -> siretOpt,
                    "companyName" -> companyNameOpt,
                    "category" -> category,
                    "count" -> count
                  )
                }
            )
          )
        )
    }

  def extractPhonesGroupBySIRET(q: Option[String], start: Option[String], end: Option[String]) =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { implicit request =>
      logger.debug(s"Requesting reportedPhones for user ${request.identity.email}")
      asyncFileRepository
        .create(AsyncFile.build(request.identity, kind = AsyncFileKind.ReportedPhones))
        .map { file =>
          reportedPhonesExtractActor ! ReportedPhonesExtractActor
            .ExtractRequest(file.id, request.identity, RawFilters(q, start, end))
        }
        .map(_ => Ok)
    }
}
