package controllers.error

import models.report.Report
import models.report.reportfile.ReportFileId
import models.website.WebsiteId
import utils.EmailAddress
import utils.SIRET

import java.util.UUID
import scala.util.control.NoStackTrace

sealed trait AppError extends Throwable with Product with Serializable with NoStackTrace {
  val `type`: String
  val title: String
  val details: String
}

sealed trait UnauthorizedError extends AppError
sealed trait NotFoundError extends AppError
sealed trait BadRequestError extends AppError
sealed trait ForbiddenError extends AppError
sealed trait ConflictError extends AppError
sealed trait InternalAppError extends AppError
sealed trait PreconditionError extends AppError

object AppError {

  final case class ServerError(message: String, cause: Option[Throwable] = None) extends InternalAppError {
    override val `type`: String = "SC-0001"
    override val title: String = message
    override val details: String = "Une erreur inattendue s'est produite."
  }

  final case class DGCCRFActivationTokenNotFound(token: String) extends NotFoundError {
    override val `type`: String = "SC-0002"
    override val title: String = s"DGCCRF user token $token not found"
    override val details: String = s"Le lien d'activation n'est pas valide ($token). Merci de contacter le support"
  }

  final case class CompanyActivationTokenNotFound(token: String, siret: SIRET) extends NotFoundError {
    override val `type`: String = "SC-0003"
    override val title: String = s"Company user token $token with siret ${siret.value} not found"
    override val details: String = s"Le lien d'activation ($token) n'est pas valide. Merci de contacter le support"
  }

  final case class CompanySiretNotFound(siret: SIRET) extends NotFoundError {
    override val `type`: String = "SC-0004"
    override val title: String = s"Company siret not found"
    override val details: String = s"Le SIRET ${siret.value} ne correspond à aucune entreprise connue"
  }

  final case class WebsiteNotFound(websiteId: WebsiteId) extends NotFoundError {
    override val `type`: String = "SC-0005"
    override val title: String = s"Website ${websiteId.value.toString} not found"
    override val details: String = "L'association site internet n'existe pas."
  }

  final case class WebsiteHostIsAlreadyIdentified(
      host: String,
      companyId: Option[UUID] = None,
      country: Option[String] = None
  ) extends BadRequestError {
    override val `type`: String = "SC-0006"
    override val title: String = s"Website ${host} already associated to a country or company"
    override val details: String =
      s"Le site internet ${host} est déjà associé à un autre pays ou une entreprise"
  }

  final case class MalformedHost(host: String) extends BadRequestError {
    override val `type`: String = "SC-0007"
    override val title: String = "Malformed host"
    override val details: String =
      s"Le site $host doit être un site valide."
  }

  final case class InvalidDGCCRFEmail(email: EmailAddress, suffix: String) extends ForbiddenError {
    override val `type`: String = "SC-0008"
    override val title: String = "Invalid Dgccrf email"
    override val details: String =
      s"Email ${email.value} invalide. Email acceptés : *${suffix}"
  }

  final case object UserAccountEmailAlreadyExist extends BadRequestError {
    override val `type`: String = "SC-0009"
    override val title: String = "User already exist"
    override val details: String =
      s"Ce compte existe déjà. Merci de demander à l'utilisateur de regénérer son mot de passe pour se connecter"
  }

  final case class TooMuchAuthAttempts(userId: UUID) extends ForbiddenError {
    override val `type`: String = "SC-0010"
    override val title: String = s"Max auth attempt reached for user id : $userId"
    override val details: String =
      "Le nombre maximum de tentative d'authentification a été dépassé, merci de rééssayer un peu plus tard."
  }

  final case class InvalidPassword(login: String) extends UnauthorizedError {
    override val `type`: String = "SC-0011"
    override val title: String = "Invalid password"
    override val details: String =
      s"Mot de passe invalide pour $login"
  }

  final case class UserNotFound(login: String) extends UnauthorizedError {
    override val `type`: String = "SC-0012"
    override val title: String = "Cannot perform action on user"
    override val details: String =
      s"Action non autorisée pour $login"
  }

  final case class DGCCRFUserEmailValidationExpired(login: String) extends ForbiddenError {
    override val `type`: String = "SC-0013"
    override val title: String = "DGCCRF user needs email revalidation"
    override val details: String =
      s"Votre compte DGCCRF a besoin d'être revalidé, un email vous a été envoyé pour réactiver votre compte."
  }

  final case object MalformedBody extends BadRequestError {
    override val `type`: String = "SC-0014"
    override val title: String = "Malformed request body"
    override val details: String = s"Le corps de la requête ne correspond pas à ce qui est attendu par l'API."
  }

