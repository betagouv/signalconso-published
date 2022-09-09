package repositories.asyncfiles

import models.AsyncFileKind
import repositories.PostgresProfile.api._

object AsyncFilesColumnType {

  implicit val AsyncFileKindColumnType =
    MappedColumnType.base[AsyncFileKind, String](
      _.entryName,
      AsyncFileKind.namesToValuesMap
    )

}
