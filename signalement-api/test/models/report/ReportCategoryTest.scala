package models.report

import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments
import ReportCategory._
import controllers.error.AppError.MalformedValue
import play.api.libs.json.Json

import java.util.UUID

class ReportCategoryTest extends Specification {

  "ReportTagTest" should {

    Fragments.foreach(
      ReportCategory.values
    ) { v =>
      s"parse json from value ${v.entryName}" in {
        v shouldEqual (Json.toJson(v).as[ReportCategory])
      }

      s"retreive from entryName ${v.entryName}" in {
        v shouldEqual fromValue(v.entryName)
      }

    }

    "Failed when passing unvalid entryName" in {
      fromValue(UUID.randomUUID().toString) must throwA[MalformedValue]
    }

  }

}
