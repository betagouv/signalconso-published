package orchestrators

import akka.Done
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxMonadError
import cats.implicits.catsSyntaxOption
import cats.implicits.toTraverseOps
import config.EmailConfiguration
import config.SignalConsoConfiguration
import config.TokenConfiguration
import controllers.error.AppError
import controllers.error.AppError.DuplicateReportCreation
import controllers.error.AppError.ExternalReportsMaxPageSizeExceeded
import controllers.error.AppError.InvalidEmail
import controllers.error.AppError.ReportCreationInvalidBody
import controllers.error.AppError.SpammerEmailBlocked
import models.event.Event._
import models._
import models.event.Event
import models.report.Report
import models.report.ReportAction
import models.report.ReportCompany
import models.report.ReportConsumerUpdate
import models.report.ReportDraft
import models.report.ReportFile
import models.report.ReportFilter
import models.report.ReportResponse
import models.report.ReportStatus
import models.report.ReportTag
import models.report.ReportWithFiles
import models.report.ReportWordOccurrence
import models.report.ReportWordOccurrence.StopWords
import models.token.TokenKind.CompanyInit
import models.website.Website
import play.api.libs.json.Json
import play.api.Logger
import repositories.accesstoken.AccessTokenRepositoryInterface
import repositories.company.CompanyRepositoryInterface
import repositories.event.EventFilter
import repositories.event.EventRepositoryInterface
import repositories.report.ReportRepositoryInterface
import repositories.subscription.SubscriptionRepositoryInterface
import repositories.website.WebsiteRepositoryInterface
import services.Email.ConsumerProResponseNotification
import services.Email.ConsumerReportAcknowledgment
import services.Email.ConsumerReportReadByProNotification
import services.Email.DgccrfDangerousProductReportNotification
import services.Email.ProNewReportNotification
import services.Email.ProResponseAcknowledgment
import services.MailService
import utils.Constants.ActionEvent._
import utils.Constants.ActionEvent
import utils.Constants.EventType
import utils.Constants
import utils.EmailAddress
import utils.URL

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAmount
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random

