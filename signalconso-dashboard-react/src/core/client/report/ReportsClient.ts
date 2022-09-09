import {
  Address,
  CompanySearchResult,
  Id,
  PaginatedData,
  PaginatedFilters,
  paginateFilters2QueryString,
  Report,
  ReportAction,
  ReportResponse,
  ReportSearch,
  ReportSearchResult,
  ReportTag,
  ReportWordCount,
  ResponseConsumerReview,
  ReportConsumerUpdate,
} from '../../model'
import {ApiSdkLogger} from '../../helper/Logger'
import {ApiClientApi} from '../ApiClient'
import {cleanObject, dateToApiDate, dateToApiTime, directDownloadBlob} from '../../helper'

export interface ReportFilterQuerystring {
  readonly departments?: string[]
  readonly withTags?: ReportTag[]
  readonly withoutTags?: ReportTag[]
  readonly companyCountries?: string[]
  readonly siretSirenList?: string[]
  readonly status?: string[]
  start?: string
  end?: string
  email?: string
  websiteURL?: string
  phone?: string
  category?: string
  details?: string
  hasWebsite?: 'true' | 'false'
  hasPhone?: 'true' | 'false'
  hasForeignCountry?: 'true' | 'false'
  hasCompany?: 'true' | 'false'
}

export const reportFilter2QueryString = (report: ReportSearch): ReportFilterQuerystring => {
  try {
    const {hasCompany, hasForeignCountry, hasWebsite, hasPhone, start, end, ...r} = report
    const parseBoolean = (_: keyof Pick<ReportSearch, 'hasForeignCountry' | 'hasWebsite' | 'hasPhone' | 'hasCompany'>) =>
      report[_] !== undefined && {[_]: ('' + report[_]) as 'true' | 'false'}
    const parseDate = (_: keyof Pick<ReportSearch, 'start' | 'end'>) => (report[_] ? {[_]: dateToApiTime(report[_])} : {})
    return {
      ...r,
      ...parseBoolean('hasCompany'),
      ...parseBoolean('hasWebsite'),
      ...parseBoolean('hasPhone'),
      ...parseBoolean('hasForeignCountry'),
      ...parseDate('start'),
      ...parseDate('end'),
    }
  } catch (e) {
    ApiSdkLogger.error('Caught error on "reportFilter2QueryString"', report, e)
    return {}
  }
}

export const cleanReportFilter = (filter: ReportSearch): ReportSearch => {
  if (filter.hasCompany === false) {
    delete filter.siretSirenList
  }
  if (filter.hasForeignCountry === false) {
    delete filter.companyCountries
  }
  if (filter.hasWebsite === false) {
    delete filter.websiteURL
  }
  if (filter.hasPhone === false) {
    delete filter.phone
  }
  return filter
}

export class ReportsClient {
  constructor(private client: ApiClientApi) {}

  readonly extract = (filters: ReportSearch & PaginatedFilters) => {
    return this.client.post<void>(`reports/extract`, {
      qs: cleanObject({
        ...reportFilter2QueryString(cleanReportFilter(filters)),
        ...paginateFilters2QueryString(filters),
        zone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      }),
    })
  }

  readonly search = (filters: ReportSearch & PaginatedFilters) => {
    const qs = cleanObject(reportFilter2QueryString(cleanReportFilter(filters)))
    return this.client.get<PaginatedData<ReportSearchResult>>(`/reports`, {qs}).then(result => {
      result.entities.forEach(entity => {
        entity.report = ReportsClient.mapReport(entity.report)
      })
      return result
    })
  }

  readonly download = (ids: Id[]) => {
    // TODO Type it and maybe improve it
    return this.client.getPdf<any>(`/reports/download`, {qs: {ids}}).then(directDownloadBlob('Signalement.pdf'))
  }

  readonly remove = (id: Id) => {
    return this.client.delete<void>(`reports/${id}`)
  }

  readonly getById = (id: Id): Promise<ReportSearchResult> => {
    return this.client.get(`/reports/${id}`).then(_ => ({files: _.files, report: ReportsClient.mapReport(_.report)}))
  }

  readonly getReviewOnReportResponse = (reportId: Id) => {
    return this.client.get<ResponseConsumerReview>(`/reports/${reportId}/response/review`)
  }

  readonly getCloudWord = (companyId: Id) => {
    return this.client.get<ReportWordCount[]>(`/reports/cloudword/${companyId}`)
  }

  readonly postResponse = (id: Id, response: ReportResponse) => {
    return this.client.post<Event>(`reports/${id}/response`, {body: {...response, fileIds: response.fileIds ?? []}})
  }

  readonly postAction = (id: Id, action: ReportAction) => {
    // const mappedAction: any = {...action, actionType: {value: action.actionType}}
    return this.client.post<Event>(`reports/${id}/action`, {body: action})
  }

  readonly updateReportCompany = (reportId: string, company: CompanySearchResult) => {
    return this.client.post<Report>(`/reports/${reportId}/company`, {
      body: {
        name: company.name,
        address: company.address,
        siret: company.siret,
        activityCode: company.activityCode,
        isHeadOffice: company.isHeadOffice,
        isOpen: company.isOpen,
      },
    })
  }

  readonly updateReportConsumer = (reportId: string, reportConsumerUpdate: ReportConsumerUpdate) => {
    return this.client.post(`reports/${reportId}/consumer`, {
      body: reportConsumerUpdate,
    })
  }

  readonly getCountByDepartments = ({start, end}: {start?: Date; end?: Date} = {}): Promise<[string, number][]> => {
    return this.client.get(`/reports/count-by-departments`, {
      qs: {
        start: dateToApiDate(start),
        end: dateToApiDate(end),
      },
    })
  }

  static readonly mapReport = (report: {[key in keyof Report]: any}): Report => ({
    ...report,
    companyAddress: ReportsClient.mapAddress(report.companyAddress),
    creationDate: new Date(report.creationDate),
  })

  static readonly mapAddress = (address: {[key in keyof Address]: any | undefined}): Address => ({
    ...address,
    country: address.country?.name,
  })
}
