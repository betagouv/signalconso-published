package utils

import company.CompanyData
import models.event.Event._
import models._
import models.event.Event
import models.report.Gender
import models.report.Report
import models.report.ReportCompany
import models.report.ReportConsumerUpdate
import models.report.ReportDraft
import models.report.ReportStatus
import models.report.WebsiteURL
import models.website.Website
import models.website.WebsiteId
import models.website.IdentificationStatus
import org.scalacheck.Arbitrary._
import org.scalacheck._
import utils.Constants.ActionEvent.ActionEventValue
import utils.Constants.EventType.EventTypeValue

import java.time.OffsetDateTime
import java.util.UUID
import scala.util.Random

object Fixtures {
  // Avoids creating strings with null chars because Postgres text fields don't support it.
  // see http://stackoverflow.com/questions/1347646/postgres-error-on-insert-error-invalid-byte-sequence-for-encoding-utf8-0x0
  implicit val arbString: Arbitrary[String] =
    Arbitrary(Gen.identifier.map(_.replaceAll("\u0000", "")))

  val genGender = Gen.oneOf(Some(Gender.Female), Some(Gender.Male), None)

  val genUser = for {
    id <- arbitrary[UUID]
    password <- arbString.arbitrary
    firstName <- genFirstName
    lastName <- genLastName
    userRole <- Gen.oneOf(UserRole.values)
    email <- genEmailAddress(firstName, lastName)
  } yield User(id, password, email, firstName, lastName, userRole, None)

  val genFirstName = Gen.oneOf("Alice", "Bob", "Charles", "Danièle", "Émilien", "Fanny", "Gérard")
  val genLastName = Gen.oneOf("Doe", "Durand", "Dupont")
  def genEmailAddress(firstName: String, lastName: String): Gen[EmailAddress] = EmailAddress(
    s"${firstName}.${lastName}.${Gen.choose(0, 1000000).sample.get}@example.com"
  )

  def genEmailAddress: Gen[EmailAddress] = EmailAddress(
    s"${genFirstName.sample.get}.${genLastName.sample.get}.${Gen.choose(0, 1000000).sample.get}@example.com"
  )

  val genAdminUser = genUser.map(_.copy(userRole = UserRole.Admin))
  val genProUser = genUser.map(_.copy(userRole = UserRole.Professionnel))
  val genDgccrfUser = genUser.map(_.copy(userRole = UserRole.DGCCRF))

  val genSiren = for {
    randInt <- Gen.choose(0, 999999999)
  } yield SIREN("" + randInt takeRight 9)

  def genSiret(siren: Option[SIREN] = None) = for {
    randInt <- Gen.choose(0, 99999)
    sirenGen <- genSiren
  } yield SIRET.fromUnsafe(siren.getOrElse(sirenGen).value + ("" + randInt takeRight 5))

  def genAddress(postalCode: Option[String] = Some("37500")) = for {
    number <- arbString.arbitrary
    street <- arbString.arbitrary
    addressSupplement <- arbString.arbitrary
    city <- arbString.arbitrary
  } yield Address(
    number = Some("number_" + number),
    street = Some("street_" + street),
    addressSupplement = Some("addressSupplement_" + addressSupplement),
    postalCode = postalCode,
    city = Some("city_" + city)
  )

  val genCompany = for {
    _ <- arbitrary[UUID]
    name <- arbString.arbitrary
    siret <- genSiret()
    address <- genAddress()
  } yield Company(
    siret = siret,
    name = name,
    address = address,
    activityCode = None,
    isOpen = true,
    isHeadOffice = false
  )

  def genCompanyData(company: Option[Company] = None) = for {
    id <- arbitrary[UUID]
    siret <- genSiret()
    denom <- arbString.arbitrary
  } yield CompanyData(
    id,
    company.map(_.siret).getOrElse(siret),
    SIREN(company.map(_.siret).getOrElse(siret)),
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    Some(denom),
    None,
    "",
    None
  )

  val genWebsiteURL = for {
    randInt <- Gen.choose(0, 1000000)
  } yield URL(s"https://www.example${randInt}.com")

  val genReportedPhone = for {
    randInt <- Gen.choose(0, 999999999)
  } yield randInt.toString

