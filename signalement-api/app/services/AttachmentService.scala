package services

import models.event.Event
import models.report.Report
import models.report.ReportFile
import play.api.Environment
import play.api.libs.mailer.Attachment
import play.api.libs.mailer.AttachmentData
import play.api.libs.mailer.AttachmentFile
import utils.FrontRoute

class AttachmentService(environment: Environment, pdfService: PDFService, frontRoute: FrontRoute) {

  val defaultAttachments: Seq[Attachment] = Seq(
    AttachmentFile(
      "logo-signal-conso.png",
      environment.getFile("/appfiles/logo-signal-conso.png"),
      contentId = Some("logo-signalconso")
    ),
    AttachmentFile(
      "logo-marianne.png",
      environment.getFile("/appfiles/logo-marianne.png"),
      contentId = Some("logo-marianne")
    )
  )

  def attachmentSeqForWorkflowStepN(n: Int): Seq[Attachment] = defaultAttachments ++ Seq(
    AttachmentFile(
      s"schemaSignalConso-Etape$n.png",
      environment.getFile(s"/appfiles/schemaSignalConso-Etape$n.png"),
      contentId = Some(s"schemaSignalConso-Etape$n")
    )
  )

  val ConsumerProResponseNotificationAttachement: Seq[Attachment] =
    defaultAttachments ++ attachmentSeqForWorkflowStepN(4) ++ Seq(
      AttachmentFile(
        s"happy.png",
        environment.getFile(s"/appfiles/happy.png"),
        contentId = Some(s"happy")
      ),
      AttachmentFile(
        s"neutral.png",
        environment.getFile(s"/appfiles/neutral.png"),
        contentId = Some(s"neutral")
      ),
      AttachmentFile(
        s"sad.png",
        environment.getFile(s"/appfiles/sad.png"),
        contentId = Some(s"sad")
      )
    )

  def needWorkflowSeqForWorkflowStepN(n: Int, report: Report): Seq[Attachment] =
    if (report.needWorkflowAttachment()) {
      attachmentSeqForWorkflowStepN(n)
    } else defaultAttachments

  def reportAcknowledgmentAttachement(
      report: Report,
      event: Event,
      files: Seq[ReportFile]
  ): Seq[Attachment] =
    needWorkflowSeqForWorkflowStepN(2, report) ++
      Seq(
        AttachmentData(
          "Signalement.pdf",
          pdfService.getPdfData(
            views.html.pdfs.report(report, Seq((event, None)), None, Seq.empty, files)(frontRoute, None)
          ),
          "application/pdf"
        )
      ).filter(_ => report.isContractualDispute() && report.companyId.isDefined)

}
