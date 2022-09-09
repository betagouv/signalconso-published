package orchestrators

import company.companydata.CompanyDataRepositoryInterface
import models.AccessLevel
import models.Company
import models.CompanyWithAccess
import models.User
import models.UserRole
import repositories.company.CompanyRepositoryInterface
import repositories.companyaccess.CompanyAccessRepositoryInterface
import utils.SIREN
import utils.SIRET

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class SiretsSirens(sirens: Seq[SIREN], sirets: Seq[SIRET]) {
  def toList() = sirens.map(_.value).concat(sirets.map(_.value)).distinct
}

class CompaniesVisibilityOrchestrator(
    companyDataRepo: CompanyDataRepositoryInterface,
    companyRepo: CompanyRepositoryInterface,
    companyAccessRepository: CompanyAccessRepositoryInterface
)(implicit val executionContext: ExecutionContext) {

  def fetchAdminsWithHeadOffice(siret: SIRET): Future[List[User]] =
    for {
//      companies <- companyRepo.findCompanyAndHeadOffice(siret)
      companiesDataIncludingHeadOffice <- companyDataRepo.searchBySiretIncludingHeadOffice(siret)
      companies <- companyRepo.findBySirets(companiesDataIncludingHeadOffice.map(_.siret))
      admins <- companyAccessRepository.fetchUsersByCompanies(companies.map(_.id))
    } yield admins

  def fetchAdminsWithHeadOffices(companies: List[(SIRET, UUID)]): Future[Map[UUID, List[User]]] =
    for {
      adminsByCompanyIdMap <- companyAccessRepository.fetchUsersByCompanyId(companies.map(_._2))
      headOfficesCompany <- companyDataRepo
        .searchHeadOfficeBySiren(companies.map(c => SIREN(c._1)), includeClosed = true)
        .map(_.map(_._1))
        .flatMap { companyDatas =>
          companyRepo.findBySirets(companyDatas.map(_.siret))
        }
      headOfficeAdminsMap <- companyAccessRepository.fetchUsersByCompanyId(headOfficesCompany.map(_.id))
      headOfficeIdByCompanyIdMap: Map[UUID, Option[UUID]] = companies
        .groupBy(_._2)
        .view
        .mapValues { uniqueSiretCompanyIdTuple =>
          val siren = uniqueSiretCompanyIdTuple.headOption.map(x => SIREN(x._1))
          headOfficesCompany.find(c => siren.contains(SIREN(c.siret))).map(_.id)
        }
        .toMap
    } yield adminsByCompanyIdMap.map { x =>
      val headOfficeId = headOfficeIdByCompanyIdMap.get(x._1).flatten
      val headOfficeAdmins = headOfficeId.flatMap(headOfficeAdminsMap.get).getOrElse(List())
      (x._1, (x._2 ++ headOfficeAdmins).distinctBy(_.id))
    }

  def fetchVisibleCompanies(pro: User): Future[List[CompanyWithAccess]] =
    for {
      authorizedCompanies <- companyAccessRepository.fetchCompaniesWithLevel(pro)
      headOfficeSirets <- companyDataRepo
        .filterHeadOffices(authorizedCompanies.map(_.company.siret))
        .map(_.map(_.siret))
      companiesForHeadOffices <- companyRepo.findBySiren(headOfficeSirets.map(SIREN.apply))
      companiesForHeadOfficesWithAccesses = addAccessToSubsidiaries(authorizedCompanies, companiesForHeadOffices)
      accessiblesCompanies = (authorizedCompanies ++ companiesForHeadOfficesWithAccesses)
        .distinctBy(_.company.siret)
        .sortBy(_.company.siret.value)
    } yield accessiblesCompanies

  private[this] def addAccessToSubsidiaries(
      authorizedCompaniesWithAccesses: List[CompanyWithAccess],
      accessibleSubsidiaries: List[Company]
  ) = {
    val levelPriority = Map(
      AccessLevel.ADMIN -> 1,
      AccessLevel.MEMBER -> 0
    ).withDefaultValue(-1)
    val getLevelBySiren = authorizedCompaniesWithAccesses
      .groupMapReduce(c => SIREN(c.company.siret))(_.level)((a, b) => if (levelPriority(a) > levelPriority(b)) a else b)
      .withDefaultValue(AccessLevel.NONE)
    accessibleSubsidiaries.map(c => CompanyWithAccess(c, getLevelBySiren(SIREN(c.siret))))
  }

  private[this] def fetchVisibleSiretsSirens(user: User): Future[SiretsSirens] =
    for {
      authorizedSirets <- companyAccessRepository.fetchCompaniesWithLevel(user).map(_.map(_.company.siret))
      authorizedHeadofficeSirens <- companyDataRepo
        .searchBySirets(authorizedSirets, includeClosed = true)
        .map(companies =>
          companies
            .map(_._1)
            .filter(_.etablissementSiege.contains("true"))
            .map(_.siren)
        )
    } yield removeRedundantSirets(SiretsSirens(authorizedHeadofficeSirens, authorizedSirets))

  def filterUnauthorizedSiretSirenList(siretSirenList: Seq[String], user: User): Future[Seq[String]] =
    if (user.userRole == UserRole.Professionnel) {
      val formattedSiretsSirens = formatSiretSirenList(siretSirenList)
      fetchVisibleSiretsSirens(user).map { allowed =>
        val filteredSiretsSirens = SiretsSirens(
          sirets = formattedSiretsSirens.sirets.filter(wanted =>
            allowed.sirens.contains(SIREN(wanted)) || allowed.sirets.contains(wanted)
          ),
          sirens = allowed.sirens.intersect(formattedSiretsSirens.sirens)
        ).toList()
        if (filteredSiretsSirens.isEmpty) {
          allowed.toList()
        } else {
          filteredSiretsSirens
        }
      }
    } else {
      Future(siretSirenList)
    }

  private[this] def removeRedundantSirets(id: SiretsSirens): SiretsSirens =
    SiretsSirens(
      id.sirens,
      id.sirets.filter(siret => !id.sirens.contains(SIREN(siret)))
    )

  private[this] def formatSiretSirenList(siretSirenList: Seq[String]): SiretsSirens =
    SiretsSirens(
      sirens = siretSirenList.filter(SIREN.isValid).map(SIREN.apply),
      sirets = siretSirenList.filter(SIRET.isValid).map(SIRET.apply)
    )
}