  def genDraftReport = for {
    gender <- genGender
    category <- arbString.arbitrary
    subcategory <- arbString.arbitrary
    firstName <- genFirstName
    lastName <- genLastName
    email <- genEmailAddress(firstName, lastName)
    contactAgreement <- arbitrary[Boolean]
    company <- genCompany
    websiteURL <- genWebsiteURL
  } yield ReportDraft(
    gender = gender,
    category = category,
    subcategories = List(subcategory),
    details = List(),
    companyName = Some(company.name),
    companyAddress = Some(company.address),
    companySiret = Some(company.siret),
    companyActivityCode = None,
    companyIsHeadOffice = Some(company.isHeadOffice),
    companyIsOpen = Some(company.isOpen),
    websiteURL = Some(websiteURL),
    phone = None,
    firstName = firstName,
    lastName = lastName,
    email = email,
    consumerPhone = None,
    consumerReferenceNumber = None,
    contactAgreement = contactAgreement,
    employeeConsumer = false,
    fileIds = List.empty
  )

  def genReportForCompany(company: Company) = for {
    id <- arbitrary[UUID]
    gender <- genGender
    category <- arbString.arbitrary
    subcategory <- arbString.arbitrary
    firstName <- genFirstName
    lastName <- genLastName
    email <- genEmailAddress(firstName, lastName)
    contactAgreement <- arbitrary[Boolean]
    status <- Gen.oneOf(ReportStatus.values)
  } yield Report(
    id = id,
    gender = gender,
    category = category,
    subcategories = List(subcategory),
    details = List(),
    companyId = Some(company.id),
    companyName = Some(company.name),
    companyAddress = company.address,
    companySiret = Some(company.siret),
    companyActivityCode = company.activityCode,
    websiteURL = WebsiteURL(None, None),
    phone = None,
    firstName = firstName,
    lastName = lastName,
    email = email,
    consumerPhone = None,
    consumerReferenceNumber = None,
    contactAgreement = contactAgreement,
    employeeConsumer = false,
    status = status
  )

  def genReportsForCompanyWithStatus(company: Company, status: ReportStatus) =
    Gen.listOfN(Random.nextInt(10), genReportForCompany(company).map(_.copy(status = status)))

  def genReportConsumerUpdate = for {
    firstName <- genFirstName
    lastName <- genLastName
    email <- genEmailAddress(firstName, lastName)
    contactAgreement <- arbitrary[Boolean]
    consumerReferenceNumber <- arbString.arbitrary
  } yield ReportConsumerUpdate(firstName, lastName, email, contactAgreement, Some(consumerReferenceNumber))

  def genReportCompany = for {
    name <- arbString.arbitrary
    address <- genAddress(postalCode = Some(Gen.choose(10000, 99999).toString))
    siret <- genSiret()
  } yield ReportCompany(name, address, siret, None, isHeadOffice = true, isOpen = true)

  def genEventForReport(reportId: UUID, eventType: EventTypeValue, actionEvent: ActionEventValue) = for {
    id <- arbitrary[UUID]
    companyId <- arbitrary[UUID]
    details <- arbString.arbitrary
  } yield Event(
    id,
    Some(reportId),
    Some(companyId),
    None,
    OffsetDateTime.now(),
    eventType,
    actionEvent,
    stringToDetailsJsValue(details)
  )

  def genEventForCompany(companyId: UUID, eventType: EventTypeValue, actionEvent: ActionEventValue) = for {
    id <- arbitrary[UUID]
    details <- arbString.arbitrary
  } yield Event(
    id,
    None,
    Some(companyId),
    None,
    OffsetDateTime.now(),
    eventType,
    actionEvent,
    stringToDetailsJsValue(details)
  )

  def genWebsite() = for {
    companyId <- arbitrary[UUID]
    websiteUrl <- genWebsiteURL
    kind <- Gen.oneOf(IdentificationStatus.values)
  } yield Website(
    id = WebsiteId.generateId(),
    creationDate = OffsetDateTime.now(),
    host = websiteUrl.getHost.get,
    companyCountry = None,
    companyId = Some(companyId),
    identificationStatus = kind
  )

}
