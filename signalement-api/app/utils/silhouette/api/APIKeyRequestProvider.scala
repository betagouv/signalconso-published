package utils.silhouette.api

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.RequestProvider
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import play.api.Logger
import play.api.mvc.Request
import repositories.consumer.ConsumerRepositoryInterface
import utils.silhouette.Credentials._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class APIKeyRequestProvider(
    passwordHasherRegistry: PasswordHasherRegistry,
    consumerRepository: ConsumerRepositoryInterface
)(implicit ec: ExecutionContext)
    extends RequestProvider {

  val logger: Logger = Logger(this.getClass)

  def authenticate[B](request: Request[B]): Future[Option[LoginInfo]] = {
    val hasher = passwordHasherRegistry.current
    val headerValueOpt = request.headers.get("X-Api-Key")

    headerValueOpt
      .map { headerValue =>
        consumerRepository.getAll().map { consumers =>
          val keyMatchOpt = consumers.find { c =>
            hasher.matches(toPasswordInfo(c.apiKey), headerValue)
          }
          keyMatchOpt match {
            case Some(keyMatch) =>
              logger.debug(s"Access to the API with token ${keyMatch.name}.")
              Some(LoginInfo(id, keyMatch.id.toString))
            case _ =>
              logger.error(
                s"Access denied to the external API, invalid X-Api-Key header when calling ${request.uri}."
              )
              None
          }
        }
      }
      .getOrElse {
        logger.error(
          s"Access denied to the external API, missing X-Api-Key header when calling ${request.uri}."
        )
        Future.successful(None)
      }
  }

  override def id = "api-key"
}
