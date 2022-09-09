package actors

import akka.actor._
import akka.stream.Materializer
import cats.data.NonEmptyList
import play.api.Logger
import play.api.libs.mailer._
import services.MailerService
import utils.EmailAddress

import javax.mail.internet.AddressException
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

object EmailActor {
  def props = Props[EmailActor]()

  case class EmailRequest(
      from: EmailAddress,
      recipients: NonEmptyList[EmailAddress],
      subject: String,
      bodyHtml: String,
      blindRecipients: Seq[EmailAddress] = Seq.empty,
      attachments: Seq[Attachment] = Seq.empty,
      times: Int = 0
  )
}

class EmailActor(mailerService: MailerService)(implicit val mat: Materializer) extends Actor {
  import EmailActor._
  implicit val ec: ExecutionContext = context.dispatcher

  val logger: Logger = Logger(this.getClass)
  override def preStart() =
    logger.debug("Starting")
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    logger.error(s"Restarting due to [${reason.getMessage}] when processing [${message.getOrElse("")}]")
  override def receive = {
    case req: EmailRequest =>
      try {
        mailerService.sendEmail(
          req.from,
          req.recipients.toList,
          req.blindRecipients,
          req.subject,
          req.bodyHtml,
          req.attachments
        )
        logger.debug(s"Sent email to ${req.recipients}")
      } catch {

        case _: AddressException =>
          logger.warn(
            s"Malformed email address [recipients : ${req.recipients.toList.mkString(",")}, subject : ${req.subject} ]"
          )
        case e: Exception =>
          logger.error(
            s"Unexpected error when sending email [ number of attempt :${req.times + 1}, from :${req.from}, recipients: ${req.recipients}, subject : ${req.subject}]",
            e
          )
          if (req.times < 2) {
            context.system.scheduler.scheduleOnce(req.times * 9 + 1 minute, self, req.copy(times = req.times + 1))
            ()
          } else {
            logger.warn(
              s"Email has exceeding max delivery attempts. Abording delivery or email [recipients : ${req.recipients}, subject : ${req.subject} ]"
            )
          }
      }

    case _ =>
      logger.debug("Could not handle request, ignoring message")

  }
}
