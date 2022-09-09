package models

import java.util.UUID
import scala.util.Failure
import scala.util.Success
import scala.util.Try
sealed trait SearchCompanyIdentity

object SearchCompanyIdentity {
  case class SearchCompanyIdentityRCS(value: String) extends SearchCompanyIdentity

  case class SearchCompanyIdentitySiret(value: String) extends SearchCompanyIdentity

  case class SearchCompanyIdentitySiren(value: String) extends SearchCompanyIdentity

  case class SearchCompanyIdentityName(value: String) extends SearchCompanyIdentity

  case class SearchCompanyIdentityId(value: UUID) extends SearchCompanyIdentity

  object SearchCompanyIdentityId {

    def unapply(value: String): Option[UUID] =
      Try(UUID.fromString(value)) match {
        case Success(uuid) => Some(uuid)
        case Failure(_)    => None
      }
  }

  def fromString(identity: String): SearchCompanyIdentity = {
    val trimmedIdentity = identity.replaceAll("\\s", "")
    trimmedIdentity match {
      case q if q.matches("[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}") => SearchCompanyIdentityRCS(q.toLowerCase())
      case q if q.matches("[0-9]{14}")                     => SearchCompanyIdentitySiret(q)
      case q if q.matches("[0-9]{9}")                      => SearchCompanyIdentitySiren(q)
      case SearchCompanyIdentityId(uuid)                   => SearchCompanyIdentityId(uuid)
      case _                                               => SearchCompanyIdentityName(identity)
    }
  }
}
