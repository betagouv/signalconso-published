package utils.silhouette.auth

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import controllers.error.AppError.UserNotFound
import play.api.Logger
import repositories.user.UserRepositoryInterface
import utils.EmailAddress
import utils.silhouette.Credentials._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

class PasswordInfoDAO(userRepository: UserRepositoryInterface)(implicit val classTag: ClassTag[PasswordInfo])
    extends DelegableAuthInfoDAO[PasswordInfo] {

  val logger: Logger = Logger(this.getClass())

  def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    update(loginInfo, authInfo)

  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    userRepository.findByLogin(loginInfo.providerKey).map {
      case Some(user) =>
        Some(toPasswordInfo(user.password))
      case _ => None
    }

  def remove(loginInfo: LoginInfo): Future[Unit] =
    userRepository.delete(EmailAddress(loginInfo.providerKey)).map(_ => ()) // FIXME: Is it used ?

  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None    => add(loginInfo, authInfo)
    }

  def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    userRepository.findByLogin(loginInfo.providerKey).flatMap {
      case Some(user) =>
        userRepository.updatePassword(user.id, authInfo.password).map(_ => authInfo)
      case _ =>
        logger.error(s"User not found for login ${loginInfo.providerKey}")
        Future.failed(UserNotFound(loginInfo.providerKey))
    }

}
