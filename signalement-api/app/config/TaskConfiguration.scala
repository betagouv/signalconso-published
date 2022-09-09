package config

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.Period
import scala.concurrent.duration.FiniteDuration

case class TaskConfiguration(
    active: Boolean,
    subscription: SubscriptionTaskConfiguration,
    report: ReportTaskConfiguration,
    inactiveAccounts: InactiveAccountsTaskConfiguration,
    companyUpdate: CompanyUpdateTaskConfiguration
)

case class SubscriptionTaskConfiguration(startTime: LocalTime, startDay: DayOfWeek)

case class InactiveAccountsTaskConfiguration(startTime: LocalTime, inactivePeriod: Period)

case class CompanyUpdateTaskConfiguration(startTime: LocalTime)

case class ReportTaskConfiguration(
    startTime: LocalTime,
    intervalInHours: FiniteDuration,
    noAccessReadingDelay: Period,
    mailReminderDelay: Period,
    reportReminderByPostDelay: Period
)
