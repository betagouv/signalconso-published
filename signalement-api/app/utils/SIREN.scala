package utils

import play.api.libs.json._
import repositories.PostgresProfile.api._

case class SIREN(value: String) {
  override def toString = value
}

object SIREN {

  val length = 9

  def apply(value: String) = new SIREN(value.replaceAll("\\s", ""))

  def apply(siret: SIRET) = new SIREN(siret.value.substring(0, 9))

  def pattern = s"[0-9]{$length}"

  def isValid(siren: String): Boolean = siren.matches(SIREN.pattern)

  implicit val sirenColumnType = MappedColumnType.base[SIREN, String](
    _.value,
    SIREN(_)
  )
  implicit val sirenListColumnType = MappedColumnType.base[List[SIREN], List[String]](
    _.map(_.value),
    _.map(SIREN(_))
  )
  implicit val sirenWrites = new Writes[SIREN] {
    def writes(o: SIREN): JsValue =
      JsString(o.value)
  }
  implicit val sirenReads = new Reads[SIREN] {
    def reads(json: JsValue): JsResult[SIREN] = json.validate[String].map(SIREN(_))
  }
}
