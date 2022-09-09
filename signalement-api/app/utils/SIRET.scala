package utils

import controllers.error.AppError.MalformedSIRET
import play.api.libs.json._
import repositories.PostgresProfile.api._

case class SIRET(value: String) {
  override def toString = value
}

object SIRET {

  val length = 14

  def apply(value: String): SIRET =
    if (value.replaceAll("\\s", "").matches(SIRET.pattern)) {
      new SIRET(value)
    } else {
      throw MalformedSIRET(value)
    }

  @Deprecated(since = "use safer version instead")
  def fromUnsafe(value: String) = new SIRET(value.replaceAll("\\s", ""))

  def pattern = s"[0-9]{$length}"

  def isValid(siret: String) = siret.matches(SIRET.pattern)

  implicit val siretColumnType = MappedColumnType.base[SIRET, String](
    _.value,
    SIRET.fromUnsafe(_)
  )
  implicit val siretListColumnType = MappedColumnType.base[List[SIRET], List[String]](
    _.map(_.value),
    _.map(SIRET.fromUnsafe(_))
  )
  implicit val siretWrites = new Writes[SIRET] {
    def writes(o: SIRET): JsValue =
      JsString(o.value)
  }
  implicit val siretReads = new Reads[SIRET] {
    def reads(json: JsValue): JsResult[SIRET] = json.validate[String].map(SIRET.fromUnsafe(_))
  }
}
