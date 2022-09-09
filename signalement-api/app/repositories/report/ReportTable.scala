package repositories.report

import models._
import models.report.DetailInputValue.toDetailInputValue
import models.report._
import repositories.PostgresProfile.api._
import utils._

import java.time._
import java.util.UUID
import ReportColumnType._
import repositories.DatabaseTable
import repositories.company.CompanyTable
import repositories.report.ReportRepository.queryFilter

class ReportTable(tag: Tag) extends DatabaseTable[Report](tag, "reports") {
  def gender = column[Option[Gender]]("gender")
  def category = column[String]("category")
  def subcategories = column[List[String]]("subcategories")
  def details = column[List[String]]("details")
  def companyId = column[Option[UUID]]("company_id")
  def companyName = column[Option[String]]("company_name")
  def companySiret = column[Option[SIRET]]("company_siret")
  def companyStreetNumber = column[Option[String]]("company_street_number")
  def companyStreet = column[Option[String]]("company_street")
  def companyAddressSupplement = column[Option[String]]("company_address_supplement")
  def companyPostalCode = column[Option[String]]("company_postal_code")
  def companyCity = column[Option[String]]("company_city")
  def companyCountry = column[Option[Country]]("company_country")
  def companyActivityCode = column[Option[String]]("company_activity_code")
  def websiteURL = column[Option[URL]]("website_url")
  def host = column[Option[String]]("host")
  def phone = column[Option[String]]("phone")
  def creationDate = column[OffsetDateTime]("creation_date")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def email = column[EmailAddress]("email")
  def consumerPhone = column[Option[String]]("consumer_phone")
  def consumerReferenceNumber = column[Option[String]]("consumer_reference_number")
  def contactAgreement = column[Boolean]("contact_agreement")
  def employeeConsumer = column[Boolean]("employee_consumer")
  def forwardToReponseConso = column[Boolean]("forward_to_reponseconso")
  def status = column[String]("status")
  def vendor = column[Option[String]]("vendor")
  def tags = column[List[ReportTag]]("tags")
  def reponseconsoCode = column[List[String]]("reponseconso_code")
  def ccrfCode = column[List[String]]("ccrf_code")

  def company = foreignKey("COMPANY_FK", companyId, CompanyTable.table)(
    _.id.?,
    onUpdate = ForeignKeyAction.Restrict,
    onDelete = ForeignKeyAction.Cascade
  )

  type ReportData = (
      UUID,
      Option[Gender],
      String,
      List[String],
      List[String],
      (
          Option[UUID],
          Option[String],
          Option[SIRET],
          Option[String],
          Option[String],
          Option[String],
          Option[String],
          Option[String],
          Option[Country],
          Option[String]
      ),
      (Option[URL], Option[String]),
      Option[String],
      OffsetDateTime,
      String,
      String,
      EmailAddress,
      Option[String],
      Option[String],
      Boolean,
      Boolean,
      Boolean,
      String,
      Option[String],
      List[ReportTag],
      List[String],
      List[String]
  )

  def constructReport: ReportData => Report = {
    case (
          id,
          gender,
          category,
          subcategories,
          details,
          (
            companyId,
            companyName,
            companySiret,
            companyStreetNumber,
            companyStreet,
            companyAddressSupplement,
            companyPostalCode,
            companyCity,
            companyCountry,
            companyActivityCode
          ),
          (websiteURL, host),
          phone,
          creationDate,
          firstName,
          lastName,
          email,
          consumerPhone,
          consumerReferenceNumber,
          contactAgreement,
          employeeConsumer,
          forwardToReponseConso,
          status,
          vendor,
          tags,
          reponseconsoCode,
          ccrfCode
        ) =>
      report.Report(
        id = id,
        gender = gender,
        category = category,
        subcategories = subcategories,
        details = details.filter(_ != null).map(toDetailInputValue),
        companyId = companyId,
        companyName = companyName,
        companyAddress = Address(
          number = companyStreetNumber,
          street = companyStreet,
          addressSupplement = companyAddressSupplement,
          postalCode = companyPostalCode,
          city = companyCity,
          country = companyCountry
        ),
        companySiret = companySiret,
        companyActivityCode = companyActivityCode,
        websiteURL = WebsiteURL(websiteURL, host),
        phone = phone,
        creationDate = creationDate,
        firstName = firstName,
        lastName = lastName,
        email = email,
        consumerPhone = consumerPhone,
        consumerReferenceNumber = consumerReferenceNumber,
        contactAgreement = contactAgreement,
        employeeConsumer = employeeConsumer,
        forwardToReponseConso = forwardToReponseConso,
        status = ReportStatus.withName(status),
        vendor = vendor,
        tags = tags,
        reponseconsoCode = reponseconsoCode,
        ccrfCode = ccrfCode
      )
  }

  def extractReport: PartialFunction[Report, ReportData] = { case r =>
    (
      r.id,
      r.gender,
      r.category,
      r.subcategories,
      r.details.map(detailInputValue => s"${detailInputValue.label} ${detailInputValue.value}"),
      (
        r.companyId,
        r.companyName,
        r.companySiret,
        r.companyAddress.number,
        r.companyAddress.street,
        r.companyAddress.addressSupplement,
        r.companyAddress.postalCode,
        r.companyAddress.city,
        r.companyAddress.country,
        r.companyActivityCode
      ),
      (r.websiteURL.websiteURL, r.websiteURL.host),
      r.phone,
      r.creationDate,
      r.firstName,
      r.lastName,
      r.email,
      r.consumerPhone,
      r.consumerReferenceNumber,
      r.contactAgreement,
      r.employeeConsumer,
      r.forwardToReponseConso,
      r.status.entryName,
      r.vendor,
      r.tags,
      r.reponseconsoCode,
      r.ccrfCode
    )
  }

  def * = (
    id,
    gender,
    category,
    subcategories,
    details,
    (
      companyId,
      companyName,
      companySiret,
      companyStreetNumber,
      companyStreet,
      companyAddressSupplement,
      companyPostalCode,
      companyCity,
      companyCountry,
      companyActivityCode
    ),
    (websiteURL, host),
    phone,
    creationDate,
    firstName,
    lastName,
    email,
    consumerPhone,
    consumerReferenceNumber,
    contactAgreement,
    employeeConsumer,
    forwardToReponseConso,
    status,
    vendor,
    tags,
    reponseconsoCode,
    ccrfCode
  ) <> (constructReport, extractReport.lift)
}

object ReportTable {

  val table = TableQuery[ReportTable]

  def table(userRole: UserRole): Query[ReportTable, Report, Seq] = userRole match {
    case UserRole.Admin | UserRole.DGCCRF => table
    case UserRole.Professionnel =>
      queryFilter(ReportFilter(status = ReportStatus.statusVisibleByPro, employeeConsumer = Some(false)))
  }
}
