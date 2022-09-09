package models.report

import enumeratum._
import models.UserRole
import models.UserRole.Admin
import models.UserRole.DGCCRF

sealed trait ReportStatus extends EnumEntry

object ReportStatus extends PlayEnum[ReportStatus] {

  val values = findValues

  case object NA extends ReportStatus
  case object LanceurAlerte extends ReportStatus

  /** Not read by pro status
    */
  case object TraitementEnCours extends ReportStatus

  /** Read by pro status
    */
  case object Transmis extends ReportStatus
  case object PromesseAction extends ReportStatus
  case object Infonde extends ReportStatus
  case object NonConsulte extends ReportStatus
  case object ConsulteIgnore extends ReportStatus
  case object MalAttribue extends ReportStatus

  val statusVisibleByPro: Seq[ReportStatus] =
    Seq(
      TraitementEnCours,
      Transmis,
      PromesseAction,
      Infonde,
      NonConsulte,
      ConsulteIgnore,
      MalAttribue
    )

  val statusWithProResponse = Seq(PromesseAction, Infonde, MalAttribue)

  val statusReadByPro = Seq(
    Transmis,
    ConsulteIgnore
  ) ++ statusWithProResponse

  def filterByUserRole(status: Seq[ReportStatus], userRole: UserRole) = {
    val requestedStatus = if (status.isEmpty) ReportStatus.values else status
    userRole match {
      case Admin  => requestedStatus
      case DGCCRF => requestedStatus
      case _      => requestedStatus.intersect(statusVisibleByPro)
    }
  }

  def hasResponse(report: Report): Boolean = statusWithProResponse.contains(report.status)

  def isFinal(status: ReportStatus): Boolean =
    Seq(MalAttribue, ConsulteIgnore, NonConsulte, Infonde, PromesseAction, LanceurAlerte, NA).contains(status)

  def translate(status: ReportStatus, userRole: UserRole): String = {
    def isPro = userRole == UserRole.Professionnel
    status match {
      case NA                => if (isPro) "" else "NA"
      case LanceurAlerte     => if (isPro) "" else "Lanceur d'alerte"
      case TraitementEnCours => if (isPro) "Non consulté" else "Traitement en cours"
      case Transmis          => if (isPro) "À répondre" else "Signalement transmis"
      case PromesseAction    => if (isPro) "Clôturé" else "Promesse action"
      case Infonde           => if (isPro) "Clôturé" else "Signalement infondé"
      case NonConsulte       => if (isPro) "Clôturé" else "Signalement non consulté"
      case ConsulteIgnore    => if (isPro) "Clôturé" else "Signalement consulté ignoré"
      case MalAttribue       => if (isPro) "Clôturé" else "Signalement mal attribué"
    }
  }

  def fromResponseType(reportResponseType: ReportResponseType) =
    reportResponseType match {
      case ReportResponseType.ACCEPTED      => ReportStatus.PromesseAction
      case ReportResponseType.REJECTED      => ReportStatus.Infonde
      case ReportResponseType.NOT_CONCERNED => ReportStatus.MalAttribue
    }
}
