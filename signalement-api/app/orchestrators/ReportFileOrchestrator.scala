package orchestrators

import actors.AntivirusScanActor
import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import cats.implicits.catsSyntaxMonadError
import cats.implicits.catsSyntaxOption
import cats.implicits.toTraverseOps
import controllers.error.AppError._
import models._
import models.report._
import models.report.reportfile.ReportFileId
import play.api.Logger
import repositories.reportfile.ReportFileRepositoryInterface
import services.S3ServiceInterface

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ReportFileOrchestrator(
    reportFileRepository: ReportFileRepositoryInterface,
    antivirusScanActor: ActorRef[AntivirusScanActor.ScanCommand],
    s3Service: S3ServiceInterface
)(implicit val executionContext: ExecutionContext, mat: Materializer) {
  val logger = Logger(this.getClass)

  def prefetchReportsFiles(reportsIds: List[UUID]): Future[Map[UUID, List[ReportFile]]] =
    reportFileRepository.prefetchReportsFiles(reportsIds)

  def attachFilesToReport(fileIds: List[ReportFileId], reportId: UUID): Future[List[ReportFile]] = for {
    _ <- reportFileRepository.attachFilesToReport(fileIds, reportId)
    files <- reportFileRepository.retrieveReportFiles(reportId)
  } yield files

  def saveReportFile(filename: String, file: java.io.File, origin: ReportFileOrigin): Future[ReportFile] =
    for {
      reportFile <- reportFileRepository.create(
        ReportFile(
          ReportFileId.generateId(),
          reportId = None,
          creationDate = OffsetDateTime.now(),
          filename = filename,
          storageFilename = file.getName(),
          origin = origin,
          avOutput = None
        )
      )
      _ <- FileIO
        .fromPath(file.toPath)
        .to(s3Service.upload(reportFile.storageFilename))
        .run()
      _ = logger.debug(s"Uploaded file ${reportFile.id} to S3")
    } yield {
      antivirusScanActor ! AntivirusScanActor.ScanFromFile(reportFile, file)
      reportFile
    }

  def removeFromReportId(reportId: UUID): Future[List[Int]] =
    for {
      reportFilesToDelete <- reportFileRepository.retrieveReportFiles(reportId)
      res <- reportFilesToDelete.map(file => remove(file.id, file.filename)).sequence
    } yield res

  def removeReportFile(fileId: ReportFileId, filename: String, user: Option[User]): Future[Int] =
    for {
      maybeReportFile <- reportFileRepository
        .get(fileId)
        .ensure(AttachmentNotFound(reportFileId = fileId, reportFileName = filename))(predicate =
          _.exists(_.filename == filename)
        )
      reportFile <- maybeReportFile.liftTo[Future](AttachmentNotFound(fileId, filename))
      userHasDeleteFilePermission = user.map(_.userRole.permissions).exists(_.contains(UserPermission.deleteFile))
      _ <- reportFile.reportId match {
        case Some(_) if userHasDeleteFilePermission => reportFileRepository.delete(fileId)
        case Some(_) =>
          logger.warn(s"Cannot delete file $fileId because user ${user.map(_.id)} is missing delete file permission")
          Future.failed(CantPerformAction)
        case None => reportFileRepository.delete(fileId)
      }
      res <- remove(fileId, filename)
    } yield res

  private def remove(fileId: ReportFileId, filename: String): Future[Int] = for {
    res <- reportFileRepository.delete(fileId)
    _ <- s3Service.delete(filename)
  } yield res

  def downloadReportAttachment(reportFileId: ReportFileId, filename: String): Future[String] = {
    logger.info(s"Downloading file with id $reportFileId")
    reportFileRepository
      .get(reportFileId)
      .flatMap {
        case Some(reportFile) if reportFile.filename == filename && reportFile.avOutput.isEmpty =>
          logger.info("Attachment has not been scan by antivirus, rescheduling scan")
          antivirusScanActor ! AntivirusScanActor.ScanFromBucket(reportFile)
          Future.failed(AttachmentNotReady(reportFileId))
        case Some(file) if file.filename == filename && file.avOutput.isDefined =>
          Future.successful(s3Service.getSignedUrl(file.storageFilename))
        case _ => Future.failed(AttachmentNotFound(reportFileId, filename))
      }
  }
}
