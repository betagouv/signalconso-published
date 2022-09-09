package controllers

import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.ControllerComponents
import utils.silhouette.auth.AuthEnv

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class StaticController(val silhouette: Silhouette[AuthEnv], controllerComponents: ControllerComponents)(implicit
    val ec: ExecutionContext
) extends BaseController(controllerComponents) {

  def api = UserAwareAction.async(parse.empty) { _ =>
    Future.successful(Ok(views.html.api()))
  }
}
