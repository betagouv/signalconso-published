package loader

import _root_.controllers._
import actors.EmailActor.EmailRequest
import actors._
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.typed
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.SilhouetteProvider
import com.mohiva.play.silhouette.api.actions._
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.authenticators.DummyAuthenticatorService
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import company.EnterpriseImportController
import company.EnterpriseImportOrchestrator
import company.companydata.CompanyDataRepository
import company.companydata.CompanyDataRepositoryInterface
import company.entrepriseimportinfo.EnterpriseImportInfoRepository
import config.ApplicationConfiguration
import config.BucketConfiguration
import config.SignalConsoConfiguration
import config.TaskConfiguration
import config.UploadConfiguration
import orchestrators.ProAccessTokenOrchestrator
import orchestrators._
import play.api._
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.slick.DbName
import play.api.db.slick.SlickComponents
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.libs.mailer.MailerComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.BodyParsers
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import pureconfig.ConfigConvert
import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.configurable.localTimeConfigConvert
import pureconfig.generic.auto._
import pureconfig.generic.semiauto.deriveReader
import repositories.accesstoken.AccessTokenRepository
import repositories.accesstoken.AccessTokenRepositoryInterface
import repositories.asyncfiles.AsyncFileRepository
import repositories.asyncfiles.AsyncFileRepositoryInterface
import repositories.authattempt.AuthAttemptRepository
import repositories.authattempt.AuthAttemptRepositoryInterface
import repositories.authtoken.AuthTokenRepository
import repositories.authtoken.AuthTokenRepositoryInterface
import repositories.company.CompanyRepository
import repositories.company.CompanyRepositoryInterface
import repositories.companyaccess.CompanyAccessRepository
import repositories.companyaccess.CompanyAccessRepositoryInterface
import repositories.consumer.ConsumerRepository
import repositories.consumer.ConsumerRepositoryInterface
import repositories.dataeconomie.DataEconomieRepository
import repositories.dataeconomie.DataEconomieRepositoryInterface
import repositories.emailvalidation.EmailValidationRepository
import repositories.emailvalidation.EmailValidationRepositoryInterface
import repositories.event.EventRepository
import repositories.event.EventRepositoryInterface
import repositories.rating.RatingRepository
import repositories.rating.RatingRepositoryInterface
import repositories.report.ReportRepository
import repositories.report.ReportRepositoryInterface
import repositories.reportblockednotification.ReportNotificationBlockedRepository
import repositories.reportblockednotification.ReportNotificationBlockedRepositoryInterface
import repositories.reportconsumerreview.ResponseConsumerReviewRepository
import repositories.reportconsumerreview.ResponseConsumerReviewRepositoryInterface
import repositories.reportfile.ReportFileRepository
import repositories.reportfile.ReportFileRepositoryInterface
import repositories.subscription.SubscriptionRepository
import repositories.subscription.SubscriptionRepositoryInterface
import repositories.user.UserRepository
import repositories.user.UserRepositoryInterface
import repositories.website.WebsiteRepository
import repositories.website.WebsiteRepositoryInterface
import services._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import tasks.account.InactiveAccountTask
import tasks.account.InactiveDgccrfAccountRemoveTask
import tasks.company.CompanyUpdateTask
import tasks.report.NoActionReportsCloseTask
import tasks.report.ReadReportsReminderTask
import tasks.report.ReportNotificationTask
import tasks.report.ReportTask
import tasks.report.UnreadReportsCloseTask
import tasks.report.UnreadReportsReminderTask
import utils.EmailAddress
import utils.FrontRoute
import utils.silhouette.api.APIKeyEnv
import utils.silhouette.api.APIKeyRequestProvider
import utils.silhouette.api.ApiKeyService
import utils.silhouette.auth.AuthEnv
import utils.silhouette.auth.PasswordInfoDAO
import utils.silhouette.auth.UserService

import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SignalConsoApplicationLoader() extends ApplicationLoader {
  var components: SignalConsoComponents = _

  override def load(context: ApplicationLoader.Context): Application = {
    components = new SignalConsoComponents(context)
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    components.application
  }
}

