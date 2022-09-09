package repositories.subscription

import models.Subscription
import repositories.CRUDRepositoryInterface
import utils.EmailAddress

import java.time.Period
import java.util.UUID
import scala.concurrent.Future

trait SubscriptionRepositoryInterface extends CRUDRepositoryInterface[Subscription] {

  def list(userId: UUID): Future[List[Subscription]]

  def listForFrequency(frequency: Period): Future[List[(Subscription, EmailAddress)]]

  def getDirectionDepartementaleEmail(department: String): Future[Seq[EmailAddress]]
}
