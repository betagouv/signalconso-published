package controllers

import akka.Done
import cats.implicits.catsSyntaxOption
import com.mohiva.play.silhouette.api.Silhouette
import config.SignalConsoConfiguration
import controllers.error.AppError.InvalidFileExtension
import controllers.error.AppError.MalformedFileKey
import models.report._
import models.report.reportfile.ReportFileId
import orchestrators.ReportFileOrchestrator
import play.api.Logger
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.MultipartFormData
import utils.silhouette.auth.AuthEnv

import java.io.File
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ReportFileController(
    reportFileOrchestrator: ReportFileOrchestrator,
    val silhouette: Silhouette[AuthEnv],
    signalConsoConfiguration: SignalConsoConfiguration,
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  def downloadReportFile(uuid: ReportFileId, filename: String): Action[AnyContent] = UnsecuredAction.async { _ =>
    reportFileOrchestrator
      .downloadReportAttachment(uuid, filename)
      .map(signedUrl => Redirect(signedUrl))

  }

  def deleteReportFile(uuid: ReportFileId, filename: String): Action[AnyContent] = UserAwareAction.async {
    implicit request =>
      reportFileOrchestrator
        .removeReportFile(uuid, filename, request.identity)
        .map(_ => NoContent)
  }

  def uploadReportFile: Action[MultipartFormData[Files.TemporaryFile]] =
    UnsecuredAction.async(parse.multipartFormData) { request =>
      for {
        filePart <- request.body.file("reportFile").liftTo[Future](MalformedFileKey("reportFile"))
        dataPart = request.body.dataParts
          .get("reportFileOrigin")
          .map(o => ReportFileOrigin(o.head))
          .getOrElse(ReportFileOrigin.CONSUMER)
        fileExtension = filePart.filename.toLowerCase.split("\\.").last
        _ <- validateFileExtension(fileExtension)
        tmpFile = pathFromFilePart(filePart)
        reportFile <- reportFileOrchestrator
          .saveReportFile(
            filePart.filename,
            tmpFile,
            dataPart
          )
      } yield Ok(Json.toJson(reportFile))
    }

  private def pathFromFilePart(filePart: MultipartFormData.FilePart[Files.TemporaryFile]): File = {
    val filename = Paths.get(filePart.filename).getFileName
    val tmpFile =
      new java.io.File(s"${signalConsoConfiguration.tmpDirectory}/${UUID.randomUUID}_${filename}")
    filePart.ref.copyTo(tmpFile)
    tmpFile
  }

  private def validateFileExtension(fileExtension: String): Future[Done.type] = {
    val allowedExtensions: Seq[String] = signalConsoConfiguration.upload.allowedExtensions
    if (allowedExtensions.contains(fileExtension)) Future.successful(Done)
    else Future.failed(InvalidFileExtension(fileExtension, allowedExtensions))
  }

}
