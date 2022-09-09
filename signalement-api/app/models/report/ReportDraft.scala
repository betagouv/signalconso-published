package models.report

import models.Address
import utils.EmailAddress
import utils.SIRET
import utils.URL
import models.report.ReportTag.TranslationReportTagReads
import ai.x.play.json.Jsonx
import ai.x.play.json.Encoders.encoder
import models.report.ReportTag
import models.report.reportfile.ReportFileId
import play.api.libs.json.OFormat
import java.time.OffsetDateTime
import java.util.UUID
import scala.annotation.nowarn

case class ReportDraft(
    gender: Option[Gender],
    category: String,
    subcategories: List[String],
    details: List[DetailInputValue],
    companyName: Option[String],
    companyAddress: Option[Address],
    companySiret: Option[SIRET],
    companyActivityCode: Option[String],
    companyIsHeadOffice: Option[Boolean],
    companyIsOpen: Option[Boolean],
    websiteURL: Option[URL],
    phone: Option[String],
    firstName: String,
    lastName: String,
    email: EmailAddress,
    consumerPhone: Option[String],
    consumerReferenceNumber: Option[String],
    contactAgreement: Boolean,
    employeeConsumer: Boolean,
    forwardToReponseConso: Option[Boolean] = Some(false),
    fileIds: List[ReportFileId],
    vendor: Option[String] = None,
    tags: List[ReportTag] = Nil,
    reponseconsoCode: Option[List[String]] = None,
    ccrfCode: Option[List[String]] = None
) {

  def generateReport(
      maybeCompanyId: Option[UUID],
      reportId: UUID = UUID.randomUUID(),
      creationDate: OffsetDateTime = OffsetDateTime.now()
  ): Report = {
    val report = Report(
      reportId,
      gender = gender,
      creationDate = creationDate,
      category = category,
      subcategories = subcategories,
      details = details,
      companyId = maybeCompanyId,
      companyName = companyName,
      companyAddress = companyAddress.getOrElse(Address()),
      companySiret = companySiret,
      companyActivityCode = companyActivityCode,
      websiteURL = WebsiteURL(websiteURL, websiteURL.flatMap(_.getHost)),
      phone = phone,
      firstName = firstName,
      lastName = lastName,
      email = email,
      consumerPhone = consumerPhone,
      consumerReferenceNumber = consumerReferenceNumber,
      contactAgreement = contactAgreement,
      employeeConsumer = employeeConsumer,
      status = ReportStatus.NA,
      forwardToReponseConso = forwardToReponseConso.getOrElse(false),
      vendor = vendor,
      tags = tags.distinct
        .filterNot(tag => tag == ReportTag.LitigeContractuel && employeeConsumer),
      reponseconsoCode = reponseconsoCode.getOrElse(Nil),
      ccrfCode = ccrfCode.getOrElse(Nil)
    )
    report.copy(status = report.initialStatus())
  }
}

object ReportDraft {
  def isValid(draft: ReportDraft): Boolean =
    (draft.companySiret.isDefined
      || draft.websiteURL.isDefined
      || draft.tags.contains(ReportTag.Influenceur) && draft.companyAddress
        .exists(_.postalCode.isDefined)
      || (draft.companyAddress.exists(x => x.country.isDefined || x.postalCode.isDefined))
      || draft.phone.isDefined)

  /** Used as workaround to parse values from their translation as signalement-app is pushing transaction instead of
    * entry name Make sure no translated values is passed as ReportTag to remove this reads
    */
  implicit val reportTagReads = TranslationReportTagReads
  @nowarn
  implicit val draftReportReads: OFormat[ReportDraft] = Jsonx.formatCaseClass[ReportDraft]

}
