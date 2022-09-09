package models.report

import ai.x.play.json.Encoders.encoder
import ai.x.play.json.Jsonx
import com.github.tminglei.slickpg.composite.Struct
import models.Address
import models.Company
import models.PaginatedResult
import models.UserRole
import models.report.ReportTag.ReportTagHiddenToProfessionnel
import models.report.ReportTag.jsonFormat
import models.report.reportfile.ReportFileId
import play.api.libs.json._
import utils.Constants.ActionEvent.ActionEventValue
import utils.EmailAddress
import utils.SIRET
import utils.URL

import java.time.OffsetDateTime
import java.util.UUID
import scala.annotation.nowarn

case class Report(
    id: UUID = UUID.randomUUID(),
    gender: Option[Gender],
    category: String,
    subcategories: List[String],
    details: List[DetailInputValue],
    companyId: Option[UUID],
    companyName: Option[String],
    companyAddress: Address,
    companySiret: Option[SIRET],
    companyActivityCode: Option[String],
    websiteURL: WebsiteURL,
    phone: Option[String],
    creationDate: OffsetDateTime = OffsetDateTime.now(),
    firstName: String,
    lastName: String,
    email: EmailAddress,
    consumerPhone: Option[String] = None,
    consumerReferenceNumber: Option[String] = None,
    contactAgreement: Boolean,
    employeeConsumer: Boolean,
    forwardToReponseConso: Boolean = false,
    status: ReportStatus = ReportStatus.NA,
    vendor: Option[String] = None,
    tags: List[ReportTag] = Nil,
    reponseconsoCode: List[String] = Nil,
    ccrfCode: List[String] = Nil
) {

  def initialStatus() =
    if (employeeConsumer) ReportStatus.LanceurAlerte
    else if (
      companySiret.isDefined && tags
        .intersect(ReportTagHiddenToProfessionnel)
        .isEmpty
    ) {
      ReportStatus.TraitementEnCours
    } else { ReportStatus.NA }

  def shortURL() = websiteURL.websiteURL.map(_.value.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)", ""))

  def isContractualDispute() = tags.contains(ReportTag.LitigeContractuel)

  def needWorkflowAttachment() = !employeeConsumer &&
    !isContractualDispute() &&
    tags.intersect(Seq(ReportTag.ProduitDangereux, ReportTag.ReponseConso)).isEmpty

  def isTransmittableToPro() = !employeeConsumer && !forwardToReponseConso
}

object Report {

  @nowarn
  private[this] val jsonFormatX = Jsonx.formatCaseClass[Report]
  implicit val reportReads: Reads[Report] = jsonFormatX

  implicit def writer(implicit userRole: Option[UserRole] = None) = new Writes[Report] {
    def writes(report: Report) =
      Json.obj(
        "id" -> report.id,
        "category" -> report.category,
        "subcategories" -> report.subcategories,
        "details" -> report.details,
        "companyId" -> report.companyId,
        "companyName" -> report.companyName,
        "companyAddress" -> Json.toJson(report.companyAddress),
        "companySiret" -> report.companySiret,
        "creationDate" -> report.creationDate,
        "contactAgreement" -> report.contactAgreement,
        "status" -> report.status,
        "websiteURL" -> report.websiteURL.websiteURL,
        "host" -> report.websiteURL.host,
        "vendor" -> report.vendor,
        "tags" -> report.tags,
        "activityCode" -> report.companyActivityCode
      ) ++ ((userRole, report.contactAgreement) match {
        case (Some(UserRole.Professionnel), false) => Json.obj()
        case (_, _) =>
          Json.obj(
            "firstName" -> report.firstName,
            "lastName" -> report.lastName,
            "email" -> report.email,
            "consumerReferenceNumber" -> report.consumerReferenceNumber
          )
      }) ++ (userRole match {
        case Some(UserRole.Professionnel) => Json.obj()
        case _ =>
          Json.obj(
            "ccrfCode" -> report.ccrfCode,
            "phone" -> report.phone,
            "consumerPhone" -> report.consumerPhone,
            "employeeConsumer" -> report.employeeConsumer,
            "reponseconsoCode" -> report.reponseconsoCode,
            "gender" -> report.gender
          )
      })

  }
}

case class WebsiteURL(websiteURL: Option[URL], host: Option[String])

object WebsiteURL {
  implicit val WebsiteURLFormat: OFormat[WebsiteURL] = Json.format[WebsiteURL]
}

case class ReportWithFiles(
    report: Report,
    files: List[ReportFile]
)

object ReportWithFiles {
  implicit def writer(implicit userRole: Option[UserRole] = None) = Json.writes[ReportWithFiles]
}

case class DetailInputValue(
    label: String,
    value: String
) extends Struct

object DetailInputValue {
  implicit val detailInputValueFormat: OFormat[DetailInputValue] = Json.format[DetailInputValue]

  val DefaultKey = "Précision :"
  def toDetailInputValue(input: String): DetailInputValue =
    input match {
      case input if input.contains(':') =>
        DetailInputValue(input.substring(0, input.indexOf(':') + 1), input.substring(input.indexOf(':') + 1).trim)
      case input =>
        DetailInputValue(DefaultKey, input)
    }

  def detailInputValuetoString(detailInputValue: DetailInputValue): String = detailInputValue.label match {
    case DefaultKey => detailInputValue.value
    case _          => s"${detailInputValue.label} ${detailInputValue.value}"
  }

}

/** @deprecated Keep it for compat purpose but no longer used in new dashboard */
case class DeprecatedCompanyWithNbReports(company: Company, count: Int)

/** @deprecated Keep it for compat purpose but no longer used in new dashboard */
object DeprecatedCompanyWithNbReports {

  implicit val companyWithNbReportsWrites = new Writes[DeprecatedCompanyWithNbReports] {
    def writes(data: DeprecatedCompanyWithNbReports) = Json.obj(
      "companySiret" -> data.company.siret,
      "companyName" -> data.company.name,
      "companyAddress" -> Json.toJson(data.company.address),
      "count" -> data.count
    )
  }

  implicit val paginatedCompanyWithNbReportsWriter = Json.writes[PaginatedResult[DeprecatedCompanyWithNbReports]]
}

case class ReportCompany(
    name: String,
    address: Address,
    siret: SIRET,
    activityCode: Option[String],
    isHeadOffice: Boolean,
    isOpen: Boolean
)

object ReportCompany {
  implicit val format = Json.format[ReportCompany]
}

case class ReportConsumerUpdate(
    firstName: String,
    lastName: String,
    email: EmailAddress,
    contactAgreement: Boolean,
    consumerReferenceNumber: Option[String]
)

object ReportConsumerUpdate {
  implicit val format = Json.format[ReportConsumerUpdate]
}

case class ReportAction(
    actionType: ActionEventValue,
    details: Option[String],
    fileIds: List[ReportFileId]
)

object ReportAction {
  implicit val reportAction: OFormat[ReportAction] = Json.format[ReportAction]
}
