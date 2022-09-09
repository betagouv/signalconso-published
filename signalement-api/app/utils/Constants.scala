package utils

import models.UserRole
import play.api.libs.json.Reads._
import play.api.libs.json._

object Constants {

  object EventType {

    // Valeurs possibles de event_type
    case class EventTypeValue(value: String)

    object EventTypeValue {
      implicit val eventTypeValueWrites = new Writes[EventTypeValue] {
        def writes(eventTypeValue: EventTypeValue) = Json.toJson(eventTypeValue.value)
      }
      implicit val eventTypeValueReads: Reads[EventTypeValue] =
        JsPath.read[String].map(fromValue(_))
    }

    object PRO extends EventTypeValue("PRO")
    object CONSO extends EventTypeValue("CONSO")
    object DGCCRF extends EventTypeValue("DGCCRF")
    object ADMIN extends EventTypeValue("ADMIN")
    object SYSTEM extends EventTypeValue("SYSTEM")

    val eventTypes = Seq(
      PRO,
      CONSO,
      DGCCRF,
      ADMIN,
      SYSTEM
    )

    def fromValue(value: String) = eventTypes.find(_.value == value).getOrElse(EventTypeValue(""))

    def fromUserRole(userRole: UserRole) =
      userRole match {
        case UserRole.Admin         => ADMIN
        case UserRole.DGCCRF        => DGCCRF
        case UserRole.Professionnel => PRO
      }

  }

  object ActionEvent {

    case class ActionEventValue(value: String)

    object ActionEventValue {
      implicit val actionEventValueReads: Reads[ActionEventValue] = JsPath.read[String].map(fromValue(_))
      implicit val actionEventValueWriter = Json.writes[ActionEventValue]
    }

    @Deprecated
    object A_CONTACTER extends ActionEventValue("À contacter")
    @Deprecated
    object HORS_PERIMETRE extends ActionEventValue("Hors périmètre")
    @Deprecated
    object RETOUR_COURRIER extends ActionEventValue("Retour de courrier")
    @Deprecated
    object REPONSE_PRO_CONTACT extends ActionEventValue("Réponse du professionnel au contact")
    @Deprecated
    object EMAIL_NON_PRISE_EN_COMPTE extends ActionEventValue("Envoi email de non prise en compte")

    object POST_ACCOUNT_ACTIVATION_DOC extends ActionEventValue("Envoi du courrier d'activation")
    object ACCOUNT_ACTIVATION extends ActionEventValue("Activation d'un compte")
    object ACTIVATION_DOC_RETURNED extends ActionEventValue("Courrier d'activation retourné")
    object ACTIVATION_DOC_REQUIRED extends ActionEventValue("Courrier d'activation à renvoyer")
    object COMPANY_ADDRESS_CHANGE extends ActionEventValue("Modification de l'adresse de l'entreprise")

    object REPORT_READING_BY_PRO extends ActionEventValue("Première consultation du signalement par le professionnel")
    object REPORT_PRO_RESPONSE extends ActionEventValue("Réponse du professionnel au signalement")
    object REPORT_REVIEW_ON_RESPONSE extends ActionEventValue("Avis du consommateur sur la réponse du professionnel")
    object REPORT_CLOSED_BY_NO_READING extends ActionEventValue("Signalement non consulté")
    object REPORT_CLOSED_BY_NO_ACTION extends ActionEventValue("Signalement consulté ignoré")

    object EMAIL_CONSUMER_ACKNOWLEDGMENT
        extends ActionEventValue("Email « Accusé de réception » envoyé au consommateur")
    object EMAIL_CONSUMER_REPORT_READING
        extends ActionEventValue("Email « Signalement consulté » envoyé au consommateur")
    object EMAIL_CONSUMER_REPORT_RESPONSE
        extends ActionEventValue("Email « L'entreprise a répondu à votre signalement » envoyé au consommateur")
    object EMAIL_CONSUMER_REPORT_CLOSED_BY_NO_READING
        extends ActionEventValue(
          "Email « L'entreprise n'a pas souhaité consulter votre signalement » envoyé au consommateur"
        )
    object EMAIL_CONSUMER_REPORT_CLOSED_BY_NO_ACTION
        extends ActionEventValue("Email « L'entreprise n'a pas répondu au signalement » envoyé au consommateur")

    object EMAIL_PRO_NEW_REPORT extends ActionEventValue("Email « Nouveau signalement » envoyé au professionnel")
    object EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT
        extends ActionEventValue("Email « Accusé de réception de la réponse » envoyé au professionnel")
    object EMAIL_PRO_REMIND_NO_READING
        extends ActionEventValue("Email « Nouveau signalement non consulté » envoyé au professionnel")
    object EMAIL_PRO_REMIND_NO_ACTION
        extends ActionEventValue("Email « Nouveau signalement en attente de réponse » envoyé au professionnel")

