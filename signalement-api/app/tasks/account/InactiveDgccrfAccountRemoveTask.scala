package tasks.account

import cats.implicits.toTraverseOps
import models.User
import play.api.Logger
import repositories.asyncfiles.AsyncFileRepositoryInterface
import repositories.event.EventRepositoryInterface
import repositories.subscription.SubscriptionRepositoryInterface
import repositories.user.UserRepositoryInterface
import tasks.model.TaskType
import tasks.TaskExecutionResult
import tasks.TaskExecutionResults
import tasks.toValidated

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class InactiveDgccrfAccountRemoveTask(
    userRepository: UserRepositoryInterface,
    subscriptionRepository: SubscriptionRepositoryInterface,
    eventRepository: EventRepositoryInterface,
    asyncFileRepository: AsyncFileRepositoryInterface
)(implicit executionContext: ExecutionContext) {

  val logger: Logger = Logger(this.getClass)

  def removeInactiveAccounts(expirationDateThreshold: OffsetDateTime): Future[TaskExecutionResults] = {

    logger.info(s"Removing inactive DGCCRF accounts with last validation below $expirationDateThreshold")
    for {
      inactiveDGCCRFAccounts <- userRepository.listExpiredDGCCRF(expirationDateThreshold)
      results <- inactiveDGCCRFAccounts.map(removeWithSubscriptions).sequence
    } yield results.sequence
  }

  private def removeWithSubscriptions(user: User): Future[TaskExecutionResult] = {

    val executionOrError = for {
      subscriptionToDelete <- subscriptionRepository.list(user.id)
      _ = logger.debug(s"Removing subscription with ids ${subscriptionToDelete.map(_.id)}")
      _ <- subscriptionToDelete.map(subscription => subscriptionRepository.delete(subscription.id)).sequence
      _ = logger.debug(s"Removing events")
      _ <- eventRepository.deleteByUserId(user.id)
      _ = logger.debug(s"Removing files")
      _ <- asyncFileRepository.deleteByUserId(user.id)
      _ = logger.debug(s"Removing user")
      _ <- userRepository.delete(user.id)
      _ = logger.debug(s"Inactive DGCCRF account user ${user.id} successfully removed")
    } yield ()

    toValidated(executionOrError, user.id, TaskType.InactiveAccountClean)

  }

}
