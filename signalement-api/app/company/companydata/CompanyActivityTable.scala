package company.companydata

import company.CompanyActivity
import repositories.PostgresProfile.api._

class CompanyActivityTable(tag: Tag) extends Table[CompanyActivity](tag, "activites") {
  def code = column[String]("code")

  def libelle = column[String]("libelle")

  def * = (code, libelle) <> (CompanyActivity.tupled, CompanyActivity.unapply)
}

object CompanyActivityTable {
  val table = TableQuery[CompanyActivityTable]
}
