package models

import enumeratum._

sealed trait UserRole extends EnumEntry {
  val permissions: Seq[UserPermission.Value]
}

object UserRole extends PlayEnum[UserRole] {

  final case object Admin extends UserRole {
    override val permissions = UserPermission.values.toSeq
  }

  final case object DGCCRF extends UserRole {
    override val permissions = Seq(
      UserPermission.listReports,
      UserPermission.createReportAction,
      UserPermission.subscribeReports
    )
  }

  final case object Professionnel extends UserRole {
    override val permissions = Seq(
      UserPermission.listReports,
      UserPermission.createReportAction
    )
  }

  override def values: IndexedSeq[UserRole] = findValues
}
