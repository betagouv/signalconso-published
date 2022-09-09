package actors

import akka.actor._
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import spoiwo.model._
import spoiwo.model.enums.CellFill
import spoiwo.model.enums.CellHorizontalAlignment
import spoiwo.model.enums.CellStyleInheritance
import spoiwo.model.enums.CellVerticalAlignment
import spoiwo.natures.xlsx.Model2XlsxConversions._
import config.SignalConsoConfiguration
import controllers.routes
import models._
import models.event.Event
import models.report.Report
import models.report.ReportFile
import models.report.ReportFileOrigin
import models.report.ReportFilter
import models.report.ReportResponse
import models.report.ReportStatus
import orchestrators.ReportOrchestrator
import play.api.Logger
import repositories.asyncfiles.AsyncFileRepositoryInterface
import repositories.companyaccess.CompanyAccessRepositoryInterface
import repositories.event.EventRepositoryInterface
import repositories.reportfile.ReportFileRepositoryInterface
import services.S3ServiceInterface
import utils.Constants
import utils.Constants.Departments
import utils.DateUtils.frenchFormatDate
import utils.DateUtils.frenchFormatDateAndTime
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.Random
import java.time.ZoneId
import java.time.OffsetDateTime

object ReportsExtractActor {
  def props = Props[ReportsExtractActor]()

  case class ExtractRequest(fileId: UUID, requestedBy: User, filters: ReportFilter, zone: ZoneId)
}

