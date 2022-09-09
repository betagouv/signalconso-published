import {Entity, Id, PaginatedFilters} from '../../model'
import {Company} from '../company/Company'
import {Address} from '../../model'
import {Country} from '../constant/Country'

export enum IdentificationStatus {
  Identified = 'Identified',
  NotIdentified = 'NotIdentified',
}

export enum InvestigationStatus {
  NotProcessed = 'NotProcessed',
  SignalConsoIdentificationFailed = 'SignalConsoIdentificationFailed',
  Processing = 'Processing',
  UnderInvestigation = 'UnderInvestigation',
  InvestigationDone = 'InvestigationDone',
}

export enum Practice {
  DropShipping = 'DropShipping',
}

export interface DepartmentDivision {
  code: string
  name: string
}

export interface WebsiteInvestigation {
  practice?: Practice
  investigationStatus?: InvestigationStatus
  identificationStatus?: IdentificationStatus
  attribution?: string
  lastUpdated?: Date
  creationDate: Date
  id: Id
}

export interface Website extends Entity {
  creationDate: Date
  host: string
  companyId: Id
  identificationStatus: IdentificationStatus
  practice?: Practice
  investigationStatus?: InvestigationStatus
  attribution?: string
  lastUpdated?: Date
}

export interface WebsiteUpdateCompany {
  siret: string
  name?: string
  address?: Address
  activityCode?: string
  isHeadOffice: boolean
  isOpen: boolean
}

export interface WebsiteWithCompany extends Website {
  company?: Company
  companyCountry?: Country
  count?: 0
}

export interface ApiHostWithReportCount {
  host: string
  count: number
}

export interface WebsiteWithCompanySearch extends PaginatedFilters {
  host?: string
  identificationStatus?: IdentificationStatus[]
  practice?: Practice[]
  investigationStatus?: InvestigationStatus[]
  attribution?: string[]
  start?: Date
  end?: Date
}

export interface HostReportCountSearch extends PaginatedFilters {
  q?: string
  start?: Date
  end?: Date
}
