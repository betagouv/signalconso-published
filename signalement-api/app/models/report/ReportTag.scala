package models.report

import controllers.error.AppError.InvalidReportTagBody
import enumeratum.EnumEntry
import enumeratum.PlayEnum
import play.api.Logger
import play.api.libs.json.JsError
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.Reads

sealed trait ReportTag extends EnumEntry

object ReportTag extends PlayEnum[ReportTag] {

  private[this] val logger = Logger(this.getClass)

  val values: IndexedSeq[ReportTag] = findValues

  override def withName(name: String): ReportTag =
    ReportTag.withNameOption(name).getOrElse(throw InvalidReportTagBody(name))

  /** Used as workaround to parse values from their translation as signalement-app is pushing translation instead of
    * entry name Make sure no translated values is passed as ReportTag to remove this reads
    */
  val TranslationReportTagReads: Reads[ReportTag] = Reads[ReportTag] {
    case JsString(s) =>
      withNameOption(s).orElse {
        ReportTag.values
          .find { v =>
            if (v.translate() == s) {
              logger.warn(s"Parsing report tag from translated value $s")
              true
            } else false
          }
      } match {
        case Some(obj) => JsSuccess(obj)
        case None      => JsError("error.expected.validenumvalue")
      }
    case _ => JsError("error.expected.enumstring")
  }

  val ReportTagHiddenToProfessionnel = Seq(ReportTag.Bloctel, ReportTag.ReponseConso, ReportTag.ProduitDangereux)

  case object LitigeContractuel extends ReportTag
  case object Hygiene extends ReportTag
  case object ProduitDangereux extends ReportTag
  case object DemarchageADomicile extends ReportTag
  case object Ehpad extends ReportTag
  case object DemarchageTelephonique extends ReportTag
  case object AbsenceDeMediateur extends ReportTag
  case object Bloctel extends ReportTag
  case object Influenceur extends ReportTag
  case object ReponseConso extends ReportTag
  case object Internet extends ReportTag
  case object ProduitIndustriel extends ReportTag
  case object ProduitAlimentaire extends ReportTag
  case object CompagnieAerienne extends ReportTag

  implicit class ReportTagTranslationOps(reportTag: ReportTag) {

    def translate(): String = reportTag match {
      case LitigeContractuel      => "Litige contractuel"
      case Hygiene                => "hygiène"
      case ProduitDangereux       => "Produit dangereux"
      case DemarchageADomicile    => "Démarchage à domicile"
      case Ehpad                  => "Ehpad"
      case DemarchageTelephonique => "Démarchage téléphonique"
      case AbsenceDeMediateur     => "Absence de médiateur"
      case Bloctel                => "Bloctel"
      case Influenceur            => "Influenceur"
      case ReponseConso           => "ReponseConso"
      case Internet               => "Internet"
      case ProduitIndustriel      => "Produit industriel"
      case ProduitAlimentaire     => "Produit alimentaire"
      case CompagnieAerienne      => "Compagnie aerienne"
    }
  }

}