class ReportsExtractActor(
    reportFileRepository: ReportFileRepositoryInterface,
    companyAccessRepository: CompanyAccessRepositoryInterface,
    reportOrchestrator: ReportOrchestrator,
    eventRepository: EventRepositoryInterface,
    asyncFileRepository: AsyncFileRepositoryInterface,
    s3Service: S3ServiceInterface,
    signalConsoConfiguration: SignalConsoConfiguration
)(implicit val mat: Materializer)
    extends Actor {
  import ReportsExtractActor._
  implicit val ec: ExecutionContext = context.dispatcher

  val logger: Logger = Logger(this.getClass)
  override def preStart() =
    logger.debug("Starting")
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    logger.debug(s"Restarting due to [${reason.getMessage}] when processing [${message.getOrElse("")}]")
  override def receive = {
    case ExtractRequest(fileId: UUID, requestedBy: User, filters: ReportFilter, zone: ZoneId) =>
      for {
        // FIXME: We might want to move the random name generation
        // in a common place if we want to reuse it for other async files
        tmpPath <- genTmpFile(requestedBy, filters, zone)
        remotePath <- saveRemotely(tmpPath, tmpPath.getFileName.toString)
        _ <- asyncFileRepository.update(fileId, tmpPath.getFileName.toString, remotePath)
      } yield logger.debug(s"Built report for User ${requestedBy.id} — async file ${fileId}")
      ()
    case _ =>
      logger.debug("Could not handle request")
      ()
  }

  // Common layout variables
  val headerStyle = CellStyle(
    fillPattern = CellFill.Solid,
    fillForegroundColor = Color.Gainsborough,
    font = Font(bold = true),
    horizontalAlignment = CellHorizontalAlignment.Center
  )
  val centerAlignmentStyle = CellStyle(
    horizontalAlignment = CellHorizontalAlignment.Center,
    verticalAlignment = CellVerticalAlignment.Center,
    wrapText = true
  )
  val leftAlignmentStyle = CellStyle(
    horizontalAlignment = CellHorizontalAlignment.Left,
    verticalAlignment = CellVerticalAlignment.Center,
    wrapText = true
  )
  val leftAlignmentColumn = Column(autoSized = true, style = leftAlignmentStyle)
  val centerAlignmentColumn = Column(autoSized = true, style = centerAlignmentStyle)

  // Columns definition
  case class ReportColumn(
      name: String,
      column: Column,
      extract: (Report, List[ReportFile], List[Event], List[User]) => String,
      available: Boolean = true
  )
  def buildColumns(requestedBy: User, zone: ZoneId) = {
    List(
      ReportColumn(
        "Date de création",
        centerAlignmentColumn,
        (report, _, _, _) => frenchFormatDate(report.creationDate, zone)
      ),
      ReportColumn(
        "Département",
        centerAlignmentColumn,
        (report, _, _, _) => report.companyAddress.postalCode.flatMap(Departments.fromPostalCode).getOrElse("")
      ),
      ReportColumn(
        "Code postal",
        centerAlignmentColumn,
        (report, _, _, _) => report.companyAddress.postalCode.getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Pays",
        centerAlignmentColumn,
        (report, _, _, _) => report.companyAddress.country.map(_.name).getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Siret",
        centerAlignmentColumn,
        (report, _, _, _) => report.companySiret.map(_.value).getOrElse("")
      ),
      ReportColumn(
        "Nom de l'entreprise",
        leftAlignmentColumn,
        (report, _, _, _) => report.companyName.getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Adresse de l'entreprise",
        leftAlignmentColumn,
        (report, _, _, _) => report.companyAddress.toString,
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Email de l'entreprise",
        centerAlignmentColumn,
        (_, _, _, companyAdmins) => companyAdmins.map(_.email).mkString(","),
        available = requestedBy.userRole == UserRole.Admin
      ),
      ReportColumn(
        "Site web de l'entreprise",
        centerAlignmentColumn,
        (report, _, _, _) => report.websiteURL.websiteURL.map(_.value).getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Téléphone de l'entreprise",
        centerAlignmentColumn,
        (report, _, _, _) => report.phone.getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Vendeur (marketplace)",
        centerAlignmentColumn,
        (report, _, _, _) => report.vendor.getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Catégorie",
        leftAlignmentColumn,
        (report, _, _, _) => report.category
      ),
      ReportColumn(
        "Sous-catégories",
        leftAlignmentColumn,
        (report, _, _, _) => report.subcategories.filter(s => s != null).mkString("\n").replace("&#160;", " ")
      ),
      ReportColumn(
        "Détails",
        Column(width = new Width(100, WidthUnit.Character), style = leftAlignmentStyle),
        (report, _, _, _) => report.details.map(d => s"${d.label} ${d.value}").mkString("\n").replace("&#160;", " ")
      ),
      ReportColumn(
        "Pièces jointes",
        leftAlignmentColumn,
        (_, files, _, _) =>
          files
            .filter(file => file.origin == ReportFileOrigin.CONSUMER)
            .map(file =>
              s"${signalConsoConfiguration.apiURL.toString}${routes.ReportFileController
                  .downloadReportFile(file.id, file.filename)
                  .url}"
            )
            .mkString("\n"),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Statut",
        leftAlignmentColumn,
        (report, _, _, _) => ReportStatus.translate(report.status, requestedBy.userRole),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Réponse au consommateur",
        leftAlignmentColumn,
        (report, _, events, _) =>
          Some(report.status)
            .filter(
              List(
                ReportStatus.PromesseAction,
                ReportStatus.MalAttribue,
                ReportStatus.Infonde
              ) contains _
            )
            .flatMap(_ =>
              events
                .find(event => event.action == Constants.ActionEvent.REPORT_PRO_RESPONSE)
                .map(e => e.details.validate[ReportResponse].get.consumerDetails)
            )
            .getOrElse("")
      ),
      ReportColumn(
        "Réponse à la DGCCRF",
        leftAlignmentColumn,
        (report, _, events, _) =>
          Some(report.status)
            .filter(
              List(
                ReportStatus.PromesseAction,
                ReportStatus.MalAttribue,
                ReportStatus.Infonde
              ) contains _
            )
            .flatMap(_ =>
              events
                .find(event => event.action == Constants.ActionEvent.REPORT_PRO_RESPONSE)
                .flatMap(e => e.details.validate[ReportResponse].get.dgccrfDetails)
            )
            .getOrElse("")
      ),
      ReportColumn(
        "Identifiant",
        centerAlignmentColumn,
        (report, _, _, _) => report.id.toString,
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Prénom",
        leftAlignmentColumn,
        (report, _, _, _) => report.firstName,
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Nom",
        leftAlignmentColumn,
        (report, _, _, _) => report.lastName,
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Email",
        leftAlignmentColumn,
        (report, _, _, _) => report.email.value,
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Téléphone",
        leftAlignmentColumn,
        (report, _, _, _) => report.consumerPhone.getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Numéro de référence dossier",
        leftAlignmentColumn,
        (report, _, _, _) => report.consumerReferenceNumber.getOrElse(""),
        available = List(UserRole.DGCCRF, UserRole.Admin) contains requestedBy.userRole
      ),
      ReportColumn(
        "Accord pour contact",
        centerAlignmentColumn,
        (report, _, _, _) => if (report.contactAgreement) "Oui" else "Non"
      ),
      ReportColumn(
        "Actions DGCCRF",
        leftAlignmentColumn,
        (_, _, events, _) =>
          events
            .filter(event => event.eventType == Constants.EventType.DGCCRF)
            .map(event =>
              s"Le ${frenchFormatDate(event.creationDate, zone)} : ${event.action.value} - ${event.getDescription}"
            )
            .mkString("\n"),
        available = requestedBy.userRole == UserRole.DGCCRF
      ),
      ReportColumn(
        "Contrôle effectué",
        centerAlignmentColumn,
        (
            _,
            _,
            events,
            _
        ) => if (events.exists(event => event.action == Constants.ActionEvent.CONTROL)) "Oui" else "Non",
        available = requestedBy.userRole == UserRole.DGCCRF
      )
    ).filter(_.available)
  }

  def genTmpFile(requestedBy: User, filters: ReportFilter, zone: ZoneId) = {
    val reportColumns = buildColumns(requestedBy, zone)
    for {
      paginatedReports <- reportOrchestrator
        .getReportsForUser(
          requestedBy,
          filter = filters,
          offset = Some(0),
          limit = Some(signalConsoConfiguration.reportsExportLimitMax)
        )
        .map(_.entities.map(_.report))
      reportFilesMap <- reportFileRepository.prefetchReportsFiles(paginatedReports.map(_.id))
      reportEventsMap <- eventRepository.prefetchReportsEvents(paginatedReports)
      companyAdminsMap <- companyAccessRepository.fetchUsersByCompanyId(
        paginatedReports.flatMap(_.companyId),
        Seq(AccessLevel.ADMIN)
      )
    } yield {
      val targetFilename = s"signalements-${Random.alphanumeric.take(12).mkString}.xlsx"
      val reportsSheet = Sheet(name = "Signalements")
        .withRows(
          Row(style = headerStyle).withCellValues(reportColumns.map(_.name)) ::
            paginatedReports.map(report =>
              Row().withCells(
                reportColumns
                  .map(
                    _.extract(
                      report,
                      reportFilesMap.getOrElse(report.id, Nil),
                      reportEventsMap.getOrElse(report.id, Nil),
                      report.companyId.flatMap(companyAdminsMap.get).getOrElse(Nil)
                    )
                  )
                  .map(StringCell(_, None, None, CellStyleInheritance.CellThenRowThenColumnThenSheet))
              )
            )
        )
        .withColumns(reportColumns.map(_.column))

      val filtersSheet = Sheet(name = "Filtres")
        .withRows(
          List(
            Some(
              Row().withCellValues(
                "Date de l'export",
                frenchFormatDateAndTime(OffsetDateTime.now, zone)
              )
            ),
            Some(filters.departments)
              .filter(_.nonEmpty)
              .map(departments => Row().withCellValues("Départment(s)", departments.mkString(","))),
            (filters.start, filters.end) match {
              case (Some(startDate), Some(endDate)) =>
                Some(
                  Row().withCellValues(
                    "Période",
                    s"Du ${frenchFormatDate(startDate, zone)} au ${frenchFormatDate(endDate, zone)}"
                  )
                )
              case (Some(startDate), _) =>
                Some(Row().withCellValues("Période", s"Depuis le ${frenchFormatDate(startDate, zone)}"))
              case (_, Some(endDate)) =>
                Some(Row().withCellValues("Période", s"Jusqu'au ${frenchFormatDate(endDate, zone)}"))
              case (_) => None
            },
            Some(Row().withCellValues("Siret", filters.siretSirenList.mkString(","))),
            filters.websiteURL.map(websiteURL => Row().withCellValues("Site internet", websiteURL)),
            filters.phone.map(phone => Row().withCellValues("Numéro de téléphone", phone)),
            Some(filters.status)
              .filter(_.nonEmpty)
              .map(status =>
                Row()
                  .withCellValues("Statut", status.map(ReportStatus.translate(_, requestedBy.userRole)).mkString(","))
              ),
            filters.category.map(category => Row().withCellValues("Catégorie", category)),
            filters.details.map(details => Row().withCellValues("Mots clés", details))
          ).filter(_.isDefined).map(_.get)
        )
        .withColumns(
          Column(autoSized = true, style = headerStyle),
          leftAlignmentColumn
        )

      val localPath = Paths.get(signalConsoConfiguration.tmpDirectory, targetFilename)
      Workbook(reportsSheet, filtersSheet).saveAsXlsx(localPath.toString)
      logger.debug(s"Generated extract locally: ${localPath}")
      localPath
    }
  }

  def saveRemotely(localPath: Path, remoteName: String) = {
    val remotePath = s"extracts/${remoteName}"
    s3Service.upload(remotePath).runWith(FileIO.fromPath(localPath)).map(_ => remotePath)
  }

}
