package config

case class ApplicationConfiguration(
    app: SignalConsoConfiguration,
    mail: EmailConfiguration,
    task: TaskConfiguration,
    amazonBucketName: String
)
