package utils.silhouette.api

import com.mohiva.play.silhouette.api.Identity

import java.util.UUID

case class APIKey(id: UUID) extends Identity
