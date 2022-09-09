package repositories.emailvalidation

import models.EmailValidation
import models.EmailValidationFilter
import models.PaginatedResult
import models.PaginatedSearch
import play.api.Logger
import repositories.CRUDRepository
import repositories.PostgresProfile
import slick.jdbc.JdbcProfile
import utils.EmailAddress

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import PostgresProfile.api._
import slick.basic.DatabaseConfig

class EmailValidationRepository(
    override val dbConfig: DatabaseConfig[JdbcProfile]
)(implicit override val ec: ExecutionContext)
    extends CRUDRepository[EmailValidationTable, EmailValidation]
    with EmailValidationRepositoryInterface {

  val logger: Logger = Logger(this.getClass)
  override val table: TableQuery[EmailValidationTable] = EmailValidationTable.table
  import dbConfig._

  override def findByEmail(email: EmailAddress): Future[Option[EmailValidation]] =
    db.run(table.filter(_.email === email).result.headOption)

  override def validate(email: EmailAddress): Future[Option[EmailValidation]] = {
    val action = (for {
      _ <- table
        .filter(_.email === email)
        .map(_.lastValidationDate)
        .update(Some(OffsetDateTime.now()))
      updated <- table.filter(_.email === email).result.headOption
    } yield updated).transactionally
    db.run(action)
  }

  override def update(email: EmailValidation): Future[Int] =
    db.run(table.filter(_.email === email.email).update(email))

  override def exists(email: EmailAddress): Future[Boolean] =
    db.run(table.filter(_.email === email).result.headOption).map(_.isDefined)

  override def isValidated(email: EmailAddress): Future[Boolean] =
    db.run(
      table
        .filter(_.email === email)
        .filter(_.lastValidationDate.isDefined)
        .result
        .headOption
    ).map(_.isDefined)

  def search(search: EmailValidationFilter, paginate: PaginatedSearch): Future[PaginatedResult[EmailValidation]] =
    table
      .filterOpt(search.email)(_.email === _)
      .filterOpt(search.validated)(_.lastValidationDate.isDefined === _)
      .sortBy(_.creationDate.desc)
      .withPagination(db)(paginate.offset, paginate.limit)
}
