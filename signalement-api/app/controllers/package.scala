import controllers.error.AppError.MalformedBody

import models.website.IdentificationStatus
import models.website.WebsiteId
import play.api.Logger
import play.api.libs.json.JsError
import play.api.libs.json.JsPath
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable
import play.api.mvc.Request
import cats.syntax.either._
import models.PublicStat
import models.extractUUID
import models.report.ReportResponseType
import models.report.reportfile.ReportFileId
import utils.DateUtils
import utils.SIRET

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

package object controllers {

  val logger: Logger = Logger(this.getClass)

  implicit val IdentificationStatusQueryStringBindable: QueryStringBindable[IdentificationStatus] =
    QueryStringBindable.bindableString
      .transform[IdentificationStatus](
        identificationStatus => IdentificationStatus.withName(identificationStatus),
        identificationStatus => identificationStatus.entryName
      )

  implicit val UUIDPathBindable =
    PathBindable.bindableString
      .transform[UUID](
        id => extractUUID(id),
        uuid => uuid.toString
      )

  implicit val OffsetDateTimeQueryStringBindable: QueryStringBindable[OffsetDateTime] =
    QueryStringBindable.bindableString
      .transform[OffsetDateTime](
        stringOffsetDateTime => DateUtils.parseTime(stringOffsetDateTime),
        offsetDateTime => offsetDateTime.toString
      )

  implicit val ReportFileIdPathBindable =
    PathBindable.bindableString
      .transform[ReportFileId](
        id => ReportFileId(extractUUID(id)),
        reportFileId => reportFileId.value.toString
      )

  implicit val WebsiteIdPathBindable =
    PathBindable.bindableString
      .transform[WebsiteId](
        id => WebsiteId(extractUUID(id)),
        websiteId => websiteId.value.toString
      )

  implicit val SIRETPathBindable =
    PathBindable.bindableString
      .transform[SIRET](
        siret => SIRET(siret),
        siret => siret.value
      )

  implicit val ReportResponseTypeQueryStringBindable: QueryStringBindable[ReportResponseType] =
    QueryStringBindable.bindableString
      .transform[ReportResponseType](
        reportResponseType => ReportResponseType.withName(reportResponseType),
        reportResponseType => reportResponseType.entryName
      )

  implicit val PublicStatQueryStringBindable: QueryStringBindable[PublicStat] =
    QueryStringBindable.bindableString
      .transform[PublicStat](
        publicStat => PublicStat.withName(publicStat),
        publicStat => publicStat.entryName
      )

  implicit class RequestOps[T <: JsValue](request: Request[T])(implicit ec: ExecutionContext) {
    def parseBody[B](path: JsPath = JsPath())(implicit reads: Reads[B]) = request.body
      .validate[B](path.read[B])
      .asEither
      .leftMap { errors =>
        logger.error(
          s"Malformed request body path ${path} [error : ${JsError.toJson(errors)} , body ${request.body} ]"
        )
        MalformedBody
      }
      .liftTo[Future]
  }
}
