package models

import com.mohiva.play.silhouette.api.Identity
import play.api.libs.json._
import utils.EmailAddress
import utils.QueryStringMapper

import java.time.OffsetDateTime
import java.util.UUID
import scala.util.Try

final case class EmailValidation(
    id: UUID = UUID.randomUUID(),
    creationDate: OffsetDateTime = OffsetDateTime.now,
    confirmationCode: String = f"${scala.util.Random.nextInt(1000000)}%06d",
    email: EmailAddress,
    attempts: Int = 0,
    lastAttempt: Option[OffsetDateTime] = None,
    lastValidationDate: Option[OffsetDateTime] = None
) extends Identity {}

object EmailValidation {
  implicit val emailValidationformat: OFormat[EmailValidation] = Json.format[EmailValidation]
}

final case class EmailValidationFilter(
    email: Option[EmailAddress],
    validated: Option[Boolean]
)

object EmailValidationFilter {
  implicit val emailValidationFilterformat: OFormat[EmailValidationFilter] = Json.format[EmailValidationFilter]

  def fromQueryString(q: Map[String, Seq[String]]): Try[EmailValidationFilter] = Try {
    val mapper = new QueryStringMapper(q)
    EmailValidationFilter(
      email = mapper.string("email").map(EmailAddress(_)),
      validated = mapper.boolean("validated")
    )
  }
}
