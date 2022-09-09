package utils.silhouette.api

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import repositories.consumer.ConsumerRepositoryInterface

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ApiKeyService(consumerRepository: ConsumerRepositoryInterface)(implicit
    val executionContext: ExecutionContext
) extends IdentityService[APIKey] {

  def retrieve(loginInfo: LoginInfo): Future[Option[APIKey]] =
    consumerRepository
      .get(UUID.fromString(loginInfo.providerKey))
      .map(_.map(c => APIKey(c.id)))

}
