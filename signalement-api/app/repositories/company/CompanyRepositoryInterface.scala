package repositories.company

import models.Company
import models.CompanyRegisteredSearch
import models.PaginatedResult
import models.PaginatedSearch
import models.UserRole
import repositories.CRUDRepositoryInterface
import utils.SIREN
import utils.SIRET

import java.util.UUID
import scala.concurrent.Future
trait CompanyRepositoryInterface extends CRUDRepositoryInterface[Company] {

  def searchWithReportsCount(
      search: CompanyRegisteredSearch,
      paginate: PaginatedSearch,
      userRole: UserRole
  ): Future[PaginatedResult[(Company, Int, Int)]]

  def getOrCreate(siret: SIRET, data: Company): Future[Company]

  def fetchCompanies(companyIds: List[UUID]): Future[List[Company]]

  def findBySiret(siret: SIRET): Future[Option[Company]]

  def findCompanyAndHeadOffice(siret: SIRET): Future[List[Company]]

  def findBySirets(sirets: List[SIRET]): Future[List[Company]]

  def findByName(name: String): Future[List[Company]]

  def findBySiren(siren: List[SIREN]): Future[List[Company]]

  def updateBySiret(siret: SIRET, isOpen: Boolean, isHeadOffice: Boolean): Future[SIRET]
}
