import {Id} from '../../model'

export interface ReportEvent {
  data: Event
  user?: EventUser
}

export interface Event {
  id: Id
  reportId?: Id
  creationDate: Date
  userId?: Id
  eventType: EventType
  action: EventActionValues
  details: {description: string} | ReportResponse
}

export type EventType = 'PRO' | 'CONSO' | 'DGCCRF' | 'ADMIN' | 'SYSTEM'

export interface EventUser {
  firstName: string
  lastName: string
  role: string
}

export enum EventActionValues {
  Creation = 'Signalement du consommateur',
  PostAccountActivationDoc = "Envoi du courrier d'activation",
  AccountActivation = "Activation d'un compte",
  ActivationDocReturned = "Courrier d'activation retourné",
  ActivationDocRequired = "Courrier d'activation à renvoyer",
  CompanyAddressChange = "Modification de l'adresse de l'entreprise",
  ReportReadingByPro = 'Première consultation du signalement par le professionnel',
  ReportProResponse = 'Réponse du professionnel au signalement',
  ReportReviewOnResponse = 'Avis du consommateur sur la réponse du professionnel',
  ReportClosedByNoReading = 'Signalement non consulté',
  ReportClosedByNoAction = 'Signalement consulté ignoré',
  EmailConsumerAcknowledgment = 'Email « Accusé de réception » envoyé au consommateur',
  EmailConsumerReportReading = 'Email « Signalement consulté » envoyé au consommateur',
  EmailConsumerReportResponse = "Email « L'entreprise a répondu à votre signalement » envoyé au consommateur",
  EmailConsumerReportClosedByNoReading = "Email « L'entreprise n'a pas souhaité consulter votre signalement » envoyé au consommateur",
  EmailConsumerReportClosedByNoAction = "Email « L'entreprise n'a pas répondu au signalement » envoyé au consommateur",
  EmailProNewReport = 'Email « Nouveau signalement » envoyé au professionnel',
  EmailProResponseAcknowledgment = 'Email « Accusé de réception de la réponse » envoyé au professionnel',
  EmailProRemindNoReading = 'Email « Nouveau signalement non consulté » envoyé au professionnel',
  EmailProRemindNoAction = 'Email « Nouveau signalement en attente de réponse » envoyé au professionnel',
  ReportCompanyChange = 'Modification du commerçant',
  ReportConsumerChange = 'Modification du consommateur',
  Comment = "Ajout d'un commentaire",
  ConsumerAttachments = 'Ajout de pièces jointes fournies par le consommateur',
  ProfessionalAttachments = "Ajout de pièces jointes fournies par l'entreprise",
  Control = 'Contrôle effectué',
}

export interface ReportResponse {
  responseType: ReportResponseTypes
  consumerDetails: string
  dgccrfDetails: string
  fileIds?: string[]
}

export enum ReportResponseTypes {
  Accepted = 'ACCEPTED',
  Rejected = 'REJECTED',
  NotConcerned = 'NOT_CONCERNED',
}

export enum ResponseEvaluation {
  Negative = 'Negative',
  Neutral = 'Neutral',
  Positive = 'Positive',
}

export interface ResponseConsumerReview {
  evaluation: ResponseEvaluation
  details?: string
}

export interface ReportWordCount {
  value: string
  count: number
}

export interface ReportAction {
  actionType: EventActionValues
  details?: string
  fileIds: string[]
}