  final case class PasswordTokenNotFoundOrInvalid(token: UUID) extends NotFoundError {
    override val `type`: String = "SC-0015"
    override val title: String = s"Token not found / invalid ${token.toString}"
    override val details: String =
      s"Lien invalide ou expiré, merci de recommencer la demande de changement de mot de passe."
  }

  final case class AccountActivationTokenNotFoundOrInvalid(token: String) extends NotFoundError {
    override val `type`: String = "SC-0015"
    override val title: String = s"Account activation token not found / invalid ${token}"
    override val details: String =
      s"Lien invalide ou expiré, merci de recommencer la procédure d'activation du compte."
  }

  final case object EmailAlreadyExist extends ConflictError {
    override val `type`: String = "SC-0016"
    override val title: String = s"Email already exists"
    override val details: String =
      s"L'adresse email existe déjà."
  }

  final case object SamePasswordError extends BadRequestError {
    override val `type`: String = "SC-0016"
    override val title: String = s"New password is equal to old password"
    override val details: String =
      s"Le nouveau mot de passe ne peut pas être le même que l'ancien mot de passe"
  }

  /** Error message is not precice on purpose, to prevent third party to crawl SIRET / CODE from our API
    */
  final case class CompanyActivationSiretOrCodeInvalid(siret: SIRET) extends NotFoundError {
    override val `type`: String = "SC-0017"
    override val title: String = s"Unable to activate company"
    override val details: String =
      s"Impossible d'activer l'entreprise (siret : ${siret.value}), merci de vérifier que le siret et le code d'activation correspondent bien à ceux indiqués sur le courrier."
  }

  final case class CompanyActivationCodeExpired(siret: SIRET) extends BadRequestError {
    override val `type`: String = "SC-0018"
    override val title: String = s"Unable to activate company, code expired"
    override val details: String =
      s"Impossible d'activer l'entreprise (siret : ${siret.value}) car le code a expiré, merci de contacter le support."
  }

  final case class ActivationCodeAlreadyUsed(email: EmailAddress) extends ConflictError {
    override val `type`: String = "SC-0019"
    override val title: String = s"Unable to activate company, code already used"
    override val details: String =
      s"Compte déjà activé, merci de vous connecter avec l'adresse ${email.value} ( vous pouvez recréer un mot de passe en cliquant sur 'MOT DE PASSE OUBLIÉ' sur la page de connexion.)"
  }

  final case class InvalidEmail(email: String) extends BadRequestError {
    override val `type`: String = "SC-0020"
    override val title: String = "Invalid email"
    override val details: String =
      s"Email ${email} est invalide."
  }

  final case object InvalidEmailProvider extends BadRequestError {
    override val `type`: String = "SC-0021"
    override val title: String = "Invalid email provider"
    override val details: String =
      s"Les adresses email temporaires sont interdites."
  }

  /** Error message is not precice on purpose to prevent third parties for sniffing emails
    */
  final case class EmailOrCodeIncorrect(email: EmailAddress) extends NotFoundError {
    override val `type`: String = "SC-0020"
    override val title: String = s"Email or code incorrect"
    override val details: String =
      s"Impossible de valider l'email ${email}, code ou email incorrect."
  }

  final case class SpammerEmailBlocked(email: EmailAddress) extends NotFoundError {
    override val `type`: String = "SC-0020"
    override val title: String = s"Email blocked, report submission ignored"
    override val details: String =
      s"L'email ${email.value} est bloquée car listée comme spam"
  }

  final case object ReportCreationInvalidBody extends BadRequestError {
    override val `type`: String = "SC-0021"
    override val title: String = s"Report's body does not match specific constraints"
    override val details: String = s"Le signalement est invalide"
  }

  final case class InvalidReportTagBody(name: String) extends BadRequestError {
    override val `type`: String = "SC-0022"
    override val title: String = s"Unknown report tag $name"
    override val details: String = s"Le tag $name est invalide. Merci de fournir une valeur correcte."
  }

  final case class ExternalReportsMaxPageSizeExceeded(maxSize: Int) extends BadRequestError {
    override val `type`: String = "SC-0024"
    override val title: String = s"Max page size reached "
    override val details: String =
      s"Le nombre d'entrée par page demandé est trop élevé. Il doit être inférieur ou égal à $maxSize"
  }

  final case class DuplicateReportCreation(reportList: List[Report]) extends BadRequestError {
    override val `type`: String = "SC-0025"
    override val title: String = s"Same report has already been created with id ${reportList.map(_.id).mkString(",")}"
    override val details: String =
      s"Il existe un ou plusieurs signalements similaire"
  }

  final case object MalformedQueryParams extends BadRequestError {
    override val `type`: String = "SC-0026"
    override val title: String = "Malformed request query params"
    override val details: String = s"Le paramètres de la requête ne correspondent pas à ce qui est attendu par l'API."
  }

