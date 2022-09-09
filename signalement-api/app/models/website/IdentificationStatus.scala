package models.website

import controllers.error.AppError.MalformedQueryParams
import enumeratum.EnumEntry
import enumeratum.PlayEnum

sealed trait IdentificationStatus extends EnumEntry

object IdentificationStatus extends PlayEnum[IdentificationStatus] {

  val values: IndexedSeq[IdentificationStatus] = findValues

  // For backward compatibility, to be removed
  def withNameRetroCompatibility(string: String) = string match {
    case "DEFAULT" => Identified
    case "PENDING" => NotIdentified
    case _         => throw MalformedQueryParams
  }
  // For backward compatibility, to be removed
  def toKind(identificationStatus: IdentificationStatus) = identificationStatus match {
    case Identified    => "DEFAULT"
    case NotIdentified => "PENDING"
  }

  override def withName(name: String): IdentificationStatus =
    super.withNameOption(name).getOrElse(withNameRetroCompatibility(name))

  final case object Identified extends IdentificationStatus
  final case object NotIdentified extends IdentificationStatus
}
