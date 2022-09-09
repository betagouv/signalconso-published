package models.token

import controllers.error.AppError.CantPerformAction
import models.UserRole
import models.logger
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import utils.EmailAddress

import java.time.OffsetDateTime

case class DGCCRFAccessToken private (
    tokenCreation: OffsetDateTime,
    token: String,
    email: Option[EmailAddress],
    tokenExpirationDate: Option[OffsetDateTime]
)

object DGCCRFAccessToken {
  def apply(
      tokenCreation: OffsetDateTime,
      token: String,
      email: Option[EmailAddress],
      tokenExpirationDate: Option[OffsetDateTime],
      userRole: UserRole
  ): DGCCRFAccessToken =
    userRole match {
      case UserRole.Admin => new DGCCRFAccessToken(tokenCreation, token, email, tokenExpirationDate)
      case _ =>
        logger.error(s"DGCCRF token accessed by unexpected user with role $userRole")
        throw CantPerformAction
    }

  implicit val DGCCRFAccessTokenFormat: OFormat[DGCCRFAccessToken] = Json.format[DGCCRFAccessToken]
}
