package models

import org.specs2.mutable.Specification
//import play.api.libs.json.Json

import scala.util.Try

class UserRoleTest extends Specification {

  "UserRole" should {

    "get value from string name" in {

      UserRole.withName("DGCCRF") shouldEqual UserRole.DGCCRF
      UserRole.withName("Admin") shouldEqual UserRole.Admin
      UserRole.withName("Professionnel") shouldEqual UserRole.Professionnel
      Try(UserRole.withName("XXXXXXXXXX")).isFailure shouldEqual true
    }

  }

}
