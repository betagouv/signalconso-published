package models

import models.report.DetailInputValue
import org.specs2.mutable.Specification
import models.report.DetailInputValue._

class DetailInputValueTest extends Specification {

  "DetailInputValueTest" should {

    "string2detailInputValue" in {
      toDetailInputValue("label : value") must equalTo(DetailInputValue("label :", "value"))
      toDetailInputValue("value") must equalTo(DetailInputValue("Précision :", "value"))
    }

    "detailInputValue2String" in {
      detailInputValuetoString(DetailInputValue("label :", "value")) must equalTo("label : value")
      detailInputValuetoString(DetailInputValue("Précision :", "value")) must equalTo("value")
    }

  }
}
