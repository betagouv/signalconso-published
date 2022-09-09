package tasks.model

import enumeratum.EnumEntry
import enumeratum._

sealed trait TaskType extends EnumEntry
sealed trait ReportTaskType extends TaskType

object TaskType extends Enum[TaskType] {

  val values = findValues

  case object InactiveAccountClean extends TaskType
  case object RemindUnreadReportsByEmail extends ReportTaskType
  case object RemindReadReportByMail extends ReportTaskType
  case object CloseReadReportWithNoAction extends ReportTaskType
  case object CloseUnreadReport extends ReportTaskType

}
