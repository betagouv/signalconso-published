package repositories.accesstoken

import models._
import repositories.PostgresProfile.api._
import models.token.TokenKind
import models.token.TokenKind.CompanyInit
import models.token.TokenKind.DGCCRFAccount
import repositories.accesstoken.AccessTokenColumnType._
import repositories.company.CompanyTable
import repositories.companyaccess.CompanyAccessColumnType._
import repositories.user.UserTable
import repositories.CRUDRepository
import repositories.computeTickValues
import repositories.companyaccess.CompanyAccessRepositoryInterface
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import utils.EmailAddress

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AccessTokenRepository(
    override val dbConfig: DatabaseConfig[JdbcProfile],
    val companyAccessRepository: CompanyAccessRepositoryInterface
)(implicit override val ec: ExecutionContext)
    extends CRUDRepository[AccessTokenTable, AccessToken]
    with AccessTokenRepositoryInterface {

  override val table: TableQuery[AccessTokenTable] = AccessTokenTable.table
  import dbConfig._

  private def fetchValidTokens =
    table
      .filter(_.expirationDate.filter(_ < OffsetDateTime.now(ZoneOffset.UTC)).isEmpty)
      .filter(_.valid)

  private def fetchCompanyValidTokens(companyId: UUID): Query[AccessTokenTable, AccessToken, Seq] =
    fetchValidTokens.filter(_.companyId === companyId)

  private def fetchCompanyValidTokens(company: Company): Query[AccessTokenTable, AccessToken, Seq] =
    fetchCompanyValidTokens(company.id)

  override def fetchToken(company: Company, emailedTo: EmailAddress): Future[Option[AccessToken]] =
    db.run(
      fetchCompanyValidTokens(company)
        .filter(_.emailedTo === emailedTo)
        .sortBy(_.expirationDate.desc)
        .result
        .headOption
    )

  override def fetchValidActivationToken(companyId: UUID): Future[Option[AccessToken]] =
    db.run(
      fetchCompanyValidTokens(companyId)
        .filter(_.kind === (CompanyInit: TokenKind))
        .filter(_.level === AccessLevel.ADMIN)
        .result
        .headOption
    )

  override def fetchActivationToken(companyId: UUID): Future[Option[AccessToken]] =
    db.run(
      table
        .filter(_.companyId === companyId)
        .filter(_.kind === (CompanyInit: TokenKind))
        .filter(_.level === AccessLevel.ADMIN)
        .result
        .headOption
    )

  override def getToken(company: Company, id: UUID): Future[Option[AccessToken]] =
    db.run(
      fetchCompanyValidTokens(company)
        .filter(_.id === id)
        .result
        .headOption
    )

  override def findToken(token: String): Future[Option[AccessToken]] =
    db.run(
      fetchValidTokens
        .filter(_.token === token)
        .filterNot(_.companyId.isDefined)
        .result
        .headOption
    )

  override def findValidToken(company: Company, token: String): Future[Option[AccessToken]] =
    db.run(
      fetchCompanyValidTokens(company)
        .filter(_.token === token)
        .result
        .headOption
    )

  override def fetchPendingTokens(company: Company): Future[List[AccessToken]] =
    db.run(
      fetchCompanyValidTokens(company)
        .sortBy(_.expirationDate.desc)
        .to[List]
        .result
    )

  override def removePendingTokens(company: Company): Future[Int] = db.run(
    fetchCompanyValidTokens(company).delete
  )

  override def fetchPendingTokens(emailedTo: EmailAddress): Future[List[AccessToken]] =
    db.run(
      table
        .filter(_.expirationDate.filter(_ < OffsetDateTime.now(ZoneOffset.UTC)).isEmpty)
        .filter(_.valid)
        .filter(_.emailedTo === emailedTo)
        .to[List]
        .result
    )

  override def fetchPendingTokensDGCCRF: Future[List[AccessToken]] =
    db.run(
      table
        .filter(_.expirationDate.filter(_ < OffsetDateTime.now(ZoneOffset.UTC)).isEmpty)
        .filter(_.valid)
        .filter(_.kind === (DGCCRFAccount: TokenKind))
        .to[List]
        .result
    )

  // TODO move to orchestrator...
  override def createCompanyAccessAndRevokeToken(token: AccessToken, user: User): Future[Boolean] =
    db.run(
      DBIO
        .seq(
          companyAccessRepository.createCompanyUserAccess(
            token.companyId.get,
            user.id,
            token.companyLevel.get
          ),
          table.filter(_.id === token.id).map(_.valid).update(false),
          table
            .filter(_.companyId === token.companyId)
            .filter(_.emailedTo.isEmpty)
            .map(_.valid)
            .update(false)
        )
        .transactionally
    ).map(_ => true)

  // TODO move to orchestrator...
  override def giveCompanyAccess(company: Company, user: User, level: AccessLevel): Future[Unit] =
    db.run(
      DBIO
        .seq(
          companyAccessRepository.createCompanyUserAccess(company.id, user.id, level),
          table
            .filter(_.companyId === company.id)
            .filter(_.emailedTo.isEmpty)
            .map(_.valid)
            .update(false)
        )
        .transactionally
    ).map(_ => ())

  override def invalidateToken(token: AccessToken): Future[Int] =
    db.run(
      table
        .filter(_.id === token.id)
        .map(_.valid)
        .update(false)
    )

  def updateToken(
      token: AccessToken,
      level: AccessLevel,
      validity: Option[java.time.temporal.TemporalAmount]
  ): Future[Int] =
    db.run(
      table
        .filter(_.id === token.id)
        .map(a => (a.level, a.expirationDate))
        .update((Some(level), validity.map(OffsetDateTime.now(ZoneOffset.UTC).plus(_))))
    )

  override def prefetchActivationCodes(companyIds: List[UUID]): Future[Map[UUID, String]] =
    db.run(
      table
        .filter(_.companyId inSetBind companyIds.distinct)
        .filter(_.expirationDate.filter(_ < OffsetDateTime.now(ZoneOffset.UTC)).isEmpty)
        .filter(_.valid)
        .filter(_.kind === (CompanyInit: TokenKind))
        .to[List]
        .result
    ).map(f => f.map(accessToken => accessToken.companyId.get -> accessToken.token).toMap)

  override def companiesToActivate(): Future[List[(AccessToken, Company)]] =
    db.run(
      table
        .join(CompanyTable.table)
        .on(_.companyId === _.id)
        .filter(
          _._1.creationDate < OffsetDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0)
        )
        .filter(_._1.expirationDate.filter(_ < OffsetDateTime.now(ZoneOffset.UTC)).isEmpty)
        .filter(_._1.valid)
        .filter(_._1.kind === (CompanyInit: TokenKind))
        .to[List]
        .result
    )

  override def fetchActivationCode(company: Company): Future[Option[String]] =
    fetchValidActivationToken(company.id).map(_.map(_.token))

  override def updateLastEmailValidation(user: User): Future[Boolean] =
    db.run(resetLastEmailValidation(user)).map(_ => true)

  override def validateEmail(token: AccessToken, user: User): Future[Boolean] =
    db.run(
      DBIO
        .seq(
          resetLastEmailValidation(user),
          table.filter(_.id === token.id).map(_.valid).update(false)
        )
        .transactionally
    ).map(_ => true)

  private def resetLastEmailValidation(user: User) = UserTable.table
    .filter(_.id === user.id)
    .map(_.lastEmailValidation)
    .update(Some(OffsetDateTime.now(ZoneOffset.UTC)))

  override def dgccrfAccountsCurve(ticks: Int): Future[Vector[(Timestamp, Int)]] =
    db.run(sql"""
      select *
      from (
        select my_date_trunc('month'::text, creation_date)::timestamp,
           sum(count(*)) over ( order by my_date_trunc('month'::text, creation_date)::timestamp rows between unbounded preceding and current row)
        from (
          select MAX(creation_date) as creation_date from access_tokens
          where kind = 'DGCCRF_ACCOUNT'
           and valid = false
           and emailed_to in (select email from users where role = 'DGCCRF')
            group by emailed_to
        ) as a
        group by my_date_trunc('month'::text, creation_date)
        order by 1 DESC LIMIT #${ticks}
      ) as res
      order by 1 ASC;
    """.as[(Timestamp, Int)])

  override def dgccrfSubscription(ticks: Int): Future[Vector[(Timestamp, Int)]] =
    db.run(sql"""select * from (select v.a, count(distinct s.user_id) from subscriptions s right join
                                               (SELECT a
                                                FROM (VALUES #${computeTickValues(ticks)} ) AS X(a))
                                                   as v on creation_date <= (my_date_trunc('month'::text, v.a)::timestamp + '1 month'::interval - '1 day'::interval)

group by v.a ) as res order by 1 ASC""".as[(Timestamp, Int)])

  override def dgccrfActiveAccountsCurve(ticks: Int): Future[Vector[(Timestamp, Int)]] =
    db.run(sql"""select * from (select v.a, count(distinct ac.emailed_to) from access_tokens ac right join
                                               (SELECT a
                                                FROM (VALUES #${computeTickValues(ticks)} ) AS X(a))
                                                   as v on creation_date between (my_date_trunc('month'::text, v.a)::timestamp + '1 month'::interval - '1 day'::interval) - '4 month'::interval 
                                                         and (my_date_trunc('month'::text, v.a)::timestamp + '1 month'::interval - '1 day'::interval) and kind = 'VALIDATE_EMAIL' and valid = false

group by v.a ) as res order by 1 ASC""".as[(Timestamp, Int)])

  override def dgccrfControlsCurve(ticks: Int): Future[Vector[(Timestamp, Int)]] =
    db.run(
      sql"""select * from (select my_date_trunc('month'::text, creation_date)::timestamp, count(distinct company_id)
  from events
    where action = 'Contrôle effectué'
  group by  my_date_trunc('month'::text,creation_date)
  order by  1 DESC LIMIT #${ticks} ) as res order by 1 ASC""".as[(Timestamp, Int)]
    )

}
