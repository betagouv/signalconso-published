package models.token

import models.token.TokenKind.DGCCRFAccount
import org.specs2.mutable.Specification
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import utils.EmailAddress
import utils.SIRET

import java.util.UUID

class TokenInfoTest extends Specification {

  "TokenInfo" should {

    "serialize DGCCRFUserActivationToken Json correctly" in {

      val dGCCRFUserActivationToken = DGCCRFUserActivationToken(
        token = UUID.randomUUID().toString,
        emailedTo = EmailAddress("test@dgccrf.gouv.fr"),
        kind = DGCCRFAccount
      )

      Json.toJson(dGCCRFUserActivationToken) mustEqual
        JsObject(
          Seq(
            "token" -> Json.toJson(dGCCRFUserActivationToken.token),
            "emailedTo" -> Json.toJson(dGCCRFUserActivationToken.emailedTo),
            "kind" -> Json.toJson(dGCCRFUserActivationToken.kind)
          )
        )

    }

    "serialize CompanyUserActivationToken Json correctly" in {

      val companyUserActivationToken = CompanyUserActivationToken(
        token = UUID.randomUUID().toString,
        emailedTo = EmailAddress("test@dgccrf.gouv.fr"),
        companySiret = SIRET.fromUnsafe("XXXXXXXXXXXXXX"),
        kind = DGCCRFAccount
      )

      Json.toJson(companyUserActivationToken) mustEqual
        JsObject(
          Seq(
            "token" -> Json.toJson(companyUserActivationToken.token),
            "emailedTo" -> Json.toJson(companyUserActivationToken.emailedTo),
            "kind" -> Json.toJson(companyUserActivationToken.kind),
            "companySiret" -> Json.toJson(companyUserActivationToken.companySiret)
          )
        )

    }

  }

}
