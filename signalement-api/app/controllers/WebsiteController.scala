package controllers

import actors.WebsitesExtractActor
import actors.WebsitesExtractActor.RawFilters
import akka.actor.ActorRef
import akka.pattern.ask
import com.mohiva.play.silhouette.api.Silhouette
import models.PaginatedResult.paginatedResultWrites
import models._
import models.investigation.DepartmentDivision
import models.investigation.InvestigationStatus
import models.investigation.Practice
import models.investigation.WebsiteInvestigationApi
import models.website._
import orchestrators.WebsitesOrchestrator
import play.api.Logger
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import repositories.company.CompanyRepositoryInterface
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithRole

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

class WebsiteController(
    val websitesOrchestrator: WebsitesOrchestrator,
    val companyRepository: CompanyRepositoryInterface,
    websitesExtractActor: ActorRef,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  implicit val timeout: akka.util.Timeout = 5.seconds
  val logger: Logger = Logger(this.getClass)

  def fetchWithCompanies(
      maybeHost: Option[String],
      maybeIdentificationStatus: Option[Seq[IdentificationStatus]],
      maybeOffset: Option[Long],
      maybeLimit: Option[Int],
      investigationStatus: Option[Seq[InvestigationStatus]],
      practice: Option[Seq[Practice]],
      attribution: Option[Seq[DepartmentDivision]],
      start: Option[OffsetDateTime],
      end: Option[OffsetDateTime],
      hasAssociation: Option[Boolean]
  ) =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { _ =>
      for {
        result <-
          websitesOrchestrator.getWebsiteCompanyCount(
            maybeHost.filter(_.nonEmpty),
            maybeIdentificationStatus.filter(_.nonEmpty),
            maybeOffset,
            maybeLimit,
            investigationStatus.filter(_.nonEmpty),
            practice.filter(_.nonEmpty),
            attribution.filter(_.nonEmpty),
            start,
            end,
            hasAssociation
          )
        resultAsJson = Json.toJson(result)(paginatedResultWrites[WebsiteCompanyReportCount])
      } yield Ok(resultAsJson)
    }

  def fetchUnregisteredHost(host: Option[String], start: Option[String], end: Option[String]) =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { _ =>
      websitesOrchestrator
        .fetchUnregisteredHost(host, start, end)
        .map(websiteHostCount => Ok(Json.toJson(websiteHostCount)))
    }

  def extractUnregisteredHost(q: Option[String], start: Option[String], end: Option[String]) =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { implicit request =>
      logger.debug(s"Requesting websites for user ${request.identity.email}")
      websitesExtractActor ? WebsitesExtractActor.ExtractRequest(
        request.identity,
        RawFilters(q.filter(_.nonEmpty), start, end)
      )
      Future.successful(Ok)
    }

  def searchByHost(url: String) = UnsecuredAction.async {
    websitesOrchestrator
      .searchByHost(url)
      .map(countries => Ok(Json.toJson(countries)))
  }

  def updateWebsiteIdentificationStatus(websiteId: WebsiteId, identificationStatus: IdentificationStatus) =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { _ =>
      websitesOrchestrator
        .updateWebsiteIdentificationStatus(websiteId, identificationStatus)
        .map(website => Ok(Json.toJson(website)))
    }

  def updateCompany(websiteId: WebsiteId) = SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async(parse.json) {
    implicit request =>
      request.body
        .validate[CompanyCreation]
        .fold(
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          company =>
            websitesOrchestrator
              .updateCompany(websiteId, company, request.identity)
              .map(websiteAndCompany => Ok(Json.toJson(websiteAndCompany)))
        )
  }

  def updateCompanyCountry(websiteId: WebsiteId, companyCountry: String) =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async { request =>
      websitesOrchestrator
        .updateCompanyCountry(websiteId, companyCountry, request.identity)
        .map(websiteAndCompany => Ok(Json.toJson(websiteAndCompany)))

    }

  def remove(websiteId: WebsiteId) = SecuredAction(WithRole(UserRole.Admin)).async { _ =>
    websitesOrchestrator
      .delete(websiteId)
      .map(_ => Ok)
  }

  def updateInvestigation() = SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)).async(parse.json) {
    implicit request =>
      for {
        websiteInvestigationApi <- request.parseBody[WebsiteInvestigationApi]()
        updated <- websitesOrchestrator.updateInvestigation(websiteInvestigationApi)
        _ = logger.debug(updated.toString)
      } yield Ok(Json.toJson(updated))
  }

  def listDepartmentDivision(): Action[AnyContent] =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)) { _ =>
      Ok(Json.toJson(websitesOrchestrator.listDepartmentDivision()))
    }

  def listInvestigationStatus(): Action[AnyContent] =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)) { _ =>
      Ok(Json.toJson(websitesOrchestrator.listInvestigationStatus()))
    }

  def listPractice(): Action[AnyContent] =
    SecuredAction(WithRole(UserRole.Admin, UserRole.DGCCRF)) { _ =>
      Ok(Json.toJson(websitesOrchestrator.listPractice()))
    }

}
