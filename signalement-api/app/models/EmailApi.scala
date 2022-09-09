package models
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

object EmailApi {

  type EmailStringRegex = MatchesRegex[
    "^(?=.{1,64}@)[\\p{L}0-9_-]+(\\.[\\p{L}0-9_-]+)*@[^-][\\p{L}0-9-]+(\\.[\\p{L}0-9-]+)*(\\.[\\p{L}]{2,})$"
  ]
  type EmailString = String Refined EmailStringRegex
}
