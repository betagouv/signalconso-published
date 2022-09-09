package models.token

import models.token.TokenKind.CompanyInit
import models.token.TokenKind.CompanyJoin
import models.token.TokenKind.DGCCRFAccount
import models.token.TokenKind.ValidateEmail
import org.specs2.mutable.Specification
import play.api.libs.json.JsString
import play.api.libs.json.Json

class TokenKindTest extends Specification {

  "TokenKind" should {

    "get string value correctly" in {
      TokenKind.CompanyInit.entryName mustEqual "COMPANY_INIT"
      TokenKind.DGCCRFAccount.entryName mustEqual "DGCCRF_ACCOUNT"
      TokenKind.ValidateEmail.entryName mustEqual "VALIDATE_EMAIL"
      TokenKind.CompanyJoin.entryName mustEqual "COMPANY_JOIN"
    }

    "find from value correctly" in {
      TokenKind.withName("COMPANY_INIT") mustEqual CompanyInit
      TokenKind.withName("DGCCRF_ACCOUNT") mustEqual DGCCRFAccount
      TokenKind.withName("VALIDATE_EMAIL") mustEqual ValidateEmail
      TokenKind.withName("COMPANY_JOIN") mustEqual CompanyJoin
    }

    "serialize Json correctly" in {
      Json.toJson(TokenKind.CompanyInit) mustEqual JsString("COMPANY_INIT")
      Json.toJson(TokenKind.DGCCRFAccount) mustEqual JsString("DGCCRF_ACCOUNT")
      Json.toJson(TokenKind.ValidateEmail) mustEqual JsString("VALIDATE_EMAIL")
      Json.toJson(TokenKind.CompanyJoin) mustEqual JsString("COMPANY_JOIN")
    }

    "deserialize Json correctly" in {
      JsString("COMPANY_INIT").as[TokenKind] mustEqual CompanyInit
      JsString("DGCCRF_ACCOUNT").as[TokenKind] mustEqual DGCCRFAccount
      JsString("VALIDATE_EMAIL").as[TokenKind] mustEqual ValidateEmail
      JsString("COMPANY_JOIN").as[TokenKind] mustEqual CompanyJoin
    }

  }

}
