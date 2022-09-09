package models.report

import models.UserRole
import models.UserRole.Admin
import models.UserRole.DGCCRF
import models.report.ReportStatus.LanceurAlerte
import models.report.ReportTag.ReportTagHiddenToProfessionnel
import utils.QueryStringMapper

import java.time.OffsetDateTime
import java.util.UUID
import scala.util.Try

case class ReportFilter(
    departments: Seq[String] = Seq.empty,
    email: Option[String] = None,
    websiteURL: Option[String] = None,
    phone: Option[String] = None,
    siretSirenList: Seq[String] = Seq.empty,
    siretSirenDefined: Option[Boolean] = None,
    companyIds: Seq[UUID] = Seq.empty,
    companyName: Option[String] = None,
    companyCountries: Seq[String] = Seq.empty,
    start: Option[OffsetDateTime] = None,
    end: Option[OffsetDateTime] = None,
    category: Option[String] = None,
    status: Seq[ReportStatus] = Seq.empty,
    details: Option[String] = None,
    employeeConsumer: Option[Boolean] = None,
    contactAgreement: Option[Boolean] = None,
    hasForeignCountry: Option[Boolean] = None,
    hasWebsite: Option[Boolean] = None,
    hasPhone: Option[Boolean] = None,
    hasCompany: Option[Boolean] = None,
    hasAttachment: Option[Boolean] = None,
    withTags: Seq[ReportTag] = Seq.empty,
    withoutTags: Seq[ReportTag] = Seq.empty,
    activityCodes: Seq[String] = Seq.empty
)

object ReportFilter {

  def fromQueryString(q: Map[String, Seq[String]], userRole: UserRole): Try[ReportFilter] = Try {
    val mapper = new QueryStringMapper(q)
    ReportFilter(
      departments = mapper.seq("departments"),
      email = mapper.string("email"),
      websiteURL = mapper.string("websiteURL"),
      phone = mapper.string("phone"),
      siretSirenList = mapper.seq("siretSirenList"),
      companyName = mapper.string("companyName"),
      companyCountries = mapper.seq("companyCountries"),
      // temporary retrocompat, so we can mep the API safely
      start = mapper.timeWithLocalDateRetrocompatStartOfDay("start"),
      end = mapper.timeWithLocalDateRetrocompatEndOfDay("end"),
      category = mapper.string("category"),
      companyIds = mapper.seq("companyIds").map(UUID.fromString),
      status = ReportStatus.filterByUserRole(
        mapper.seq("status").map(ReportStatus.withName),
        userRole
      ),
      details = mapper.string("details"),
      employeeConsumer = userRole match {
        case Admin  => None
        case DGCCRF => None
        case _      => Some(false)
      },
      hasForeignCountry = mapper.boolean("hasForeignCountry"),
      hasWebsite = mapper.boolean("hasWebsite"),
      hasPhone = mapper.boolean("hasPhone"),
      hasCompany = mapper.boolean("hasCompany"),
      hasAttachment = mapper.boolean("hasAttachment"),
      contactAgreement = mapper.boolean("contactAgreement"),
      withTags = mapper.seq("withTags").map(ReportTag.withName),
      withoutTags = mapper.seq("withoutTags").map(ReportTag.withName),
      activityCodes = mapper.seq("activityCodes")
    )
  }

  val allReportsFilter = ReportFilter()

  val transmittedReportsFilter = ReportFilter(
    status = ReportStatus.values.filterNot(_ == LanceurAlerte),
    withoutTags = ReportTagHiddenToProfessionnel,
    siretSirenDefined = Some(true)
  )
}
