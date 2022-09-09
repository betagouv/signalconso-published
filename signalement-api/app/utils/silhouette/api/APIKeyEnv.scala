package utils.silhouette.api

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticator

trait APIKeyEnv extends Env {
  type I = APIKey
  type A = DummyAuthenticator
}
