package repositories.consumer

import models.Consumer
import repositories.DatabaseTable
import repositories.PostgresProfile.api._

import java.time.OffsetDateTime

class ConsumerTable(tag: Tag) extends DatabaseTable[Consumer](tag, "consumer") {
  def name = column[String]("name")
  def creationDate = column[OffsetDateTime]("creation_date")
  def apiKey = column[String]("api_key")
  def deleteDate = column[Option[OffsetDateTime]]("delete_date")

  def * = (
    id,
    name,
    creationDate,
    apiKey,
    deleteDate
  ) <> (Consumer.tupled, Consumer.unapply)
}

object ConsumerTable {
  val table = TableQuery[ConsumerTable]
}
