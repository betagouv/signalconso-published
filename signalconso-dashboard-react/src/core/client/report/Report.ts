import {Address, UploadedFile} from '../../model'
import format from 'date-fns/format'

export const ReportingDateLabel = 'Date du constat'
export const ReportingTimeslotLabel = 'Heure du constat'
export const DescriptionLabel = 'Description'

export enum Gender {
  Male = 'Male',
  Female = 'Female',
}

export enum CompanyKinds {
  SIRET = 'SIRET',
  WEBSITE = 'WEBSITE',
  PHONE = 'PHONE',
  LOCATION = 'LOCATION',
  INFLUENCEUR = 'INFLUENCEUR',
}

export interface Category {
  id: string
  title: string
  subcategoriesTitle?: string
  subcategories?: Subcategory[]
  companyKind?: CompanyKinds
}

export enum DetailInputType {
  TEXT = 'TEXT',
  DATE_NOT_IN_FUTURE = 'DATE_NOT_IN_FUTURE',
  DATE = 'DATE',
  TIMESLOT = 'TIMESLOT',
  RADIO = 'RADIO',
  CHECKBOX = 'CHECKBOX',
  TEXTAREA = 'TEXTAREA',
}

export interface DetailInput {
  label: string
  /** @deprecated */
  rank?: number
  type: DetailInputType
  placeholder?: string
  options?: string[]
  defaultValue?: string
  example?: string
  optionnal?: boolean
}

export interface Information {
  title?: string
  content?: string
  actions?: Action[]
  subTitle?: string
  outOfScope?: boolean
}

export interface Action {
  question: string
  example?: string
  answer: string
}

export enum ReportTag {
  LitigeContractuel = 'LitigeContractuel',
  Hygiene = 'Hygiene',
  ProduitDangereux = 'ProduitDangereux',
  DemarchageADomicile = 'DemarchageADomicile',
  Ehpad = 'Ehpad',
  DemarchageTelephonique = 'DemarchageTelephonique',
  AbsenceDeMediateur = 'AbsenceDeMediateur',
  Bloctel = 'Bloctel',
  Influenceur = 'Influenceur',
  ReponseConso = 'ReponseConso',
  Internet = 'Internet',
  ProduitIndustriel = 'ProduitIndustriel',
  ProduitAlimentaire = 'ProduitAlimentaire',
  CompagnieAerienne = 'CompagnieAerienne',
}

export interface SubcategoryBase extends Category {
  description?: string
  tags?: ReportTag[]
  example?: string
  reponseconsoCode?: string[]
  ccrfCode?: string[]
}

export interface SubcategoryInput extends SubcategoryBase {
  detailTitle?: string
  fileLabel?: string
  detailInputs?: DetailInput[]
}

export interface SubcategoryInformation extends SubcategoryBase {
  information: Information
}

export type Subcategory = SubcategoryBase | SubcategoryInput | SubcategoryInformation

export interface Report {
  id: string
  gender?: Gender
  category: string
  subcategories: Subcategory[]
  tags: ReportTag[]
  companyId: string
  companyName: string
  companyAddress: Address
  companySiret?: string
  websiteURL?: string
  vendor?: string
  phone?: string
  details: DetailInputValue[]
  firstName: string
  lastName: string
  email: string
  consumerPhone?: string
  consumerReferenceNumber?: string
  employeeConsumer: boolean
  contactAgreement: boolean
  creationDate: Date
  status: ReportStatus
  reponseconsoCode: string[]
  ccrfCode: string[]
}

export interface DetailInputValue {
  label: string
  value: string
}

export interface ReportSearchResult {
  report: Report
  files: UploadedFile[]
}

export enum ReportStatus {
  NA = 'NA',
  LanceurAlerte = 'LanceurAlerte',
  TraitementEnCours = 'TraitementEnCours',
  Transmis = 'Transmis',
  PromesseAction = 'PromesseAction',
  Infonde = 'Infonde',
  NonConsulte = 'NonConsulte',
  ConsulteIgnore = 'ConsulteIgnore',
  MalAttribue = 'MalAttribue',
}

export enum ReportStatusPro {
  NonConsulte = 'NonConsulte',
  ARepondre = 'ARepondre',
  Cloture = 'Cloture',
}

export type ReportConsumerUpdate = Pick<
  Report,
  'firstName' | 'lastName' | 'email' | 'contactAgreement' | 'consumerReferenceNumber'
>

export class Report {
  static readonly closedStatus = [
    ReportStatus.PromesseAction,
    ReportStatus.Infonde,
    ReportStatus.NonConsulte,
    ReportStatus.ConsulteIgnore,
    ReportStatus.MalAttribue,
  ]

  static readonly transmittedStatus = [
    ReportStatus.TraitementEnCours,
    ReportStatus.Transmis,
    ReportStatus.PromesseAction,
    ReportStatus.Infonde,
    ReportStatus.NonConsulte,
    ReportStatus.ConsulteIgnore,
    ReportStatus.MalAttribue,
  ]

  static readonly readStatus = [
    ReportStatus.Transmis,
    ReportStatus.PromesseAction,
    ReportStatus.Infonde,
    ReportStatus.MalAttribue,
    ReportStatus.ConsulteIgnore,
  ]

  static readonly respondedStatus = [ReportStatus.PromesseAction, ReportStatus.Infonde, ReportStatus.MalAttribue]

  static readonly isClosed = (status: ReportStatus) => {
    return Report.closedStatus.includes(status)
  }

  private static readonly mapStatusPro: {[key in ReportStatus]: () => ReportStatusPro} = {
    [ReportStatus.NA]: () => {
      throw new Error(`Invalid status`)
    },
    [ReportStatus.LanceurAlerte]: () => {
      throw new Error(`Invalid status`)
    },
    [ReportStatus.TraitementEnCours]: () => ReportStatusPro.NonConsulte,
    [ReportStatus.Transmis]: () => ReportStatusPro.ARepondre,
    [ReportStatus.PromesseAction]: () => ReportStatusPro.Cloture,
    [ReportStatus.Infonde]: () => ReportStatusPro.Cloture,
    [ReportStatus.NonConsulte]: () => ReportStatusPro.Cloture,
    [ReportStatus.ConsulteIgnore]: () => ReportStatusPro.Cloture,
    [ReportStatus.MalAttribue]: () => ReportStatusPro.Cloture,
  }

  private static mapStatusProInverted: {[key in ReportStatusPro]: () => ReportStatus[]} = Object.entries(
    Report.mapStatusPro,
  ).reduce((acc, [status, statusProFn]) => {
    try {
      const statusPro = statusProFn()
      const prevStatus = acc[statusPro] ? acc[statusPro]() : []
      acc[statusPro] = () => [...prevStatus, status as ReportStatus]
      return acc
    } catch {
      return acc
    }
  }, {} as {[key in ReportStatusPro]: () => ReportStatus[]})

  static readonly getStatusProByStatus = (status: ReportStatus): ReportStatusPro => Report.mapStatusPro[status]()

  static readonly getStatusByStatusPro = (status: ReportStatusPro): ReportStatus[] => Report.mapStatusProInverted[status]()

  static readonly isGovernmentCompany = (_?: {activityCode?: string}): boolean => _?.activityCode?.startsWith('84.') ?? false
}
