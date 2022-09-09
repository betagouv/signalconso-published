package loader

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.EventBus
import com.mohiva.play.silhouette.api.RequestProvider
import com.mohiva.play.silhouette.api.crypto.CrypterAuthenticatorEncoder
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.crypto.JcaCrypter
import com.mohiva.play.silhouette.crypto.JcaCrypterSettings
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticatorSettings
import com.mohiva.play.silhouette.impl.util.SecureRandomIDGenerator
import play.api.Configuration
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.EnumerationReader._

import scala.concurrent.ExecutionContext

object SilhouetteEnv {

  private val eventBus: EventBus = EventBus()

  def getEnv[E <: Env](
      identityServiceImpl: IdentityService[E#I],
      authenticatorServiceImpl: AuthenticatorService[E#A],
      requestProvidersImpl: Seq[RequestProvider] = Seq()
  )(implicit ec: ExecutionContext): Environment[E] =
    Environment[E](
      identityServiceImpl,
      authenticatorServiceImpl,
      requestProvidersImpl,
      eventBus
    )

  def getJWTAuthenticatorService(
      configuration: Configuration
  )(implicit ec: ExecutionContext): AuthenticatorService[JWTAuthenticator] = {

    val crypterSettings = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
    val jWTAuthenticatorSettings = configuration.underlying.as[JWTAuthenticatorSettings]("silhouette.authenticator")
    val crypter = new JcaCrypter(crypterSettings)

    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(jWTAuthenticatorSettings, None, encoder, new SecureRandomIDGenerator, Clock())

  }

}
