package company.companydata

import company.CompanyActivity
import company.CompanyData
import repositories.CRUDRepositoryInterface
import repositories.PostgresProfile
import utils.SIREN
import utils.SIRET

import scala.concurrent.Future

trait CompanyDataRepositoryInterface extends CRUDRepositoryInterface[CompanyData] {

  def insertAll(companies: Map[String, Option[String]]): PostgresProfile.api.DBIO[Int]

  def updateName(name: (SIREN, String)): PostgresProfile.api.DBIO[Int]

  def search(q: String, postalCode: String): Future[List[(CompanyData, Option[CompanyActivity])]]

  def searchBySirets(
      sirets: List[SIRET],
      includeClosed: Boolean = false
  ): Future[List[(CompanyData, Option[CompanyActivity])]]

  def searchBySiret(
      siret: SIRET,
      includeClosed: Boolean = false
  ): Future[List[(CompanyData, Option[CompanyActivity])]]

  def filterHeadOffices(sirets: List[SIRET]): Future[List[CompanyData]]

  def getHeadOffice(siret: SIRET): Future[List[CompanyData]]

  def searchBySiretIncludingHeadOfficeWithActivity(siret: SIRET): Future[List[(CompanyData, Option[CompanyActivity])]]

  def searchBySiretIncludingHeadOffice(siret: SIRET): Future[List[CompanyData]]

  def searchBySirens(
      sirens: List[SIREN],
      includeClosed: Boolean = false
  ): Future[List[(CompanyData, Option[CompanyActivity])]]

  def searchBySiren(
      siren: SIREN
  ): Future[List[(CompanyData, Option[CompanyActivity])]]

  def searchHeadOfficeBySiren(siren: SIREN): Future[Option[(CompanyData, Option[CompanyActivity])]]

  def searchHeadOfficeBySiren(
      sirens: List[SIREN],
      includeClosed: Boolean = false
  ): Future[List[(CompanyData, Option[CompanyActivity])]]

}
