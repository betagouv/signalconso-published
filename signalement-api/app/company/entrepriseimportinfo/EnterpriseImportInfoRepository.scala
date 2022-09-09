package company.entrepriseimportinfo

import company.EnterpriseImportInfo
import repositories.PostgresProfile
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class EnterpriseImportInfoRepository(val dbConfig: DatabaseConfig[JdbcProfile])(implicit
    ec: ExecutionContext
) {

  import PostgresProfile.api._
  import dbConfig._

  class EnterpriseSyncInfoTable(tag: Tag) extends Table[EnterpriseImportInfo](tag, "etablissements_import_info") {
    def id = column[UUID]("id", O.PrimaryKey)
    def fileName = column[String]("file_name")
    def fileUrl = column[String]("file_url")
    def linesCount = column[Double]("lines_count")
    def linesDone = column[Double]("lines_done")
    def startedAt = column[OffsetDateTime]("started_at")
    def endedAt = column[Option[OffsetDateTime]]("ended_at")
    def errors = column[Option[String]]("errors")

    def * = (
      id,
      fileName,
      fileUrl,
      linesCount,
      linesDone,
      startedAt,
      endedAt,
      errors
    ) <> ((EnterpriseImportInfo.apply _).tupled, EnterpriseImportInfo.unapply)
  }

  val EnterpriseSyncInfotableQuery = TableQuery[EnterpriseSyncInfoTable]

  def create(info: EnterpriseImportInfo): Future[EnterpriseImportInfo] =
    db.run(EnterpriseSyncInfotableQuery += info).map(_ => info)

  def byId(id: UUID) = EnterpriseSyncInfotableQuery.filter(_.id === id)

  def updateLinesDone(id: UUID, linesDone: Double): Future[Int] =
    db.run(byId(id).map(_.linesDone).update(linesDone))

  def updateEndedAt(id: UUID, endAt: OffsetDateTime = OffsetDateTime.now()): Future[Int] =
    db.run(byId(id).map(_.endedAt).update(Some(endAt)))

  def updateError(id: UUID, error: String): Future[Int] =
    db.run(byId(id).map(_.errors).update(Some(error)))

  def updateAllEndedAt(name: String, endAt: OffsetDateTime = OffsetDateTime.now()): Future[Int] =
    db.run(
      EnterpriseSyncInfotableQuery
        .filter(_.endedAt.isEmpty)
        .filter(_.fileName === name)
        .map(_.endedAt)
        .update(Some(endAt))
    )

  def updateAllError(name: String, error: String): Future[Int] =
    db.run(
      EnterpriseSyncInfotableQuery
        .filter(_.endedAt.isEmpty)
        .filter(_.fileName === name)
        .map(_.errors)
        .update(Some(error))
    )

  def findRunning(name: String): Future[Option[EnterpriseImportInfo]] =
    db.run(
      EnterpriseSyncInfotableQuery
        .filter(_.fileName === name)
        .filter(_.endedAt.isEmpty)
        .sortBy(_.startedAt.desc)
        .result
        .headOption
    )

  def findLastEnded(name: String): Future[Option[EnterpriseImportInfo]] =
    db.run(
      EnterpriseSyncInfotableQuery
        .filter(_.fileName === name)
        .filter(_.endedAt.isDefined)
        .sortBy(_.startedAt.desc)
        .result
        .headOption
    )

  def findLast(name: String): Future[Option[EnterpriseImportInfo]] =
    db.run(
      EnterpriseSyncInfotableQuery
        .filter(_.fileName === name)
        .sortBy(_.startedAt.desc)
        .result
        .headOption
    )
}
