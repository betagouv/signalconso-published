package repositories

import repositories.PostgresProfile.api._
import slick.ast.TypedType
import slick.lifted.Rep
import slick.lifted.Tag

import java.util.UUID

abstract class TypedDatabaseTable[T, ID](tag: Tag, name: String)(implicit tt: TypedType[ID])
    extends Table[T](tag, name) {
  val id: Rep[ID] = column[ID]("id", O.PrimaryKey)
}

abstract class DatabaseTable[T](tag: Tag, name: String) extends TypedDatabaseTable[T, UUID](tag, name)
