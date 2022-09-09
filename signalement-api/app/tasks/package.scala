import play.api.Logger

import tasks.model.TaskType

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration
import cats.data.Validated._
import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import controllers.error.AppError

import java.util.UUID

package object tasks {

  val logger: Logger = Logger(this.getClass)

  type Task = (UUID, TaskType)
  type TaskExecutionResults = ValidatedNel[Task, List[Task]]
  type TaskExecutionResult = ValidatedNel[Task, Task]

  def toValidated[T](taskExecution: Future[T], elementId: UUID, taskType: TaskType)(implicit
      ec: ExecutionContext
  ): Future[TaskExecutionResult] =
    taskExecution.map(_ => Valid((elementId, taskType))).recover {
      case err: AppError =>
        logger.warn(err.details, err)
        (elementId, taskType).invalidNel[Task]
      case err =>
        val errorMessage = s"Error processing ${taskType.entryName} on element with id : ${elementId}"
        logger.error(errorMessage, err)
        (elementId, taskType).invalidNel[Task]
    }

  def computeStartingTime(startTime: LocalTime): FiniteDuration = {

    val startDate: LocalDateTime =
      if (LocalTime.now.isAfter(startTime)) LocalDate.now.plusDays(1).atTime(startTime)
      else LocalDate.now.atTime(startTime)

    (LocalDateTime.now.until(startDate, ChronoUnit.SECONDS) % (24 * 7 * 3600)).seconds
  }

}
