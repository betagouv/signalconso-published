package repositories.reportfile

import models.report._
import models.report.reportfile.ReportFileId
import repositories.TypedCRUDRepository
import repositories.PostgresProfile.api._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import repositories.reportfile.ReportFileColumnType._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ReportFileRepository(override val dbConfig: DatabaseConfig[JdbcProfile])(implicit
    override val ec: ExecutionContext
) extends TypedCRUDRepository[ReportFileTable, ReportFile, ReportFileId]
    with ReportFileRepositoryInterface {

  override val table: TableQuery[ReportFileTable] = ReportFileTable.table
  import dbConfig._

  override def attachFilesToReport(fileIds: List[ReportFileId], reportId: UUID): Future[Int] = {
    val queryFile =
      for (refFile <- table.filter(_.id.inSet(fileIds)))
        yield refFile.reportId
    db.run(queryFile.update(Some(reportId)))
  }

  override def retrieveReportFiles(reportId: UUID): Future[List[ReportFile]] = db
    .run(
      table
        .filter(_.reportId === reportId)
        .to[List]
        .result
    )

  override def prefetchReportsFiles(reportsIds: List[UUID]): Future[Map[UUID, List[ReportFile]]] =
    db.run(
      table
        .filter(
          _.reportId inSetBind reportsIds
        )
        .to[List]
        .result
    ).map(events => events.groupBy(_.reportId.get))

  override def setAvOutput(fileId: ReportFileId, output: String): Future[Int] = db
    .run(
      table
        .filter(_.id === fileId)
        .map(_.avOutput)
        .update(Some(output))
    )

  override def removeStorageFileName(fileId: ReportFileId): Future[Int] = db
    .run(
      table
        .filter(_.id === fileId)
        .map(_.storageFilename)
        .update("")
    )

}
