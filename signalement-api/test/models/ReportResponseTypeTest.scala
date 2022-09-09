package models

import models.report.ReportResponseType
import org.specs2.mutable.Specification
import play.api.libs.json.JsString
import play.api.libs.json.Json

import scala.util.Try

class ReportResponseTypeTest extends Specification {

  "ReportResponseType" should {

    "get value from string name" in {
      ReportResponseType.withName("ACCEPTED") shouldEqual ReportResponseType.ACCEPTED
      ReportResponseType.withName("REJECTED") shouldEqual ReportResponseType.REJECTED
      ReportResponseType.withName("NOT_CONCERNED") shouldEqual ReportResponseType.NOT_CONCERNED
      Try(ReportResponseType.withName("XXXXXXXXXX")).isFailure shouldEqual true
    }

    "parse json as expected" in {

      val ACCEPTEDJson = JsString("ACCEPTED")
      val REJECTEDJson = JsString("REJECTED")
      val NOT_CONCERNEDJson = JsString("NOT_CONCERNED")
      val unknownJson = JsString("XXXXXX")

      ACCEPTEDJson.as[ReportResponseType] shouldEqual ReportResponseType.ACCEPTED
      REJECTEDJson.as[ReportResponseType] shouldEqual ReportResponseType.REJECTED
      NOT_CONCERNEDJson.as[ReportResponseType] shouldEqual ReportResponseType.NOT_CONCERNED
      Try(unknownJson.as[ReportResponseType]).isFailure shouldEqual true
    }

    "write json as expected" in {
      Json.toJson[ReportResponseType](ReportResponseType.ACCEPTED) shouldEqual JsString("ACCEPTED")
      Json.toJson[ReportResponseType](ReportResponseType.REJECTED) shouldEqual JsString("REJECTED")
      Json.toJson[ReportResponseType](ReportResponseType.NOT_CONCERNED) shouldEqual JsString("NOT_CONCERNED")

    }

  }

}