class SignalConsoComponents(
    context: ApplicationLoader.Context
) extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with play.filters.cors.CORSComponents
    with AssetsComponents
    with AhcWSComponents
    with SlickComponents
    with SlickEvolutionsComponents
    with EvolutionsComponents
    with SecuredActionComponents
    with SecuredErrorHandlerComponents
    with UnsecuredActionComponents
    with UnsecuredErrorHandlerComponents
    with UserAwareActionComponents
    with MailerComponents {

  applicationEvolutions

  implicit val localTimeInstance: ConfigConvert[LocalTime] = localTimeConfigConvert(DateTimeFormatter.ISO_TIME)
  implicit val personReader: ConfigReader[EmailAddress] = deriveReader[EmailAddress]
  val csvStringListReader = ConfigReader[String].map(_.split(",").toList)
  implicit val stringListReader = ConfigReader[List[String]].orElse(csvStringListReader)

  val applicationConfiguration: ApplicationConfiguration = ConfigSource.default.loadOrThrow[ApplicationConfiguration]
  def emailConfiguration = applicationConfiguration.mail
  def signalConsoConfiguration: SignalConsoConfiguration = applicationConfiguration.app
  def tokenConfiguration = signalConsoConfiguration.token
  def uploadConfiguration: UploadConfiguration = signalConsoConfiguration.upload

  def passwordHasherRegistry: PasswordHasherRegistry = PasswordHasherRegistry(
    new BCryptPasswordHasher()
  )

  //  Repositories

  val dbConfig: DatabaseConfig[JdbcProfile] = slickApi.dbConfig[JdbcProfile](DbName("default"))
  val dbConfigCompanyDb: DatabaseConfig[JdbcProfile] = slickApi.dbConfig[JdbcProfile](DbName("company_db"))

  val companyAccessRepository: CompanyAccessRepositoryInterface = new CompanyAccessRepository(dbConfig)
  val accessTokenRepository: AccessTokenRepositoryInterface =
    new AccessTokenRepository(dbConfig, companyAccessRepository)
  val asyncFileRepository: AsyncFileRepositoryInterface = new AsyncFileRepository(dbConfig)
  val authAttemptRepository: AuthAttemptRepositoryInterface = new AuthAttemptRepository(dbConfig)
  val authTokenRepository: AuthTokenRepositoryInterface = new AuthTokenRepository(dbConfig)
  val companyRepository: CompanyRepositoryInterface = new CompanyRepository(dbConfig)
  val companyDataRepository: CompanyDataRepositoryInterface = new CompanyDataRepository(dbConfigCompanyDb)
  val consumerRepository: ConsumerRepositoryInterface = new ConsumerRepository(dbConfig)
  val dataEconomieRepository: DataEconomieRepositoryInterface = new DataEconomieRepository(actorSystem)
  val emailValidationRepository: EmailValidationRepositoryInterface = new EmailValidationRepository(dbConfig)
  val enterpriseImportInfoRepository: EnterpriseImportInfoRepository = new EnterpriseImportInfoRepository(
    dbConfigCompanyDb
  )
  def eventRepository: EventRepositoryInterface = new EventRepository(dbConfig)
  val ratingRepository: RatingRepositoryInterface = new RatingRepository(dbConfig)
  def reportRepository: ReportRepositoryInterface = new ReportRepository(dbConfig)
  val reportNotificationBlockedRepository: ReportNotificationBlockedRepositoryInterface =
    new ReportNotificationBlockedRepository(dbConfig)
  val responseConsumerReviewRepository: ResponseConsumerReviewRepositoryInterface =
    new ResponseConsumerReviewRepository(dbConfig)
  def reportFileRepository: ReportFileRepositoryInterface = new ReportFileRepository(dbConfig)
  val subscriptionRepository: SubscriptionRepositoryInterface = new SubscriptionRepository(dbConfig)
  val userRepository: UserRepositoryInterface = new UserRepository(dbConfig, passwordHasherRegistry)
  val websiteRepository: WebsiteRepositoryInterface = new WebsiteRepository(dbConfig)

  val userService = new UserService(userRepository)
  val apiUserService = new ApiKeyService(consumerRepository)

  val authenticatorService: AuthenticatorService[JWTAuthenticator] =
    SilhouetteEnv.getJWTAuthenticatorService(configuration)

  def authEnv: Environment[AuthEnv] = SilhouetteEnv.getEnv[AuthEnv](userService, authenticatorService)

  val silhouette: Silhouette[AuthEnv] =
    new SilhouetteProvider[AuthEnv](authEnv, securedAction, unsecuredAction, userAwareAction)

  def authApiEnv: Environment[APIKeyEnv] =
    SilhouetteEnv.getEnv[APIKeyEnv](
      apiUserService,
      new DummyAuthenticatorService(),
      Seq(new APIKeyRequestProvider(passwordHasherRegistry, consumerRepository))
    )

  val silhouetteApi: Silhouette[APIKeyEnv] =
    new SilhouetteProvider[APIKeyEnv](authApiEnv, securedAction, unsecuredAction, userAwareAction)

  val authInfoRepository = new DelegableAuthInfoRepository(
    new PasswordInfoDAO(
      userRepository
    )
  )

  val credentialsProvider = new CredentialsProvider(authInfoRepository, passwordHasherRegistry)

  implicit val bucketConfiguration: BucketConfiguration = BucketConfiguration(
    keyId = configuration.get[String]("alpakka.s3.aws.credentials.access-key-id"),
    secretKey = configuration.get[String]("alpakka.s3.aws.credentials.secret-access-key"),
    amazonBucketName = applicationConfiguration.amazonBucketName
  )

  def s3Service: S3ServiceInterface = new S3Service()
  def mailer = new MailerService(mailerClient)

  //  Actor
  val emailActor: ActorRef = actorSystem.actorOf(Props(new EmailActor(mailer)), "email-actor")
  val enterpriseSyncActor: ActorRef = actorSystem.actorOf(
    Props(new EnterpriseSyncActor(enterpriseImportInfoRepository, companyDataRepository)),
    "enterprise-sync-actor"
  )

  val antivirusScanActor: typed.ActorRef[AntivirusScanActor.ScanCommand] = actorSystem.spawn(
    AntivirusScanActor.create(uploadConfiguration, reportFileRepository, s3Service),
    "antivirus-scan-actor"
  )
  val reportedPhonesExtractActor: ActorRef =
    actorSystem.actorOf(
      Props(
        new ReportedPhonesExtractActor(signalConsoConfiguration, reportRepository, asyncFileRepository, s3Service)
      ),
      "reported-phones-extract-actor"
    )

  val websitesExtractActor: ActorRef =
    actorSystem.actorOf(
      Props(new WebsitesExtractActor(websiteRepository, asyncFileRepository, s3Service, signalConsoConfiguration)),
      "websites-extract-actor"
    )

  val pdfService = new PDFService(signalConsoConfiguration)
  implicit val frontRoute = new FrontRoute(signalConsoConfiguration)
  val attachmentService = new AttachmentService(environment, pdfService, frontRoute)
  val mailService = new MailService(
    (emailRequest: EmailRequest) => emailActor ! emailRequest,
    emailConfiguration,
    reportNotificationBlockedRepository,
    pdfService,
    attachmentService
  )

  // Orchestrator

  val userOrchestrator = new UserOrchestrator(userRepository)

  val proAccessTokenOrchestrator = new ProAccessTokenOrchestrator(
    userOrchestrator,
    companyRepository,
    companyAccessRepository,
    companyDataRepository,
    accessTokenRepository,
    userRepository,
    eventRepository,
    mailService,
    frontRoute,
    emailConfiguration,
    tokenConfiguration
  )

  val accessesOrchestrator = new AccessesOrchestrator(
    userOrchestrator,
    accessTokenRepository,
    mailService,
    frontRoute,
    emailConfiguration,
    tokenConfiguration
  )

  val authOrchestrator = new AuthOrchestrator(
    userService,
    authAttemptRepository,
    userRepository,
    accessesOrchestrator,
    authTokenRepository,
    tokenConfiguration,
    credentialsProvider,
    mailService,
    silhouette
  )

  def companiesVisibilityOrchestrator =
    new CompaniesVisibilityOrchestrator(companyDataRepository, companyRepository, companyAccessRepository)

  val companyAccessOrchestrator =
    new CompanyAccessOrchestrator(
      companyDataRepository,
      companyAccessRepository,
      companyRepository,
      accessTokenRepository,
      proAccessTokenOrchestrator
    )

  private val taskConfiguration: TaskConfiguration = applicationConfiguration.task
  val companyOrchestrator = new CompanyOrchestrator(
    companyRepository,
    companiesVisibilityOrchestrator,
    reportRepository,
    companyDataRepository,
    websiteRepository,
    accessTokenRepository,
    eventRepository,
    taskConfiguration
  )

  val dataEconomieOrchestrator = new DataEconomieOrchestrator(dataEconomieRepository)
  val emailValidationOrchestrator =
    new EmailValidationOrchestrator(mailService, emailValidationRepository, emailConfiguration)

  val enterpriseImportOrchestrator =
    new EnterpriseImportOrchestrator(enterpriseImportInfoRepository, enterpriseSyncActor)

  val eventsOrchestrator = new EventsOrchestrator(eventRepository, reportRepository, companyRepository)

  val reportBlockedNotificationOrchestrator = new ReportBlockedNotificationOrchestrator(
    reportNotificationBlockedRepository
  )

  val reportConsumerReviewOrchestrator =
    new ReportConsumerReviewOrchestrator(reportRepository, eventRepository, responseConsumerReviewRepository)

  val reportFileOrchestrator = new ReportFileOrchestrator(reportFileRepository, antivirusScanActor, s3Service)

  val reportOrchestrator = new ReportOrchestrator(
    mailService,
    reportConsumerReviewOrchestrator,
    reportRepository,
    reportFileOrchestrator,
    companyRepository,
    accessTokenRepository,
    eventRepository,
    websiteRepository,
    companiesVisibilityOrchestrator,
    subscriptionRepository,
    emailValidationOrchestrator,
    emailConfiguration,
    tokenConfiguration,
    signalConsoConfiguration
  )

  val reportsExtractActor: ActorRef =
    actorSystem.actorOf(
      Props(
        new ReportsExtractActor(
          reportFileRepository,
          companyAccessRepository,
          reportOrchestrator,
          eventRepository,
          asyncFileRepository,
          s3Service,
          signalConsoConfiguration
        )
      ),
      "reports-extract-actor"
    )

  val statsOrchestrator =
    new StatsOrchestrator(reportRepository, eventRepository, responseConsumerReviewRepository, accessTokenRepository)

  val websitesOrchestrator =
    new WebsitesOrchestrator(websiteRepository, companyRepository)

  val unreadReportsReminderTask =
    new UnreadReportsReminderTask(applicationConfiguration.task, eventRepository, mailService)
  val unreadReportsCloseTask =
    new UnreadReportsCloseTask(applicationConfiguration.task, eventRepository, reportRepository, mailService)

  val readReportsReminderTask = new ReadReportsReminderTask(applicationConfiguration.task, eventRepository, mailService)

  val companyTask = new CompanyUpdateTask(
    actorSystem,
    applicationConfiguration.task.companyUpdate,
    companyRepository,
    companyDataRepository
  )

  companyTask.runTask()

  val noActionReportsCloseTask =
    new NoActionReportsCloseTask(eventRepository, reportRepository, mailService, taskConfiguration)

  val reportTask = new ReportTask(
    actorSystem,
    reportRepository,
    eventRepository,
    companiesVisibilityOrchestrator,
    signalConsoConfiguration,
    unreadReportsReminderTask,
    unreadReportsCloseTask,
    readReportsReminderTask,
    noActionReportsCloseTask,
    taskConfiguration
  )
  val reportNotificationTask =
    new ReportNotificationTask(actorSystem, reportRepository, subscriptionRepository, mailService, taskConfiguration)

  val inactiveDgccrfAccountRemoveTask =
    new InactiveDgccrfAccountRemoveTask(userRepository, subscriptionRepository, eventRepository, asyncFileRepository)
  val inactiveAccountTask = new InactiveAccountTask(
    actorSystem,
    inactiveDgccrfAccountRemoveTask,
    applicationConfiguration.task
  )

  // Controller
  val accountController = new AccountController(
    silhouette,
    userOrchestrator,
    userRepository,
    accessesOrchestrator,
    proAccessTokenOrchestrator,
    emailConfiguration,
    controllerComponents
  )

  val adminController = new AdminController(
    silhouette,
    reportRepository,
    companyAccessRepository,
    eventRepository,
    mailService,
    emailConfiguration,
    frontRoute,
    controllerComponents
  )

  val asyncFileController = new AsyncFileController(asyncFileRepository, silhouette, s3Service, controllerComponents)

  val authController = new AuthController(silhouette, authOrchestrator, controllerComponents)

  val companyAccessController =
    new CompanyAccessController(
      userRepository,
      companyRepository,
      companyAccessRepository,
      accessTokenRepository,
      proAccessTokenOrchestrator,
      companiesVisibilityOrchestrator,
      companyAccessOrchestrator,
      silhouette,
      controllerComponents
    )

  val companyController = new CompanyController(
    companyOrchestrator,
    companiesVisibilityOrchestrator,
    companyRepository,
    accessTokenRepository,
    eventRepository,
    reportRepository,
    pdfService,
    silhouette,
    companiesVisibilityOrchestrator,
    frontRoute,
    taskConfiguration,
    emailConfiguration,
    controllerComponents
  )

  val constantController = new ConstantController(silhouette, controllerComponents)
  val dataEconomieController = new DataEconomieController(dataEconomieOrchestrator, silhouetteApi, controllerComponents)
  val emailValidationController =
    new EmailValidationController(silhouette, emailValidationOrchestrator, controllerComponents)
  val enterpriseImportController =
    new EnterpriseImportController(enterpriseImportOrchestrator, silhouette, controllerComponents)
  val eventsController = new EventsController(eventsOrchestrator, silhouette, controllerComponents)
  val ratingController = new RatingController(ratingRepository, silhouette, controllerComponents)
  val reportBlockedNotificationController =
    new ReportBlockedNotificationController(
      silhouette,
      silhouetteApi,
      reportBlockedNotificationOrchestrator,
      controllerComponents
    )
  val reportConsumerReviewController =
    new ReportConsumerReviewController(reportConsumerReviewOrchestrator, silhouette, controllerComponents)

  val reportFileController =
    new ReportFileController(reportFileOrchestrator, silhouette, signalConsoConfiguration, controllerComponents)

  val reportWithDataOrchestrator =
    new ReportWithDataOrchestrator(reportOrchestrator, eventRepository, reportFileRepository)

  val reportController = new ReportController(
    reportOrchestrator,
    reportRepository,
    reportFileRepository,
    pdfService,
    frontRoute,
    silhouette,
    controllerComponents,
    reportWithDataOrchestrator
  )
  val reportedPhoneController = new ReportedPhoneController(
    reportRepository,
    companyRepository,
    asyncFileRepository,
    reportedPhonesExtractActor,
    silhouette,
    controllerComponents
  )

  val reportListController =
    new ReportListController(
      reportOrchestrator,
      asyncFileRepository,
      reportsExtractActor,
      silhouette,
      silhouetteApi,
      controllerComponents
    )

  val reportToExternalController =
    new ReportToExternalController(
      reportRepository,
      reportFileRepository,
      reportOrchestrator,
      silhouetteApi,
      controllerComponents
    )

  val staticController = new StaticController(silhouette, controllerComponents)

  val statisticController = new StatisticController(statsOrchestrator, silhouette, controllerComponents)

  val subscriptionController = new SubscriptionController(subscriptionRepository, silhouette, controllerComponents)
  val websiteController = new WebsiteController(
    websitesOrchestrator,
    companyRepository,
    websitesExtractActor,
    silhouette,
    controllerComponents
  )

  io.sentry.Sentry.captureException(
    new Exception("This is a test Alert, used to check that Sentry alert are still active on each new deployments.")
  )

  // Routes
  lazy val router: Router =
    new _root_.router.Routes(
      httpErrorHandler,
      staticController,
      statisticController,
      companyAccessController,
      reportListController,
      reportFileController,
      reportController,
      reportConsumerReviewController,
      eventsController,
      reportToExternalController,
      dataEconomieController,
      adminController,
      asyncFileController,
      constantController,
      authController,
      enterpriseImportController,
      accountController,
      emailValidationController,
      companyController,
      ratingController,
      subscriptionController,
      websiteController,
      reportedPhoneController,
      reportBlockedNotificationController,
      assets
    )

  override def securedBodyParser: BodyParsers.Default = new BodyParsers.Default(controllerComponents.parsers)

  override def unsecuredBodyParser: BodyParsers.Default = new BodyParsers.Default(controllerComponents.parsers)

  override def userAwareBodyParser: BodyParsers.Default = new BodyParsers.Default(controllerComponents.parsers)

  override def config: Config = ConfigFactory.load()

  override def httpFilters: Seq[EssentialFilter] =
    Seq(csrfFilter, securityHeadersFilter, allowedHostsFilter, corsFilter)

}