  final case class AttachmentNotReady(reportFileId: ReportFileId) extends ConflictError {
    override val `type`: String = "SC-0027"
    override val title: String = "Attachement not available"
    override val details: String =
      s"Le fichier [id = ${reportFileId.value.toString}] n'est pas encore disponible au téléchargement, veuillez réessayer plus tard."
  }

  final case class AttachmentNotFound(reportFileId: ReportFileId, reportFileName: String) extends NotFoundError {
    override val `type`: String = "SC-0028"
    override val title: String = "Cannot download attachment"
    override val details: String =
      s"Impossible de récupérer le fichier [id = ${reportFileId.value.toString}, nom = $reportFileName]"
  }

  final case class BucketFileNotFound(bucketName: String, fileName: String) extends NotFoundError {
    override val `type`: String = "SC-0029"
    override val title: String = "Cannot download file from S3"
    override val details: String =
      s"Impossible de récupérer le fichier $fileName sur le bucket $bucketName"
  }

  final case class CannotReviewReportResponse(reportId: UUID) extends ForbiddenError {
    override val `type`: String = "SC-0030"
    override val title: String = "Cannot review response for report"
    override val details: String =
      s"Impossible de donner un avis sur la réponse donnée au signalement ${reportId.toString}"
  }

  final case class MalformedId(id: String) extends BadRequestError {
    override val `type`: String = "SC-0031"
    override val title: String = "Malformed id"
    override val details: String =
      s"Malformed id : $id"
  }

  final case object ReviewAlreadyExists extends ForbiddenError {
    override val `type`: String = "SC-0032"
    override val title: String = "Review already exists for the report response."
    override val details: String =
      s"Un avis existe déjà pour la réponse de l'entreprise."
  }

  final case class ReportNotFound(reportId: UUID) extends NotFoundError {
    override val `type`: String = "SC-0033"
    override val title: String = s"Report with id ${reportId.toString} not found"
    override val details: String =
      s"Signalement avec id ${reportId.toString} introuvable"
  }

  final case class MalformedSIRET(InvalidSIRET: String) extends BadRequestError {
    override val `type`: String = "SC-0034"
    override val title: String = "Malformed SIRET"
    override val details: String =
      s"Malformed SIRET : $InvalidSIRET"
  }

  final case class CompanyNotFound(companyId: UUID) extends NotFoundError {
    override val `type`: String = "SC-0035"
    override val title: String = s"Company with id ${companyId.toString} not found"
    override val details: String =
      s"Entreprise avec id ${companyId.toString} introuvable"
  }

  final case object CantPerformAction extends ForbiddenError {
    override val `type`: String = "SC-0036"
    override val title: String = s"Access forbidden"
    override val details: String =
      s"L'action demandée n'est pas autorisée."
  }

  final case class InvalidFileExtension(currentExtension: String, validExtensions: Seq[String])
      extends BadRequestError {
    override val `type`: String = "SC-0037"
    override val title: String = s"Invalid file extension"
    override val details: String =
      s"Impossible de charger un fichier avec l'extension '.$currentExtension', extensions valides : ${validExtensions
          .mkString("'", "' , '", "'")}"
  }

  final case class MalformedFileKey(key: String) extends BadRequestError {
    override val `type`: String = "SC-0038"
    override val title: String = "Malformed file key"
    override val details: String =
      s"Cannot find file with key $key"
  }

  final case class WebsiteNotIdentified(host: String) extends BadRequestError {
    override val `type`: String = "SC-0039"
    override val title: String = s"Website must be associated to identify or update investigation"
    override val details: String =
      s"Le site $host doit être associé à une entreprise ou un pays pour l'identifier ou modifier l'enquête"
  }

  final case class CannotDeleteWebsite(host: String) extends BadRequestError {
    override val `type`: String = "SC-0040"
    override val title: String = s"Website must not be under investigation or identified"
    override val details: String =
      s"Impossible de supprimer le site. Vérifiez que le site ne soit pas identifié ou qu'il ne fasse pas l'objet d'une enquête / affectation"
  }

  final case object CannotReportPublicAdministration extends BadRequestError {
    override val `type`: String = "SC-0041"
    override val title: String = s"Cannot report public administration"
    override val details: String =
      s"Impossible de signaler une administration publique"
  }

  final case class MalformedValue(value: String, expectedValidType: String) extends BadRequestError {
    override val `type`: String = "SC-0042"
    override val title: String = s"Malformed value, $value is not a valid value, expecting valid $expectedValidType"
    override val details: String =
      s"La valeur $value ne correspond pas à ce qui est attendu par l'API. Merci de renseigner une valeur valide pour $expectedValidType"
  }

}
