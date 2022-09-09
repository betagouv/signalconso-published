package company

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import models.UserRole
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithRole

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class EnterpriseImportController(
    enterpriseSyncOrchestrator: EnterpriseImportOrchestrator,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  implicit val timeout: akka.util.Timeout = 5.seconds

  def startEtablissementFile = SecuredAction(WithRole(UserRole.Admin)) { _ =>
    enterpriseSyncOrchestrator.startEtablissementFile
    Ok
  }

  def startUniteLegaleFile = SecuredAction(WithRole(UserRole.Admin)) { _ =>
    enterpriseSyncOrchestrator.startUniteLegaleFile
    Ok
  }

  def cancelAllFiles = SecuredAction(WithRole(UserRole.Admin)) { _ =>
    enterpriseSyncOrchestrator.cancelUniteLegaleFile
    enterpriseSyncOrchestrator.cancelEntrepriseFile
    Ok
  }

  def cancelEtablissementFile = SecuredAction(WithRole(UserRole.Admin)) { _ =>
    enterpriseSyncOrchestrator.cancelEntrepriseFile
    Ok
  }

  def cancelUniteLegaleFile = SecuredAction(WithRole(UserRole.Admin)) { _ =>
    enterpriseSyncOrchestrator.cancelUniteLegaleFile
    Ok
  }

  def getSyncInfo = SecuredAction(WithRole(UserRole.Admin)).async { _ =>
    for {
      etablissementImportInfo <- enterpriseSyncOrchestrator.getLastEtablissementImportInfo()
      uniteLegaleInfo <- enterpriseSyncOrchestrator.getUniteLegaleImportInfo()
    } yield Ok(
      Json.obj(
        "etablissementImportInfo" -> etablissementImportInfo,
        "uniteLegaleInfo" -> uniteLegaleInfo
      )
    )
  }
}
