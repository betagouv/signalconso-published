package controllers

import com.mohiva.play.silhouette.api.Silhouette
import models.report.ReportCategory
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import utils.Country
import utils.silhouette.auth.AuthEnv

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ConstantController(val silhouette: Silhouette[AuthEnv], controllerComponents: ControllerComponents)(implicit
    val ec: ExecutionContext
) extends BaseController(controllerComponents) {
  val logger: Logger = Logger(this.getClass)

  def getCountries = UnsecuredAction.async {
    Future(Ok(Json.toJson(Country.countries)))
  }

  def getCategories = UnsecuredAction.async {
    Future(Ok(Json.toJson(ReportCategory.values.filterNot(_.legacy))))
  }

}
