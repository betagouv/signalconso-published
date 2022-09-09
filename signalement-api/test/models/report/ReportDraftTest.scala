package models.report

import io.scalaland.chimney.dsl.TransformerOps
import models.Address
import org.specs2.mutable.Specification
import utils.Country
import utils.Fixtures
import utils.SIRET
import utils.URL

import java.time.OffsetDateTime
import java.util.UUID

class ReportDraftTest extends Specification {

  "ReportDraftTest" should {

    "generateReport should return report with default value" in {

      val aDraftReport = Fixtures.genDraftReport.sample.get.copy(
        companyAddress = None,
        forwardToReponseConso = None,
        employeeConsumer = true,
        tags = List(ReportTag.LitigeContractuel),
        reponseconsoCode = None,
        ccrfCode = None,
        companyActivityCode = Some("40.7Z")
      )

      val companyId = None
      val reportId = UUID.randomUUID()
      val creationDate: OffsetDateTime = OffsetDateTime.now()

      val res =
        aDraftReport.generateReport(maybeCompanyId = companyId, creationDate = creationDate, reportId = reportId)

      val expectedReport = aDraftReport
        .into[Report]
        .withFieldConst(_.forwardToReponseConso, false)
        .withFieldConst(_.ccrfCode, Nil)
        .withFieldComputed(_.websiteURL, r => WebsiteURL(r.websiteURL, r.websiteURL.flatMap(_.getHost)))
        .withFieldConst(_.reponseconsoCode, Nil)
        .withFieldConst(_.tags, Nil)
        .withFieldConst(_.companyAddress, Address())
        .withFieldConst(_.companyId, companyId)
        .withFieldConst(_.id, reportId)
        .withFieldConst(_.creationDate, creationDate)
        .withFieldConst(_.status, ReportStatus.LanceurAlerte)
        .transform

      res shouldEqual expectedReport
    }

    val anInvalidDraftReport = Fixtures.genDraftReport.sample.get.copy(
      companySiret = None,
      websiteURL = None,
      companyAddress = None,
      tags = Nil,
      phone = None
    )

    "generateReport should be invalid" in {
      ReportDraft.isValid(anInvalidDraftReport) shouldEqual false
    }

    "generateReport should be valid when phone is defined" in {
      val aColdCallingDraftReport = anInvalidDraftReport.copy(
        phone = Some("0651445522")
      )
      ReportDraft.isValid(aColdCallingDraftReport) shouldEqual true
    }

    "generateReport should be valid when company siret is defined" in {
      val anIdentifiedCompanyDraftReport = anInvalidDraftReport.copy(
        companySiret = Some(SIRET("11111111451212"))
      )
      ReportDraft.isValid(anIdentifiedCompanyDraftReport) shouldEqual true
    }

    "generateReport should be valid when company siret is defined" in {
      val anInternetDraftReport = anInvalidDraftReport.copy(
        websiteURL = Some(URL("http://badcompany.com"))
      )
      ReportDraft.isValid(anInternetDraftReport) shouldEqual true
    }

    "generateReport should fail when reporting influencer without postal code" in {
      val anInfluencerDraftReport = anInvalidDraftReport.copy(
        tags = List(ReportTag.Influenceur)
      )
      ReportDraft.isValid(anInfluencerDraftReport) shouldEqual false
    }

    "generateReport should be valid when reporting influencer" in {
      val anInfluencerDraftReport = anInvalidDraftReport.copy(
        tags = List(ReportTag.Influenceur),
        companyAddress = Some(Address(postalCode = Some("75000")))
      )
      ReportDraft.isValid(anInfluencerDraftReport) shouldEqual true
    }

    "generateReport should be valid when reporting foreign country" in {
      val aForeignCountryDraftReport = anInvalidDraftReport.copy(
        companyAddress = Some(Address(country = Some(Country.Inde)))
      )
      ReportDraft.isValid(aForeignCountryDraftReport) shouldEqual true
    }

    "generateReport should be valid when reporting with postal code" in {
      val aPostalCodeDraftReport = anInvalidDraftReport.copy(
        companyAddress = Some(Address(postalCode = Some("888888")))
      )
      ReportDraft.isValid(aPostalCodeDraftReport) shouldEqual true
    }

  }
}
