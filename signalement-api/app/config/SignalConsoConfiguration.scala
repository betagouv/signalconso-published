package config

import java.net.URI
import java.time.Period

case class SignalConsoConfiguration(
    tmpDirectory: String,
    apiURL: URI,
    websiteURL: URI,
    dashboardURL: URI,
    token: TokenConfiguration,
    upload: UploadConfiguration,
    reportEmailsBlacklist: List[String],
    reportsExportLimitMax: Int = 300000
)

case class UploadConfiguration(allowedExtensions: Seq[String], avScanEnabled: Boolean, downloadDirectory: String)

case class TokenConfiguration(
    companyInitDuration: Option[Period],
    companyJoinDuration: Option[Period],
    dgccrfJoinDuration: Option[Period],
    dgccrfDelayBeforeRevalidation: Period,
    dgccrfRevalidationTokenDuration: Option[Period]
)