class ReportOrchestrator(
    mailService: MailService,
    reportConsumerReviewOrchestrator: ReportConsumerReviewOrchestrator,
    reportRepository: ReportRepositoryInterface,
    reportFileOrchestrator: ReportFileOrchestrator,
    companyRepository: CompanyRepositoryInterface,
    accessTokenRepository: AccessTokenRepositoryInterface,
    eventRepository: EventRepositoryInterface,
    websiteRepository: WebsiteRepositoryInterface,
    companiesVisibilityOrchestrator: CompaniesVisibilityOrchestrator,
    subscriptionRepository: SubscriptionRepositoryInterface,
    emailValidationOrchestrator: EmailValidationOrchestrator,
    emailConfiguration: EmailConfiguration,
    tokenConfiguration: TokenConfiguration,
    signalConsoConfiguration: SignalConsoConfiguration
)(implicit val executionContext: ExecutionContext) {
  val logger = Logger(this.getClass)

  implicit val timeout: akka.util.Timeout = 5.seconds

  private def genActivationToken(companyId: UUID, validity: Option[TemporalAmount]): Future[String] =
    for {
      existingToken <- accessTokenRepository.fetchValidActivationToken(companyId)
      _ <- existingToken
        .map(accessTokenRepository.updateToken(_, AccessLevel.ADMIN, validity))
        .getOrElse(Future(None))
      token <- existingToken
        .map(Future(_))
        .getOrElse(
          accessTokenRepository.create(
            AccessToken.build(
              kind = CompanyInit,
              token = f"${Random.nextInt(1000000)}%06d",
              validity = validity,
              companyId = Some(companyId),
              level = Some(AccessLevel.ADMIN)
            )
          )
        )
    } yield token.token

  private def notifyProfessionalOfNewReport(report: Report, company: Company): Future[Report] =
    for {
      maybeCompanyUsers <- companiesVisibilityOrchestrator
        .fetchAdminsWithHeadOffice(company.siret)
        .map(NonEmptyList.fromList)

      updatedReport <- maybeCompanyUsers match {
        case Some(companyUsers) =>
          logger.debug("Found user, sending notification")
          val companyUserEmails: NonEmptyList[EmailAddress] = companyUsers.map(_.email)
          for {
            _ <- mailService.send(ProNewReportNotification(companyUserEmails, report))
            reportWithUpdatedStatus <- reportRepository.update(
              report.id,
              report.copy(status = ReportStatus.TraitementEnCours)
            )
            _ <- createEmailProNewReportEvent(report, company, companyUsers)
          } yield reportWithUpdatedStatus
        case None =>
          logger.debug("No user found, generating activation token")
          genActivationToken(company.id, tokenConfiguration.companyInitDuration).map(_ => report)
      }
    } yield updatedReport

  private def createEmailProNewReportEvent(report: Report, company: Company, companyUsers: NonEmptyList[User]) =
    eventRepository
      .create(
        Event(
          UUID.randomUUID(),
          Some(report.id),
          Some(company.id),
          Some(companyUsers.head.id),
          OffsetDateTime.now(),
          Constants.EventType.PRO,
          Constants.ActionEvent.EMAIL_PRO_NEW_REPORT,
          stringToDetailsJsValue(
            s"Notification du professionnel par mail de la réception d'un nouveau signalement ( ${companyUsers.map(_.email).toList.mkString(", ")} )"
          )
        )
      )

  private[this] def createReportedWebsite(
      companyOpt: Option[Company],
      companyCountry: Option[String],
      websiteURLOpt: Option[URL]
  ): Future[Option[Website]] = {
    val maybeWebsite: Option[Website] = for {
      websiteUrl <- websiteURLOpt
      host <- websiteUrl.getHost
    } yield Website(host = host, companyCountry = companyCountry, companyId = companyOpt.map(_.id))

    maybeWebsite.map { website =>
      logger.debug("Creating website entry")
      websiteRepository.validateAndCreate(website)
    }.sequence
  }

  def validateAndCreateReport(draftReport: ReportDraft): Future[Report] =
    for {
      _ <- validateCompany(draftReport)
      _ <- validateSpamSimilarReport(draftReport)
      _ <- validateReportIdentification(draftReport)
      _ <- validateConsumerEmail(draftReport)
      createdReport <- createReport(draftReport)
    } yield createdReport

  private def validateReportIdentification(draftReport: ReportDraft) =
    if (ReportDraft.isValid(draftReport)) {
      Future.unit
    } else {
      Future.failed(ReportCreationInvalidBody)
    }

  private def validateConsumerEmail(draftReport: ReportDraft) = for {
    _ <- emailValidationOrchestrator
      .isEmailValid(draftReport.email)
      .ensure {
        logger.warn(s"Email ${draftReport.email} is not valid, abort report creation")
        InvalidEmail(draftReport.email.value)
      }(isValid => isValid || emailConfiguration.skipReportEmailValidation)
    _ <- validateReportSpammerBlockList(draftReport.email)
  } yield ()

  private[orchestrators] def validateSpamSimilarReport(draftReport: ReportDraft): Future[Unit] = {
    logger.debug(s"Checking if similar report have been submitted")

    val startOfDay = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC)
    val startOfWeek = LocalDate.now().minusDays(7).atStartOfDay().atOffset(ZoneOffset.UTC)

    val MAX_SIMILAR_CONSUMER_COMPANY_REPORT_WHITHIN_A_WEEK = 4
    val MAX_SIMILAR_CONSUMER_COMPANY_REPORT_WHITHIN_A_DAY = 2

    reportRepository
      .findSimilarReportList(draftReport, after = startOfWeek)
      .map { reportList =>
        val exactSameReportList =
          reportList
            .filter(r => r.creationDate.isAfter(startOfDay) || r.creationDate.isEqual(startOfDay))
            .filter(_.details.containsSlice(draftReport.details))

        val reportsWithSameUserAndCompanyTodayList =
          reportList.filter(r => r.creationDate.isAfter(startOfDay) || r.creationDate.isEqual(startOfDay))

        val reportsWithSameUserAndCompanyThisWeek =
          reportList.filter(r => r.creationDate.isAfter(startOfWeek) || r.creationDate.isEqual(startOfWeek))

        if (exactSameReportList.nonEmpty) {
          throw DuplicateReportCreation(exactSameReportList)
        } else if (
          reportsWithSameUserAndCompanyTodayList.size > MAX_SIMILAR_CONSUMER_COMPANY_REPORT_WHITHIN_A_DAY - 1
        ) {
          throw DuplicateReportCreation(reportsWithSameUserAndCompanyTodayList)
        } else if (
          reportsWithSameUserAndCompanyThisWeek.size > MAX_SIMILAR_CONSUMER_COMPANY_REPORT_WHITHIN_A_WEEK - 1
        ) {
          throw DuplicateReportCreation(reportsWithSameUserAndCompanyThisWeek)
        } else ()
      }

  }

  private def validateReportSpammerBlockList(emailAddress: EmailAddress) =
    if (signalConsoConfiguration.reportEmailsBlacklist.contains(emailAddress.value)) {
      Future.failed(SpammerEmailBlocked(emailAddress))
    } else {
      Future.unit
    }

  private[orchestrators] def validateCompany(reportDraft: ReportDraft): Future[Done.type] =
    reportDraft.companyActivityCode match {
      case Some(activityCode) if activityCode.startsWith("84.") =>
        Future.failed(AppError.CannotReportPublicAdministration)
      case _ => Future.successful(Done)
    }

  private def createReport(draftReport: ReportDraft): Future[Report] =
    for {
      maybeCompany <- extractOptionnalCompany(draftReport)
      maybeCountry = extractOptionnalCountry(draftReport)
      _ <- createReportedWebsite(maybeCompany, maybeCountry, draftReport.websiteURL)
      reportToCreate = draftReport.generateReport(maybeCompany.map(_.id))
      report <- reportRepository.create(reportToCreate)
      _ = logger.debug(s"Created report with id ${report.id}")
      files <- reportFileOrchestrator.attachFilesToReport(draftReport.fileIds, report.id)
      updatedReport <- notifyProfessionalIfNeeded(maybeCompany, report)
      _ <- notifyDgccrfIfNeeded(updatedReport)
      _ <- notifyConsumer(updatedReport, maybeCompany, files)
      _ = logger.debug(s"Report ${updatedReport.id} created")
    } yield updatedReport

  private def notifyDgccrfIfNeeded(report: Report): Future[Unit] = for {
    ddEmails <-
      if (report.tags.contains(ReportTag.ProduitDangereux)) {
        report.companyAddress.postalCode
          .map(postalCode => subscriptionRepository.getDirectionDepartementaleEmail(postalCode.take(2)))
          .getOrElse(Future(Seq()))
      } else Future(Seq())
    _ <-
      if (ddEmails.nonEmpty) {
        mailService.send(DgccrfDangerousProductReportNotification(ddEmails, report))
      } else {
        Future.unit
      }
  } yield ()

  private def notifyConsumer(report: Report, maybeCompany: Option[Company], reportAttachements: List[ReportFile]) = {
    val event = Event(
      UUID.randomUUID(),
      Some(report.id),
      maybeCompany.map(_.id),
      None,
      OffsetDateTime.now(),
      Constants.EventType.CONSO,
      Constants.ActionEvent.EMAIL_CONSUMER_ACKNOWLEDGMENT
    )
    for {
      _ <- mailService.send(ConsumerReportAcknowledgment(report, event, reportAttachements))
      _ <- eventRepository.create(
        Event(
          UUID.randomUUID(),
          Some(report.id),
          maybeCompany.map(_.id),
          None,
          OffsetDateTime.now(),
          Constants.EventType.CONSO,
          Constants.ActionEvent.EMAIL_CONSUMER_ACKNOWLEDGMENT
        )
      )
    } yield ()
  }

  private def notifyProfessionalIfNeeded(maybeCompany: Option[Company], report: Report) =
    (report.status, maybeCompany) match {
      case (ReportStatus.TraitementEnCours, Some(company)) =>
        notifyProfessionalOfNewReport(report, company)
      case _ => Future.successful(report)
    }

  private def extractOptionnalCountry(draftReport: ReportDraft) =
    draftReport.companyAddress.flatMap(_.country.map { country =>
      logger.debug(s"Found country ${country} from draft report")
      country.name
    })

  private def extractOptionnalCompany(draftReport: ReportDraft): Future[Option[Company]] =
    draftReport.companySiret match {
      case Some(siret) =>
        val company = Company(
          siret = siret,
          name = draftReport.companyName.get,
          address = draftReport.companyAddress.get,
          activityCode = draftReport.companyActivityCode,
          isHeadOffice = draftReport.companyIsHeadOffice.getOrElse(false),
          isOpen = draftReport.companyIsOpen.getOrElse(true)
        )
        companyRepository.getOrCreate(siret, company).map { company =>
          logger.debug("Company extracted from report")
          Some(company)
        }
      case None =>
        logger.debug("No company attached to report")
        Future(None)
    }

  def updateReportCompany(reportId: UUID, reportCompany: ReportCompany, userUUID: UUID): Future[Option[Report]] =
    for {
      existingReport <- reportRepository.get(reportId)
      company <- companyRepository.getOrCreate(
        reportCompany.siret,
        Company(
          siret = reportCompany.siret,
          name = reportCompany.name,
          address = reportCompany.address,
          activityCode = reportCompany.activityCode,
          isHeadOffice = reportCompany.isHeadOffice,
          isOpen = reportCompany.isOpen
        )
      )
      reportWithNewData <- existingReport match {
        case Some(report) =>
          reportRepository
            .update(
              report.id,
              report.copy(
                companyId = Some(company.id),
                companyName = Some(reportCompany.name),
                companyAddress = reportCompany.address,
                companySiret = Some(reportCompany.siret)
              )
            )
            .map(Some(_))
        case _ => Future(None)
      }
      reportWithNewStatus <- reportWithNewData
        .filter(_.companySiret != existingReport.flatMap(_.companySiret))
        .filter(_.creationDate.isAfter(OffsetDateTime.now(ZoneOffset.UTC).minusDays(7)))
        .map(report =>
          reportRepository
            .update(
              report.id,
              report.copy(
                status = report.initialStatus()
              )
            )
            .map(Some(_))
        )
        .getOrElse(Future(reportWithNewData))
      updatedReport <- reportWithNewStatus
        .filter(_.status == ReportStatus.TraitementEnCours)
        .filter(_.companySiret.isDefined)
        .filter(_.companySiret != existingReport.flatMap(_.companySiret))
        .map(r => notifyProfessionalOfNewReport(r, company).map(Some(_)))
        .getOrElse(Future(reportWithNewStatus))
      _ <- existingReport match {
        case Some(report) =>
          eventRepository
            .create(
              Event(
                UUID.randomUUID(),
                Some(report.id),
                Some(company.id),
                Some(userUUID),
                OffsetDateTime.now(),
                Constants.EventType.ADMIN,
                Constants.ActionEvent.REPORT_COMPANY_CHANGE,
                stringToDetailsJsValue(
                  s"Entreprise précédente : Siret ${report.companySiret
                      .getOrElse("non renseigné")} - ${Some(report.companyAddress.toString).filter(_ != "").getOrElse("Adresse non renseignée")}"
                )
              )
            )
            .map(Some(_))
        case _ => Future(None)
      }
      _ <- existingReport.flatMap(_.companyId).map(id => removeAccessToken(id)).getOrElse(Future(()))
    } yield updatedReport

  def updateReportConsumer(
      reportId: UUID,
      reportConsumer: ReportConsumerUpdate,
      userUUID: UUID
  ): Future[Option[Report]] =
    for {
      existingReport <- reportRepository.get(reportId)
      updatedReport <- existingReport match {
        case Some(report) =>
          reportRepository
            .update(
              report.id,
              report.copy(
                firstName = reportConsumer.firstName,
                lastName = reportConsumer.lastName,
                email = reportConsumer.email,
                contactAgreement = reportConsumer.contactAgreement,
                consumerReferenceNumber = reportConsumer.consumerReferenceNumber
              )
            )
            .map(Some(_))
        case _ => Future(None)
      }
      _ <- existingReport match {
        case Some(report) =>
          eventRepository
            .create(
              Event(
                UUID.randomUUID(),
                Some(report.id),
                report.companyId,
                Some(userUUID),
                OffsetDateTime.now(),
                Constants.EventType.ADMIN,
                Constants.ActionEvent.REPORT_CONSUMER_CHANGE,
                stringToDetailsJsValue(
                  s"Consommateur précédent : ${report.firstName} ${report.lastName} - ${report.email}" +
                    report.consumerReferenceNumber.map(nb => s" - ref $nb").getOrElse("") +
                    s"- Accord pour contact : ${if (report.contactAgreement) "oui" else "non"}"
                )
              )
            )
            .map(Some(_))
        case _ => Future(None)
      }
    } yield updatedReport

  def handleReportView(report: Report, user: User): Future[Report] =
    if (user.userRole == UserRole.Professionnel) {
      eventRepository
        .getEvents(report.id, EventFilter(None))
        .flatMap(events =>
          if (!events.exists(_.action == Constants.ActionEvent.REPORT_READING_BY_PRO)) {
            manageFirstViewOfReportByPro(report, user.id)
          } else {
            Future(report)
          }
        )
    } else {
      Future(report)
    }

  private def removeAccessToken(companyId: UUID) =
    for {
      company <- companyRepository.get(companyId)
      reports <- company
        .map(c => reportRepository.getReports(ReportFilter(companyIds = Seq(c.id))).map(_.entities))
        .getOrElse(Future(Nil))
      cnt <- if (reports.isEmpty) accessTokenRepository.removePendingTokens(company.get) else Future(0)
    } yield {
      logger.debug(s"Removed ${cnt} tokens for company ${companyId}")
      ()
    }

  def deleteReport(id: UUID) =
    for {
      report <- reportRepository.get(id)
      _ <- eventRepository.deleteByReportId(id)
      _ <- reportFileOrchestrator.removeFromReportId(id)
      _ <- reportConsumerReviewOrchestrator.remove(id)
      _ <- reportRepository.delete(id)
      _ <- report.flatMap(_.companyId).map(id => removeAccessToken(id)).getOrElse(Future(()))
    } yield report.isDefined

  private def manageFirstViewOfReportByPro(report: Report, userUUID: UUID) =
    for {
      _ <- eventRepository.create(
        Event(
          UUID.randomUUID(),
          Some(report.id),
          report.companyId,
          Some(userUUID),
          OffsetDateTime.now(),
          Constants.EventType.PRO,
          Constants.ActionEvent.REPORT_READING_BY_PRO
        )
      )
      updatedReport <-
        if (ReportStatus.isFinal(report.status)) {
          Future(report)
        } else {
          notifyConsumerOfReportTransmission(report)
        }
    } yield updatedReport

  private def notifyConsumerOfReportTransmission(report: Report): Future[Report] =
    for {
      newReport <- reportRepository.update(report.id, report.copy(status = ReportStatus.Transmis))
      _ <- mailService.send(ConsumerReportReadByProNotification(report))
      _ <- eventRepository.create(
        Event(
          id = UUID.randomUUID(),
          reportId = Some(report.id),
          companyId = report.companyId,
          userId = None,
          creationDate = OffsetDateTime.now(),
          eventType = Constants.EventType.CONSO,
          action = Constants.ActionEvent.EMAIL_CONSUMER_REPORT_READING
        )
      )
    } yield newReport

  private def sendMailsAfterProAcknowledgment(report: Report, reportResponse: ReportResponse, user: User) = for {
    _ <- mailService.send(ProResponseAcknowledgment(report, reportResponse, user))
    _ <- mailService.send(ConsumerProResponseNotification(report, reportResponse))
  } yield ()

  def newEvent(reportId: UUID, draftEvent: Event, user: User): Future[Option[Event]] =
    for {
      report <- reportRepository.get(reportId)
      newEvent <- report match {
        case Some(r) =>
          eventRepository
            .create(
              draftEvent.copy(
                id = UUID.randomUUID(),
                creationDate = OffsetDateTime.now(),
                reportId = Some(r.id),
                companyId = r.companyId,
                userId = Some(user.id)
              )
            )
            .map(Some(_))
        case _ => Future(None)
      }
      _ <- (report, newEvent) match {
        case (Some(r), Some(event)) =>
          reportRepository
            .update(
              r.id,
              r.copy(status = event.action match {
                case POST_ACCOUNT_ACTIVATION_DOC => ReportStatus.TraitementEnCours
                case _                           => r.status
              })
            )
            .map(Some(_))
        case _ => Future(None)
      }
    } yield {
      newEvent.foreach(event =>
        event.action match {
          case REPORT_READING_BY_PRO => notifyConsumerOfReportTransmission(report.get)
          case _                     => ()
        }
      )
      newEvent
    }

  def handleReportResponse(report: Report, reportResponse: ReportResponse, user: User): Future[Report] = {
    logger.debug(s"handleReportResponse ${reportResponse.responseType}")
    for {
      _ <- reportFileOrchestrator.attachFilesToReport(reportResponse.fileIds, report.id)
      updatedReport <- reportRepository.update(
        report.id,
        report.copy(
          status = ReportStatus.fromResponseType(reportResponse.responseType)
        )
      )
      _ <- eventRepository.create(
        Event(
          UUID.randomUUID(),
          Some(report.id),
          report.companyId,
          Some(user.id),
          OffsetDateTime.now(),
          EventType.PRO,
          ActionEvent.REPORT_PRO_RESPONSE,
          Json.toJson(reportResponse)
        )
      )
      _ <- sendMailsAfterProAcknowledgment(updatedReport, reportResponse, user)
      _ <- eventRepository.create(
        Event(
          UUID.randomUUID(),
          Some(report.id),
          updatedReport.companyId,
          None,
          OffsetDateTime.now(),
          Constants.EventType.CONSO,
          Constants.ActionEvent.EMAIL_CONSUMER_REPORT_RESPONSE
        )
      )
      _ <- eventRepository.create(
        Event(
          UUID.randomUUID(),
          Some(report.id),
          updatedReport.companyId,
          Some(user.id),
          OffsetDateTime.now(),
          Constants.EventType.PRO,
          Constants.ActionEvent.EMAIL_PRO_RESPONSE_ACKNOWLEDGMENT
        )
      )
    } yield updatedReport
  }

  def handleReportAction(report: Report, reportAction: ReportAction, user: User): Future[Event] =
    for {
      _ <- reportFileOrchestrator.attachFilesToReport(reportAction.fileIds, report.id)
      newEvent <- eventRepository.create(
        Event(
          UUID.randomUUID(),
          Some(report.id),
          report.companyId,
          Some(user.id),
          OffsetDateTime.now(),
          EventType.fromUserRole(user.userRole),
          reportAction.actionType,
          reportAction.details
            .map(details => Json.obj("description" -> details))
            .getOrElse(Json.toJson(reportAction))
        )
      )
    } yield {
      logger.debug(
        s"Create event ${newEvent.id} on report ${report.id} for reportActionType ${reportAction.actionType}"
      )
      newEvent
    }

  def getReportsForUser(
      connectedUser: User,
      filter: ReportFilter,
      offset: Option[Long],
      limit: Option[Int]
  ): Future[PaginatedResult[ReportWithFiles]] =
    for {
      sanitizedSirenSirets <- companiesVisibilityOrchestrator.filterUnauthorizedSiretSirenList(
        filter.siretSirenList,
        connectedUser
      )
      paginatedReportFiles <-
        if (sanitizedSirenSirets.isEmpty && connectedUser.userRole == UserRole.Professionnel) {
          Future(PaginatedResult(totalCount = 0, hasNextPage = false, entities = List.empty[ReportWithFiles]))
        } else {
          getReportsWithFile[ReportWithFiles](
            filter.copy(siretSirenList = sanitizedSirenSirets),
            offset,
            limit,
            (r: Report, m: Map[UUID, List[ReportFile]]) => ReportWithFiles(r, m.getOrElse(r.id, Nil))
          )
        }
    } yield paginatedReportFiles

  def getReportsWithFile[T](
      filter: ReportFilter,
      offset: Option[Long],
      limit: Option[Int],
      toApi: (Report, Map[UUID, List[ReportFile]]) => T
  ): Future[PaginatedResult[T]] = {
    val maxResults = signalConsoConfiguration.reportsExportLimitMax
    for {
      _ <- limit match {
        case Some(limitValue) if limitValue > maxResults =>
          logger.error(s"Max page size reached $limitValue > $maxResults")
          Future.failed(ExternalReportsMaxPageSizeExceeded(maxResults))
        case a => Future.successful(a)
      }
      validLimit = limit.orElse(Some(maxResults))
      validOffset = offset.orElse(Some(0L))
      paginatedReports <-
        reportRepository.getReports(
          filter,
          validOffset,
          validLimit
        )
      reportFilesMap <- reportFileOrchestrator.prefetchReportsFiles(paginatedReports.entities.map(_.id))
    } yield paginatedReports.copy(entities = paginatedReports.entities.map(r => toApi(r, reportFilesMap)))
  }

  def getVisibleReportForUser(reportId: UUID, user: User): Future[Option[Report]] =
    for {
      report <- reportRepository.get(reportId)
      visibleReport <-
        if (Seq(UserRole.DGCCRF, UserRole.Admin).contains(user.userRole))
          Future(report)
        else {
          companiesVisibilityOrchestrator
            .fetchVisibleCompanies(user)
            .map(_.map(v => Some(v.company.siret)))
            .map { visibleSirets =>
              report.filter(r => visibleSirets.contains(r.companySiret))
            }
        }
    } yield visibleReport

  def getCloudWord(companyId: UUID): Future[List[ReportWordOccurrence]] =
    for {
      maybeCompany <- companyRepository.get(companyId)
      company <- maybeCompany.liftTo[Future](AppError.CompanyNotFound(companyId))
      wordOccurenceList <- reportRepository.cloudWord(companyId)
    } yield wordOccurenceList
      .filterNot { wordOccurrence =>
        wordOccurrence.value.exists(_.isDigit) ||
        wordOccurrence.count < 10 ||
        StopWords.contains(wordOccurrence.value) ||
        wordOccurrence.value.contains(company.name.toLowerCase)
      }
      .sortWith(_.count > _.count)
      .slice(0, 50)

}
