package utils

object EmailSubjects {
  val RESET_PASSWORD = "Votre mot de passe SignalConso"
  val NEW_COMPANY_ACCESS = (companyName: String) =>
    s"Vous avez maintenant accès à l'entreprise ${companyName} sur SignalConso"
  val COMPANY_ACCESS_INVITATION = (companyName: String) => s"Rejoignez l'entreprise ${companyName} sur SignalConso"
  val DGCCRF_ACCESS_LINK = "Votre accès DGCCRF sur SignalConso"
  val VALIDATE_EMAIL = "Veuillez valider cette adresse email"
  val NEW_REPORT = "Nouveau signalement"
  val REPORT_ACK = "Votre signalement"
  val REPORT_TRANSMITTED = "L'entreprise a pris connaissance de votre signalement"
  val REPORT_ACK_PRO = "Votre réponse au signalement"
  val REPORT_ACK_PRO_CONSUMER = "L'entreprise a répondu à votre signalement, donnez nous votre avis sur sa réponse"
  val REPORT_UNREAD_REMINDER = "Nouveau signalement"
  val REPORT_TRANSMITTED_REMINDER = "Signalement en attente de réponse"
  val REPORT_CLOSED_NO_READING = "L'entreprise n'a pas souhaité consulter votre signalement"
  val REPORT_CLOSED_NO_ACTION = "L'entreprise n'a pas répondu au signalement"
  val REPORT_NOTIF_DGCCRF = (cnt: Int, additional: Option[String]) =>
    s"[SignalConso] ${additional.getOrElse("")}${if (cnt > 1) s"${cnt} nouveaux signalements"
      else "Un nouveau signalement"}"
}
