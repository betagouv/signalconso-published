package controllers

import com.mohiva.play.silhouette.api.Silhouette
import models.Subscription
import models.SubscriptionCreation
import models.SubscriptionUpdate
import models.UserPermission
import play.api.Logger
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import repositories.subscription.SubscriptionRepositoryInterface
import utils.Country
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.WithPermission

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SubscriptionController(
    subscriptionRepository: SubscriptionRepositoryInterface,
    val silhouette: Silhouette[AuthEnv],
    controllerComponents: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BaseController(controllerComponents) {

  val logger: Logger = Logger(this.getClass)

  def createSubscription = SecuredAction(WithPermission(UserPermission.subscribeReports)).async(parse.json) {
    implicit request =>
      request.body
        .validate[SubscriptionCreation]
        .fold(
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          draftSubscription =>
            subscriptionRepository
              .create(
                Subscription(
                  userId = Some(request.identity.id),
                  email = None,
                  departments = draftSubscription.departments,
                  categories = draftSubscription.categories,
                  withTags = draftSubscription.withTags,
                  withoutTags = draftSubscription.withoutTags,
                  countries = draftSubscription.countries.map(Country.fromCode),
                  sirets = draftSubscription.sirets,
                  frequency = draftSubscription.frequency
                )
              )
              .map(subscription => Ok(Json.toJson(subscription)))
        )
  }

  def updateSubscription(uuid: UUID) =
    SecuredAction(WithPermission(UserPermission.subscribeReports)).async(parse.json) { implicit request =>
      request.body
        .validate[SubscriptionUpdate]
        .fold(
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          draftSubscription =>
            for {
              subscriptions <- subscriptionRepository.list(request.identity.id)
              updatedSubscription <- subscriptions
                .find(_.id == uuid)
                .map(s =>
                  subscriptionRepository
                    .update(
                      s.id,
                      s.copy(
                        departments = draftSubscription.departments.getOrElse(s.departments),
                        categories = draftSubscription.categories.getOrElse(s.categories),
                        withTags = draftSubscription.withTags.getOrElse(s.withTags),
                        withoutTags = draftSubscription.withoutTags.getOrElse(s.withoutTags),
                        countries = draftSubscription.countries
                          .map(_.map(Country.fromCode))
                          .getOrElse(s.countries),
                        sirets = draftSubscription.sirets.getOrElse(s.sirets),
                        frequency = draftSubscription.frequency.getOrElse(s.frequency)
                      )
                    )
                    .map(Some(_))
                )
                .getOrElse(Future(None))
            } yield if (updatedSubscription.isDefined) Ok(Json.toJson(updatedSubscription)) else NotFound
        )
    }

  def getSubscriptions = SecuredAction(WithPermission(UserPermission.subscribeReports)).async { implicit request =>
    subscriptionRepository.list(request.identity.id).map(subscriptions => Ok(Json.toJson(subscriptions)))
  }

  def getSubscription(uuid: UUID) = SecuredAction(WithPermission(UserPermission.subscribeReports)).async {
    implicit request =>
      subscriptionRepository
        .get(uuid)
        .map(subscription =>
          subscription
            .filter(s => s.userId == Some(request.identity.id))
            .map(s => Ok(Json.toJson(s)))
            .getOrElse(NotFound)
        )
  }

  def removeSubscription(uuid: UUID) = SecuredAction(WithPermission(UserPermission.subscribeReports)).async {
    implicit request =>
      for {
        subscriptions <- subscriptionRepository.list(request.identity.id)
        deletedCount <- subscriptions
          .find(_.id == uuid)
          .map(subscription => subscriptionRepository.delete(subscription.id))
          .getOrElse(Future(0))
      } yield if (deletedCount > 0) Ok else NotFound
  }
}
