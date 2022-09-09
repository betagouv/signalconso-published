package repositories.rating

import models.Rating

import repositories.DatabaseTable

import java.time.OffsetDateTime
import java.util.UUID
import repositories.PostgresProfile.api._

class RatingTable(tag: Tag) extends DatabaseTable[Rating](tag, "ratings") {

  def creationDate = column[OffsetDateTime]("creation_date")
  def category = column[String]("category")
  def subcategories = column[List[String]]("subcategories")
  def positive = column[Boolean]("positive")

  type RatingData = (UUID, OffsetDateTime, String, List[String], Boolean)

  def constructRating: RatingData => Rating = { case (id, creationDate, category, subcategories, positive) =>
    Rating(Some(id), Some(creationDate), category, subcategories, positive)
  }

  def extractRating: PartialFunction[Rating, RatingData] = {
    case Rating(id, creationDate, category, subcategories, positive) =>
      (id.get, creationDate.get, category, subcategories, positive)
  }

  def * =
    (id, creationDate, category, subcategories, positive) <> (constructRating, extractRating.lift)
}

object RatingTable {
  val table = TableQuery[RatingTable]

}
