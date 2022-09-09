package repositories.companyaccess

import models._
import repositories.PostgresProfile.api._
import repositories.company.CompanyTable
import repositories.companyaccess.CompanyAccessColumnType._
import repositories.computeTickValues
import repositories.user.UserTable
import slick.basic.DatabaseConfig
import slick.dbio.Effect
import slick.jdbc.JdbcProfile
import slick.sql.FixedSqlAction

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CompanyAccessRepository(val dbConfig: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
    extends CompanyAccessRepositoryInterface {

  val table: TableQuery[CompanyAccessTable] = CompanyAccessTable.table
  import dbConfig._

  override def getUserLevel(companyId: UUID, user: User): Future[AccessLevel] =
    db.run(
      table
        .filter(_.companyId === companyId)
        .filter(_.userId === user.id)
        .map(_.level)
        .result
        .headOption
    ).map(_.getOrElse(AccessLevel.NONE))

  override def fetchCompaniesWithLevel(user: User): Future[List[CompanyWithAccess]] =
    db.run(
      table
        .join(CompanyTable.table)
        .on(_.companyId === _.id)
        .filter(_._1.userId === user.id)
        .filter(_._1.level =!= AccessLevel.NONE)
        .sortBy(_._1.updateDate.desc)
        .map(r => (r._2, r._1.level))
        .to[List]
        .result
    ).map(_.map(x => CompanyWithAccess(x._1, x._2)))

  override def fetchUsersWithLevel(companyIds: Seq[UUID]): Future[List[(User, AccessLevel)]] =
    db.run(
      table
        .join(UserTable.table)
        .on(_.userId === _.id)
        .filter(_._1.companyId inSet companyIds)
        .filter(_._1.level =!= AccessLevel.NONE)
        .sortBy(entry => (entry._1.level, entry._2.email))
        .map(r => (r._2, r._1.level))
        .distinct
        .to[List]
        .result
    )

  private[this] def fetchUsersAndAccessesByCompanies(
      companyIds: List[UUID],
      levels: Seq[AccessLevel]
  ): Future[List[(UUID, User)]] =
    db.run(
      (for {
        access <- table if access.level.inSet(levels) && (access.companyId inSetBind companyIds)
        user <- UserTable.table if user.id === access.userId
      } yield (access.companyId, user)).to[List].result
    )

  override def fetchUsersByCompanies(
      companyIds: List[UUID],
      levels: Seq[AccessLevel] = Seq(AccessLevel.ADMIN, AccessLevel.MEMBER)
  ): Future[List[User]] =
    fetchUsersAndAccessesByCompanies(companyIds, levels).map(_.map(_._2))

  override def fetchUsersByCompanyId(
      companyIds: List[UUID],
      levels: Seq[AccessLevel] = Seq(AccessLevel.ADMIN, AccessLevel.MEMBER)
  ): Future[Map[UUID, List[User]]] =
    fetchUsersAndAccessesByCompanies(companyIds, levels).map(users =>
      users.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    )

  override def fetchAdmins(companyId: UUID): Future[List[User]] =
    db.run(
      table
        .join(UserTable.table)
        .on(_.userId === _.id)
        .filter(_._1.companyId === companyId)
        .filter(_._1.level === AccessLevel.ADMIN)
        .map(_._2)
        .to[List]
        .result
    )

  override def createCompanyUserAccess(
      companyId: UUID,
      userId: UUID,
      level: AccessLevel
  ): FixedSqlAction[Int, NoStream, Effect.Write] =
    CompanyAccessTable.table.insertOrUpdate(
      UserAccess(
        companyId = companyId,
        userId = userId,
        level = level,
        updateDate = OffsetDateTime.now,
        creationDate = OffsetDateTime.now
      )
    )

  override def createUserAccess(companyId: UUID, userId: UUID, level: AccessLevel): Future[Int] =
    db.run(createCompanyUserAccess(companyId, userId, level))

  override def setUserLevel(company: Company, user: User, level: AccessLevel): Future[Unit] =
    db.run(
      table
        .filter(_.companyId === company.id)
        .filter(_.userId === user.id)
        .map(companyAccess => (companyAccess.level, companyAccess.updateDate))
        .update((level, OffsetDateTime.now()))
    ).map(_ => ())

  override def proFirstActivationCount(
      ticks: Int = 12
  ): Future[Vector[(Timestamp, Int)]] =
    db.run(sql"""select * from (
          select v.a, count(distinct id)
          from (select distinct company_id as id, min(my_date_trunc('month'::text, creation_date)::timestamp) as creation_date
                from company_accesses 
                group by company_id
                order by creation_date desc) as t
          right join
                (SELECT a FROM (VALUES #${computeTickValues(ticks)} ) AS X(a)) as v on t.creation_date = v.a
          group by v.a
          order by 1 DESC
    ) as res order by 1 ASC;    
         """.as[(Timestamp, Int)])

}
