package models

import play.api.libs.json._
import utils.QueryStringMapper
import utils.SIRET

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import scala.util.Try

sealed case class AccessLevel(value: String)

object AccessLevel {
  val NONE = AccessLevel("none")
  val MEMBER = AccessLevel("member")
  val ADMIN = AccessLevel("admin")

  def fromValue(v: String) =
    List(NONE, MEMBER, ADMIN).find(_.value == v).getOrElse(NONE)
  implicit val reads = new Reads[AccessLevel] {
    def reads(json: JsValue): JsResult[AccessLevel] = json.validate[String].map(fromValue)
  }
  implicit val writes = new Writes[AccessLevel] {
    def writes(level: AccessLevel) = Json.toJson(level.value)
  }
}

case class UserAccess(
    companyId: UUID,
    userId: UUID,
    level: AccessLevel,
    updateDate: OffsetDateTime,
    creationDate: OffsetDateTime
)

case class Company(
    id: UUID = UUID.randomUUID(),
    siret: SIRET,
    creationDate: OffsetDateTime = OffsetDateTime.now,
    name: String,
    address: Address,
    activityCode: Option[String],
    isHeadOffice: Boolean,
    isOpen: Boolean
) {
  def shortId = this.id.toString.substring(0, 13).toUpperCase
}

case class CompanyRegisteredSearch(
    departments: Seq[String] = Seq.empty[String],
    activityCodes: Seq[String] = Seq.empty[String],
    identity: Option[SearchCompanyIdentity] = None,
    emailsWithAccess: Option[String] = None
)

object CompanyRegisteredSearch {
  def fromQueryString(q: Map[String, Seq[String]]): Try[CompanyRegisteredSearch] = Try {
    val mapper = new QueryStringMapper(q)
    CompanyRegisteredSearch(
      departments = mapper.seq("departments"),
      activityCodes = mapper.seq("activityCodes"),
      emailsWithAccess = mapper.string("emailsWithAccess"),
      identity = mapper.string("identity").map(SearchCompanyIdentity.fromString)
    )
  }
}

object Company {

  implicit val companyFormat: OFormat[Company] = Json.format[Company]
}

case class CompanyWithAccess(
    company: Company,
    level: AccessLevel
)

object CompanyWithAccess {
  implicit def writes: Writes[CompanyWithAccess] = (companyWithAccess: CompanyWithAccess) => {
    val companyJson = Json.toJson(companyWithAccess.company).as[JsObject]
    companyJson + ("level" -> Json.toJson(companyWithAccess.level))
  }
}

case class CompanyCreation(
    siret: SIRET,
    name: String,
    address: Address,
    activityCode: Option[String],
    isHeadOffice: Option[Boolean],
    isOpen: Option[Boolean]
) {
  def toCompany(): Company = Company(
    siret = siret,
    name = name,
    address = address,
    activityCode = activityCode,
    isHeadOffice = isHeadOffice.getOrElse(false),
    isOpen = isOpen.getOrElse(true)
  )
}

object CompanyCreation {

  implicit val format: OFormat[CompanyCreation] = Json.format[CompanyCreation]
}

case class CompanyWithNbReports(
    id: UUID = UUID.randomUUID(),
    siret: SIRET,
    creationDate: OffsetDateTime = OffsetDateTime.now,
    name: String,
    address: Address,
    activityCode: Option[String],
    count: Int,
    responseRate: Int
)

object CompanyWithNbReports {
  implicit val writes: Writes[CompanyWithNbReports] = Json.writes[CompanyWithNbReports]
}

case class CompanyAddressUpdate(
    address: Address,
    activationDocumentRequired: Boolean = false
)

object CompanyAddressUpdate {
  implicit val format: OFormat[CompanyAddressUpdate] = Json.format[CompanyAddressUpdate]
}

case class UndeliveredDocument(returnedDate: LocalDate)

object UndeliveredDocument {
  implicit val format: OFormat[UndeliveredDocument] = Json.format[UndeliveredDocument]
}
