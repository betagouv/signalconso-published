package orchestrators

import models.report.ReportBlockedNotification
import repositories.reportblockednotification.ReportNotificationBlockedRepositoryInterface

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ReportBlockedNotificationOrchestrator(
    repository: ReportNotificationBlockedRepositoryInterface
)(implicit val executionContext: ExecutionContext) {

  def findByUserId(userId: UUID): Future[Seq[ReportBlockedNotification]] = repository.findByUserId(userId)

  def createIfNotExists(userId: UUID, companyIds: Seq[UUID]): Future[Seq[ReportBlockedNotification]] =
    for {
      currentBlocked <- repository.findByUserId(userId)
      notExistingCompanyIds = companyIds.diff(currentBlocked.map(_.companyId))
      blocked <- repository.create(userId, notExistingCompanyIds)
    } yield blocked

  def delete(userId: UUID, companyIds: Seq[UUID]): Future[Int] = repository.delete(userId, companyIds)
}
