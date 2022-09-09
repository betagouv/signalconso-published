package models.report

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import utils.EmailAddress
import utils.SIRET
import utils.URL
import ReportTag._
import models.report.reportfile.ReportFileId

import java.time.OffsetDateTime
import java.util.UUID

case class ReportToExternal(
    id: UUID,
    gender: Option[Gender],
    creationDate: OffsetDateTime,
    category: String,
    subcategories: List[String],
    details: List[DetailInputValue],
    description: Option[String],
    question: Option[String],
    postalCode: Option[String],
    siret: Option[SIRET],
    websiteURL: Option[URL],
    phone: Option[String],
    consumerPhone: Option[String],
    consumerReferenceNumber: Option[String],
    firstName: String,
    lastName: String,
    email: EmailAddress,
    contactAgreement: Boolean,
    effectiveDate: Option[String],
    reponseconsoCode: List[String],
    ccrfCode: List[String],
    tags: List[String]
) {}

object ReportToExternal {

  val reponseConsoInputLabel = "Votre question"
  val descriptionInputLabel = "Description"

  def fromReport(r: Report) =
    ReportToExternal(
      id = r.id,
      gender = r.gender,
      creationDate = r.creationDate,
      category = r.category,
      subcategories = r.subcategories,
      details = r.details,
      description = r.details
        .filter(d => d.label.matches(descriptionInputLabel + ".*"))
        .map(_.value)
        .headOption,
      question = r.details
        .filter(d => d.label.matches(reponseConsoInputLabel + ".*"))
        .map(_.value)
        .headOption,
      siret = r.companySiret,
      postalCode = r.companyAddress.postalCode,
      websiteURL = r.websiteURL.websiteURL,
      phone = r.phone,
      firstName = r.firstName,
      lastName = r.lastName,
      email = r.email,
      consumerPhone = r.consumerPhone,
      consumerReferenceNumber = r.consumerReferenceNumber,
      contactAgreement = r.contactAgreement,
      effectiveDate = r.details
        .filter(d => d.label.matches("Date .* (constat|contrat|rendez-vous|course) .*"))
        .map(_.value)
        .headOption,
      reponseconsoCode = r.reponseconsoCode,
      ccrfCode = r.ccrfCode,
      tags = r.tags.map(_.translate())
    )

  implicit val format: OFormat[ReportToExternal] = Json.format[ReportToExternal]
}

case class ReportFileToExternal(
    id: ReportFileId,
    filename: String
)

object ReportFileToExternal {

  def fromReportFile(reportFile: ReportFile) = ReportFileToExternal(
    id = reportFile.id,
    filename = reportFile.filename
  )

  implicit val format: OFormat[ReportFileToExternal] = Json.format[ReportFileToExternal]
}

case class ReportWithFilesToExternal(
    report: ReportToExternal,
    files: List[ReportFileToExternal]
)

object ReportWithFilesToExternal {
  implicit val format: OFormat[ReportWithFilesToExternal] = Json.format[ReportWithFilesToExternal]

  def fromReportWithFiles(reportWithFiles: ReportWithFiles) = ReportWithFilesToExternal(
    report = ReportToExternal.fromReport(reportWithFiles.report),
    files = reportWithFiles.files.map(ReportFileToExternal.fromReportFile)
  )
}
