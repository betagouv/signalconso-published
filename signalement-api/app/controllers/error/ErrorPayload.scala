package controllers.error

import play.api.libs.json.Format
import play.api.libs.json.Json

final case class ErrorPayload(`type`: String, title: String, details: String)

object ErrorPayload {
  def apply(error: AppError): ErrorPayload = ErrorPayload(error.`type`, error.title, error.details)

  val AuthenticationErrorPayload = ErrorPayload(
    "SC-AUTH",
    "Cannot authenticate user",
    """ Email ou mot de passe incorrect. Si vous avez oublié votre mot de passe, cliquez sur 'mot de passe oublié' pour le récupérer.""".stripMargin
  )

  implicit val ErrorPayloadFormat: Format[ErrorPayload] = Json.format[ErrorPayload]
}
