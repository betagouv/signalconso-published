package services

import actors.EmailActor.EmailRequest
import cats.data.NonEmptyList
import config.EmailConfiguration
import play.api.Logger
import play.api.libs.mailer.Attachment
import repositories.reportblockednotification.ReportNotificationBlockedRepositoryInterface
import utils.EmailAddress
import utils.FrontRoute

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class MailService(
    sendEmail: EmailRequest => Unit,
    emailConfiguration: EmailConfiguration,
    reportNotificationBlocklistRepo: ReportNotificationBlockedRepositoryInterface,
    val pdfService: PDFService,
    attachmentService: AttachmentService
)(implicit
    val frontRoute: FrontRoute,
    private[this] val executionContext: ExecutionContext
) extends MailServiceInterface {

  private[this] val logger = Logger(this.getClass)
  private[this] val mailFrom = emailConfiguration.from
  implicit private[this] val contactAddress = emailConfiguration.contactAddress

  override def send(
      email: Email
  ): Future[Unit] = email match {
    case email: ProFilteredEmail => filterBlockedAndSend(email)
    case email =>
      send(
        email.recipients,
        email.subject,
        email.getBody(frontRoute, contactAddress),
        email.getAttachements(attachmentService)
      )
  }

  /** Filter pro user recipients that are excluded from notifications and send email
    */
  private def filterBlockedAndSend(email: ProFilteredEmail): Future[Unit] =
    email.report.companyId match {
      case Some(companyId) =>
        reportNotificationBlocklistRepo
          .filterBlockedEmails(email.recipients, companyId)
          .flatMap(recipient =>
            send(
              recipient.toList,
              email.subject,
              email.getBody(frontRoute, contactAddress),
              email.getAttachements(attachmentService)
            )
          )
      case None =>
        logger.debug("No company linked to report, not sending emails")
        Future.successful(())
    }

  private def filterEmail(recipients: Seq[EmailAddress]): Seq[EmailAddress] =
    recipients.filter(_.nonEmpty).filter { emailAddress =>
      val isAllowed = emailConfiguration.outboundEmailFilterRegex.findFirstIn(emailAddress.value).nonEmpty
      if (!isAllowed) {
        logger.warn(
          s"""Filtering email ${emailAddress}
             |because it does not match outboundEmailFilterRegex conf pattern :
             | ${emailConfiguration.outboundEmailFilterRegex
              .toString()}""".stripMargin
        )
      }
      isAllowed
    }

  private def send(
      recipients: Seq[EmailAddress],
      subject: String,
      bodyHtml: String,
      attachments: Seq[Attachment]
  ): Future[Unit] = {
    val filteredEmptyEmail: Seq[EmailAddress] = filterEmail(recipients)
    NonEmptyList.fromList(filteredEmptyEmail.toList) match {
      case None =>
        Future.successful(())
      case Some(filteredRecipients) =>
        val emailRequest = EmailRequest(
          from = mailFrom,
          recipients = filteredRecipients,
          subject = subject,
          bodyHtml = bodyHtml,
          attachments = attachments
        )
        Future.successful(sendEmail(emailRequest))
    }
  }

}
