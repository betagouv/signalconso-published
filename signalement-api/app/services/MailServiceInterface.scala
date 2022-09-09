package services
import scala.concurrent.Future

trait MailServiceInterface {
  def send(email: Email): Future[Unit]
}
