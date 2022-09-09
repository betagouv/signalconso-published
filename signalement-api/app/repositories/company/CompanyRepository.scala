package repositories.company

import models.SearchCompanyIdentity.SearchCompanyIdentityId
import models.SearchCompanyIdentity.SearchCompanyIdentityName
import models.SearchCompanyIdentity.SearchCompanyIdentityRCS
import models.SearchCompanyIdentity.SearchCompanyIdentitySiren
import models.SearchCompanyIdentity.SearchCompanyIdentitySiret
import models._
import models.report.ReportStatus.statusWithProResponse
import repositories.PostgresProfile.api._
import repositories.companyaccess.CompanyAccessTable
import repositories.report.ReportTable
import repositories.user.UserTable
import slick.jdbc.JdbcProfile
import utils.EmailAddress
import utils.SIREN
import utils.SIRET
import repositories.CRUDRepository
import slick.basic.DatabaseConfig

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CompanyRepository(override val dbConfig: DatabaseConfig[JdbcProfile])(implicit
    override val ec: ExecutionContext
) extends CRUDRepository[CompanyTable, Company]
    with CompanyRepositoryInterface {

  override val table: TableQuery[CompanyTable] = CompanyTable.table
  import dbConfig._

  override def searchWithReportsCount(
      search: CompanyRegisteredSearch,
      paginate: PaginatedSearch,
      userRole: UserRole
  ): Future[PaginatedResult[(Company, Int, Int)]] = {
    def companyIdByEmailTable(emailWithAccess: EmailAddress) = CompanyAccessTable.table
      .join(UserTable.table)
      .on(_.userId === _.id)
      .filter(_._2.email === emailWithAccess)
      .map(_._1.companyId)

    val query = table
      .joinLeft(ReportTable.table(userRole))
      .on(_.id === _.companyId)
      .filterIf(search.departments.nonEmpty) { case (company, _) =>
        company.department.map(a => a.inSet(search.departments)).getOrElse(false)
      }
      .filterIf(search.activityCodes.nonEmpty) { case (company, _) =>
        company.activityCode.map(a => a.inSet(search.activityCodes)).getOrElse(false)
      }
      .groupBy(_._1)
      .map { case (grouped, all) =>
        (
          grouped,
          all.map(_._2).map(_.map(_.id)).countDefined,
          /* Response rate
           * Equivalent to following select clause
           * count((case when (status in ('Promesse action','Signalement infondé','Signalement mal attribué') then id end))
           */
          all
            .map(_._2)
            .map(b =>
              b.flatMap { a =>
                Case If a.status.inSet(
                  statusWithProResponse.map(_.entryName)
                ) Then a.id
              }
            )
            .countDefined: Rep[Int]
        )
      }
      .sortBy(_._2.desc)
    search.identity
      .map {
        case SearchCompanyIdentityRCS(q)   => query.filter(_._1.id.asColumnOf[String] like s"%${q}%")
        case SearchCompanyIdentitySiret(q) => query.filter(_._1.siret === SIRET.fromUnsafe(q))
        case SearchCompanyIdentitySiren(q) => query.filter(_._1.siret.asColumnOf[String] like s"${q}_____")
        case SearchCompanyIdentityName(q)  => query.filter(_._1.name.toLowerCase like s"%${q.toLowerCase}%")
        case id: SearchCompanyIdentityId   => query.filter(_._1.id === id.value)
      }
      .getOrElse(query)
      .filterOpt(search.emailsWithAccess) { case (table, email) =>
        table._1.id.in(companyIdByEmailTable(EmailAddress(email)))
      }
      .withPagination(db)(maybeOffset = paginate.offset, maybeLimit = paginate.limit)
  }

  override def getOrCreate(siret: SIRET, data: Company): Future[Company] =
    db.run(table.filter(_.siret === siret).result.headOption)
      .flatMap(
        _.map(Future(_)).getOrElse(db.run(table returning table += data))
      )

  override def fetchCompanies(companyIds: List[UUID]): Future[List[Company]] =
    db.run(table.filter(_.id inSetBind companyIds).to[List].result)

  override def findBySiret(siret: SIRET): Future[Option[Company]] =
    db.run(table.filter(_.siret === siret).result.headOption)

  def findCompanyAndHeadOffice(siret: SIRET): Future[List[Company]] =
    db.run(
      table
        .filter(_.siret.asColumnOf[String] like s"${SIREN(siret).value}%")
        .filter { companyTable =>
          val companyWithSameSiret: Rep[Boolean] = companyTable.siret === siret
          val companyHeadOffice: Rep[Boolean] = companyTable.isHeadOffice
          companyWithSameSiret || companyHeadOffice
        }
        .filter(_.isOpen)
        .result
        .map(_.toList)
    )

  override def findBySirets(sirets: List[SIRET]): Future[List[Company]] =
    db.run(table.filter(_.siret inSet sirets).to[List].result)

  override def findByName(name: String): Future[List[Company]] =
    db.run(table.filter(_.name.toLowerCase like s"%${name.toLowerCase}%").to[List].result)

  override def findBySiren(siren: List[SIREN]): Future[List[Company]] =
    db.run(
      table
        .filter(x => SubstrSQLFunction(x.siret.asColumnOf[String], 0.bind, 10.bind) inSetBind siren.map(_.value))
        .to[List]
        .result
    )

  override def updateBySiret(siret: SIRET, isOpen: Boolean, isHeadOffice: Boolean): Future[SIRET] = db
    .run(
      table
        .filter(_.siret === siret)
        .map(c => (c.isHeadOffice, c.isOpen))
        .update((isHeadOffice, isOpen))
    )
    .map(_ => siret)

}
