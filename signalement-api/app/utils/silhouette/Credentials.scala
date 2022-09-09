package utils.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.password.BCryptPasswordHasher

object Credentials {
  def toLoginInfo(key: String): LoginInfo = LoginInfo(CredentialsProvider.ID, key)
  def toPasswordInfo(pwd: String): PasswordInfo =
    PasswordInfo(BCryptPasswordHasher.ID, pwd, salt = Some("SignalConso"))
}
