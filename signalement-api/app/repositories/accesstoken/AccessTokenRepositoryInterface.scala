package repositories.accesstoken

import models.AccessLevel
import models.AccessToken
import models.Company
import models.User
import repositories.CRUDRepositoryInterface
import utils.EmailAddress

import java.sql.Timestamp
import java.time.temporal.TemporalAmount
import java.util.UUID
import scala.concurrent.Future

trait AccessTokenRepositoryInterface extends CRUDRepositoryInterface[AccessToken] {

  def fetchToken(company: Company, emailedTo: EmailAddress): Future[Option[AccessToken]]

  def fetchValidActivationToken(companyId: UUID): Future[Option[AccessToken]]

  def fetchActivationToken(companyId: UUID): Future[Option[AccessToken]]

  def getToken(company: Company, id: UUID): Future[Option[AccessToken]]

  def findToken(token: String): Future[Option[AccessToken]]

  def findValidToken(company: Company, token: String): Future[Option[AccessToken]]

  def fetchPendingTokens(company: Company): Future[List[AccessToken]]

  def removePendingTokens(company: Company): Future[Int]

  def fetchPendingTokens(emailedTo: EmailAddress): Future[List[AccessToken]]

  def fetchPendingTokensDGCCRF: Future[List[AccessToken]]

  def createCompanyAccessAndRevokeToken(token: AccessToken, user: User): Future[Boolean]

  def giveCompanyAccess(company: Company, user: User, level: AccessLevel): Future[Unit]

  def invalidateToken(token: AccessToken): Future[Int]

  def updateToken(token: AccessToken, level: AccessLevel, validity: Option[TemporalAmount]): Future[Int]

  def prefetchActivationCodes(companyIds: List[UUID]): Future[Map[UUID, String]]

  def companiesToActivate(): Future[List[(AccessToken, Company)]]

  def fetchActivationCode(company: Company): Future[Option[String]]

  def validateEmail(token: AccessToken, user: User): Future[Boolean]

  def dgccrfAccountsCurve(ticks: Int): Future[Vector[(Timestamp, Int)]]

  def dgccrfSubscription(ticks: Int): Future[Vector[(Timestamp, Int)]]

  def dgccrfActiveAccountsCurve(ticks: Int): Future[Vector[(Timestamp, Int)]]

  def dgccrfControlsCurve(ticks: Int): Future[Vector[(Timestamp, Int)]]

  def updateLastEmailValidation(user: User): Future[Boolean]
}
