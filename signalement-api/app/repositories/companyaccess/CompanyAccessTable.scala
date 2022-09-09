package repositories.companyaccess

import models.AccessLevel
import models.UserAccess
import repositories.company.CompanyTable
import repositories.PostgresProfile.api._
import CompanyAccessColumnType._
import repositories.user.UserTable
import slick.lifted.TableQuery

import java.util.UUID
import java.time.OffsetDateTime

class CompanyAccessTable(tag: Tag) extends Table[UserAccess](tag, "company_accesses") {
  def companyId = column[UUID]("company_id")
  def userId = column[UUID]("user_id")
  def level = column[AccessLevel]("level")
  def updateDate = column[OffsetDateTime]("update_date")
  def creationDate = column[OffsetDateTime]("creation_date")
  def pk = primaryKey("pk_company_user", (companyId, userId))
  def * = (companyId, userId, level, updateDate, creationDate) <> (UserAccess.tupled, UserAccess.unapply)

  def company = foreignKey("COMPANY_FK", companyId, CompanyTable.table)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade
  )
  def user = foreignKey("USER_FK", userId, UserTable.table)(
    _.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade
  )
}

object CompanyAccessTable {
  val table = TableQuery[CompanyAccessTable]
}
