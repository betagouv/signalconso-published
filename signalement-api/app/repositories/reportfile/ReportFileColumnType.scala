package repositories.reportfile

import models.report.ReportFileOrigin
import models.report.reportfile.ReportFileId
import repositories.PostgresProfile.api._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import java.util.UUID

object ReportFileColumnType {

  implicit val ReportFileOriginColumnType: JdbcType[ReportFileOrigin] with BaseTypedType[ReportFileOrigin] =
    MappedColumnType.base[ReportFileOrigin, String](_.value, ReportFileOrigin(_))

  implicit val ReportFileIdColumnType: JdbcType[ReportFileId] with BaseTypedType[ReportFileId] =
    MappedColumnType.base[ReportFileId, UUID](
      _.value,
      ReportFileId(_)
    )

}