    object REPORT_COMPANY_CHANGE extends ActionEventValue("Modification du commerçant")
    object REPORT_CONSUMER_CHANGE extends ActionEventValue("Modification du consommateur")
    object COMMENT extends ActionEventValue("Ajout d'un commentaire")
    object CONSUMER_ATTACHMENTS extends ActionEventValue("Ajout de pièces jointes fournies par le consommateur")
    object PROFESSIONAL_ATTACHMENTS extends ActionEventValue("Ajout de pièces jointes fournies par l'entreprise")
    object CONTROL extends ActionEventValue("Contrôle effectué")

    val actionEvents = Seq(
      A_CONTACTER,
      HORS_PERIMETRE,
      POST_ACCOUNT_ACTIVATION_DOC,
      ACCOUNT_ACTIVATION,
      ACTIVATION_DOC_RETURNED,
      ACTIVATION_DOC_REQUIRED,
      COMPANY_ADDRESS_CHANGE,
      REPORT_READING_BY_PRO,
      REPORT_PRO_RESPONSE,
      REPORT_REVIEW_ON_RESPONSE,
      REPORT_CLOSED_BY_NO_READING,
      REPORT_CLOSED_BY_NO_ACTION,
      EMAIL_CONSUMER_ACKNOWLEDGMENT,
      EMAIL_CONSUMER_REPORT_READING,
      EMAIL_CONSUMER_REPORT_RESPONSE,
      EMAIL_CONSUMER_REPORT_CLOSED_BY_NO_READING,
      EMAIL_CONSUMER_REPORT_CLOSED_BY_NO_ACTION,
      EMAIL_PRO_NEW_REPORT,
      EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT,
      EMAIL_PRO_REMIND_NO_READING,
      EMAIL_PRO_REMIND_NO_ACTION,
      EMAIL_NON_PRISE_EN_COMPTE,
      REPORT_COMPANY_CHANGE,
      REPORT_CONSUMER_CHANGE,
      COMMENT,
      CONSUMER_ATTACHMENTS,
      PROFESSIONAL_ATTACHMENTS,
      CONTROL
    )

    val actionsForUserRole: Map[UserRole, List[ActionEventValue]] =
      Map(
        UserRole.Professionnel -> List(COMMENT),
        UserRole.Admin -> List(COMMENT, CONSUMER_ATTACHMENTS, PROFESSIONAL_ATTACHMENTS),
        UserRole.DGCCRF -> List(COMMENT, CONTROL)
      )

    def fromValue(value: String) = actionEvents.find(_.value == value).getOrElse(ActionEventValue(""))
  }
  object Departments {

    val AURA = List("01", "03", "07", "15", "26", "38", "42", "43", "63", "69", "73", "74")
    val BretagneFrancheComte = List("21", "25", "39", "58", "70", "71", "89", "90")
    val Bretagne = List("22", "29", "35", "56")
    val CDVL = List("18", "28", "36", "37", "41", "45")
    val CollectivitesOutreMer = List("975", "977", "978", "984", "986", "987", "988", "989")
    val Corse = List("2A", "2B")
    val GrandEst = List("08", "10", "51", "52", "54", "55", "57", "67", "68", "88")
    val Guadeloupe = List("971")
    val Guyane = List("973")
    val HautsDeFrance = List("02", "59", "60", "62", "80")
    val IleDeFrance = List("75", "77", "78", "91", "92", "93", "94", "95")
    val LaReunion = List("974")
    val Martinique = List("972")
    val Mayotte = List("976")
    val Normandie = List("14", "27", "50", "61", "76")
    val NouvelleAquitaine = List("16", "17", "19", "23", "24", "33", "40", "47", "64", "79", "86", "87")
    val OCC = List("09", "11", "12", "30", "31", "32", "34", "46", "48", "65", "66", "81", "82")
    val PaysDeLaLoire = List("44", "49", "53", "72", "85")
    val ProvenceAlpesCoteAzur = List("04", "05", "06", "13", "83", "84")

    val METROPOLE = AURA ++ BretagneFrancheComte ++ Bretagne ++ CDVL ++ Corse ++ GrandEst ++ HautsDeFrance ++
      IleDeFrance ++ Normandie ++ NouvelleAquitaine ++ OCC ++ PaysDeLaLoire ++ ProvenceAlpesCoteAzur
    val DOM_TOM = CollectivitesOutreMer ++ Guadeloupe ++ Guyane ++ LaReunion ++ Martinique ++ Mayotte

    val ALL = METROPOLE ++ DOM_TOM

    def fromPostalCode(postalCode: String) =
      postalCode match {
        case "97150"                        => Some("978")
        case "97133"                        => Some("977")
        case code if code.startsWith("200") => Some("2A")
        case code if code.startsWith("201") => Some("2A")
        case code if code.startsWith("202") => Some("2B")
        case code if code.startsWith("204") => Some("2B")
        case code if code.startsWith("206") => Some("2B")
        case _                              => Departments.ALL.find(postalCode.startsWith)
      }

    def toPostalCode(postalCode: String) =
      postalCode match {
        case "978" => Seq("978", "97150")
        case "977" => Seq("977", "97133")
        case "2A"  => Seq("200", "201", "2A")
        case "2B"  => Seq("202", "204", "206", "2B")
        case other => Seq(other)
      }
  }
}
