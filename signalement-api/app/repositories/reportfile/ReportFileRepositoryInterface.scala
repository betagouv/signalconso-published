package repositories.reportfile

import models.report.ReportFile
import models.report.reportfile.ReportFileId
import repositories.TypedCRUDRepositoryInterface

import java.util.UUID
import scala.concurrent.Future

trait ReportFileRepositoryInterface extends TypedCRUDRepositoryInterface[ReportFile, ReportFileId] {

  def attachFilesToReport(fileIds: List[ReportFileId], reportId: UUID): Future[Int]

  def retrieveReportFiles(reportId: UUID): Future[List[ReportFile]]

  def prefetchReportsFiles(reportsIds: List[UUID]): Future[Map[UUID, List[ReportFile]]]

  def setAvOutput(fileId: ReportFileId, output: String): Future[Int]

  def removeStorageFileName(fileId: ReportFileId): Future[Int]
}
