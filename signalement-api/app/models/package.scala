import controllers.error.AppError.MalformedId

import play.api.Logger

import java.util.UUID
import scala.util.Try

package object models {

  val logger = Logger(this.getClass)

  def extractUUID(stringUUID: String): UUID =
    Try(UUID.fromString(stringUUID)).fold(
      { e =>
        logger.error(s"Unable to parse $stringUUID to UUID", e)
        throw MalformedId(stringUUID)
      },
      identity
    )

}
