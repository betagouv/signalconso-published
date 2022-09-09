package controllers.error

import controllers.error.AppError.ServerError
import controllers.error.ErrorPayload.AuthenticationErrorPayload
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results

import java.util.UUID

object AppErrorTransformer {

  val logger: Logger = Logger(this.getClass())

  private def formatMessage[R <: Request[_]](request: R, maybeUser: Option[UUID], appError: AppError): String =
    formatMessage(request, maybeUser, appError.details)

  private def formatMessage[R <: Request[_]](request: R, maybeUser: Option[UUID], details: String): String =
    s"""[ user = ${maybeUser.getOrElse("not_connected")}, uri = ${request.uri}]
       | ${details}
       | """.stripMargin

  def handleError[R <: Request[_]](request: R, err: Throwable, maybeUserId: Option[UUID] = None): Result =
    err match {
      case appError: AppError =>
        handleAppError(request, appError, maybeUserId)
      case err =>
        logger.error(formatMessage(request, maybeUserId, "Unexpected error occured"), err)
        Results.InternalServerError(Json.toJson(ErrorPayload(ServerError("Encountered unexpected error", Some(err)))))
    }

  private def handleAppError[R <: Request[_]](request: R, error: AppError, maybeUserId: Option[UUID]): Result =
    error match {
      case error: NotFoundError =>
        logger.warn(formatMessage(request, maybeUserId, error))
        Results.NotFound(Json.toJson(ErrorPayload(error)))

      case error: PreconditionError =>
        logger.warn(formatMessage(request, maybeUserId, error))
        Results.PreconditionFailed(Json.toJson(ErrorPayload(error)))

      case error: ConflictError =>
        logger.warn(formatMessage(request, maybeUserId, error))
        Results.Conflict(Json.toJson(ErrorPayload(error)))

      case error: BadRequestError =>
        logger.warn(formatMessage(request, maybeUserId, error))
        Results.BadRequest(Json.toJson(ErrorPayload(error)))

      case error: ForbiddenError =>
        logger.warn(formatMessage(request, maybeUserId, error))
        Results.Forbidden(Json.toJson(ErrorPayload(error)))

      case error: InternalAppError =>
        logger.error(formatMessage(request, maybeUserId, error), error)
        Results.InternalServerError(Json.toJson(ErrorPayload(error)))

      case error: UnauthorizedError =>
        logger.warn(formatMessage(request, maybeUserId, error), error)
        Results.Unauthorized(Json.toJson(AuthenticationErrorPayload))
    }
}
