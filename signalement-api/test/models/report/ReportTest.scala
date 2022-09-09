package models.report

import models.report.ReportTag.ReportTagHiddenToProfessionnel
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments
import utils.Fixtures
import utils.SIRET

class ReportTest extends Specification {
  "ReportTest" should {

    val aReport = Fixtures.genReportForCompany(Fixtures.genCompany.sample.get).sample.get

    s"initialStatus should be ${ReportStatus.TraitementEnCours} when company identified is set to true and report tag not in ${ReportTagHiddenToProfessionnel}" in {
      val allTagExceptReportTagHiddenToProfessionnel: List[ReportTag] =
        ReportTag.values.filterNot(t => ReportTagHiddenToProfessionnel.contains(t)).toList

      val companyVisibleReport =
        aReport.copy(
          tags = allTagExceptReportTagHiddenToProfessionnel,
          employeeConsumer = false,
          companySiret = Some(SIRET("11111111111111"))
        )

      companyVisibleReport.initialStatus() shouldEqual (ReportStatus.TraitementEnCours)
    }

    s"initialStatus should be ${ReportStatus.LanceurAlerte} when employeeCustomer is set to true" in {
      val employeeCustomerReport = aReport.copy(employeeConsumer = true)
      employeeCustomerReport.initialStatus() shouldEqual (ReportStatus.LanceurAlerte)
    }

    s"initialStatus should be ${ReportStatus.NA} when company has not been identified" in {
      val unknownCompanyReport = aReport.copy(companySiret = None)
      unknownCompanyReport.initialStatus() shouldEqual (ReportStatus.NA)
    }

    Fragments.foreach(
      ReportTagHiddenToProfessionnel
    ) { tag =>
      s"initialStatus should be ${ReportStatus.NA} when company has been identified but report tag is $tag" in {
        val unknownCompanyReport = aReport.copy(companySiret = Some(SIRET("11111111111111")), tags = List(tag))
        unknownCompanyReport.initialStatus() shouldEqual (ReportStatus.NA)
      }
    }

    Fragments.foreach(
      ReportTagHiddenToProfessionnel
    ) { tag =>
      s"initialStatus should be ${ReportStatus.NA} when report tag is $tag" in {
        val NATagReport = aReport.copy(tags = List(tag))
        NATagReport.initialStatus() shouldEqual (ReportStatus.NA)
      }
    }

  }
}
