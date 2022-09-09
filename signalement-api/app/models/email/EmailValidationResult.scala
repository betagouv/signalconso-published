package models.email

import play.api.libs.json.Json
import play.api.libs.json.OFormat

case class EmailValidationResult(valid: Boolean, reason: Option[String] = None)

object EmailValidationResult {
  implicit val EmailValidationResultResultFormat: OFormat[EmailValidationResult] = Json.format[EmailValidationResult]

  def success: EmailValidationResult = EmailValidationResult(true)
  def failure: EmailValidationResult = EmailValidationResult(false)
  def invalidCode: EmailValidationResult = EmailValidationResult(false, Some("INVALID_CODE"))
}
