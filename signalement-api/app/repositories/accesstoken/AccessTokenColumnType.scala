package repositories.accesstoken

import models.token.TokenKind
import repositories.PostgresProfile.api._

object AccessTokenColumnType {

  implicit val TokenKindColumnType = MappedColumnType.base[TokenKind, String](_.entryName, TokenKind.withName(_))

}
