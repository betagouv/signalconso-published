package utils

import com.typesafe.config.Config
import play.api.ConfigLoader
import play.api.libs.json._
import repositories.PostgresProfile.api._

case class EmailAddress(value: String) {
  override def toString = value
}

object EmailAddress {

  val EmptyEmailAddress = EmailAddress("")

  implicit class EmailAddressOps(emailAddress: EmailAddress) {
    implicit def isEmpty: Boolean = emailAddress == EmptyEmailAddress
    implicit def nonEmpty: Boolean = emailAddress != EmptyEmailAddress
  }

  def apply(value: String) = new EmailAddress(value.trim.toLowerCase)
  implicit val EmailColumnType = MappedColumnType.base[EmailAddress, String](
    _.value,
    EmailAddress(_)
  )
  implicit val emailWrites = new Writes[EmailAddress] {
    def writes(o: EmailAddress): JsValue =
      JsString(o.value)
  }
  implicit val emailReads = new Reads[EmailAddress] {
    def reads(json: JsValue): JsResult[EmailAddress] = json.validate[String].map(EmailAddress(_))
  }

  implicit val configLoader: ConfigLoader[EmailAddress] = (rootConfig: Config, path: String) =>
    EmailAddress(rootConfig.getString(path))

}
