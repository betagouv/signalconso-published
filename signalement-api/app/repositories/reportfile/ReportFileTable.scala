package repositories.reportfile

import models.report.ReportFile
import models.report.ReportFileOrigin
import repositories.PostgresProfile.api._
import repositories.report.ReportTable
import ReportFileColumnType._
import models.report.reportfile.ReportFileId
import repositories.TypedDatabaseTable

import java.time.OffsetDateTime
import java.util.UUID

class ReportFileTable(tag: Tag) extends TypedDatabaseTable[ReportFile, ReportFileId](tag, "report_files") {

  def reportId = column[Option[UUID]]("report_id")
  def creationDate = column[OffsetDateTime]("creation_date")
  def filename = column[String]("filename")
  def storageFilename = column[String]("storage_filename")
  def origin = column[ReportFileOrigin]("origin")
  def avOutput = column[Option[String]]("av_output")
  def report = foreignKey("report_files_fk", reportId, ReportTable.table)(_.id.?)

  type FileData = (ReportFileId, Option[UUID], OffsetDateTime, String, String, ReportFileOrigin, Option[String])

  def constructFile: FileData => ReportFile = {
    case (id, reportId, creationDate, filename, storageFilename, origin, avOutput) =>
      ReportFile(id, reportId, creationDate, filename, storageFilename, origin, avOutput)
  }

  def extractFile: PartialFunction[ReportFile, FileData] = {
    case ReportFile(id, reportId, creationDate, filename, storageFilename, origin, avOutput) =>
      (id, reportId, creationDate, filename, storageFilename, origin, avOutput)
  }

  def * =
    (id, reportId, creationDate, filename, storageFilename, origin, avOutput) <> (constructFile, extractFile.lift)
}

object ReportFileTable {
  val table = TableQuery[ReportFileTable]
}
