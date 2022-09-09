import {
  Company,
  CompanyCreation,
  CompanySearch,
  CompanyToActivate,
  CompanyUpdate,
  CompanyWithAccessLevel,
  CompanyWithReportsCount,
  Event,
  Id,
  Paginate,
} from '../../model'
import {format} from 'date-fns'
import {dateToApiDate, directDownloadBlob} from '../../helper'
import {ApiClientApi} from '../ApiClient'

export class CompanyClient {
  constructor(private client: ApiClientApi) {}

  readonly search = (search: CompanySearch): Promise<Paginate<CompanyWithReportsCount>> => {
    return this.client.get<Paginate<CompanyWithReportsCount>>(`/companies`, {qs: search}).then(res => ({
      ...res,
      entities: res.entities.map(CompanyClient.mapCompany),
    }))
  }

  readonly byId = (id: Id): Promise<Paginate<CompanyWithReportsCount>> => {
    return this.client.get<Paginate<CompanyWithReportsCount>>(`/companies/${id}`).then(res => ({
      ...res,
      entities: res.entities.map(CompanyClient.mapCompany),
    }))
  }

  readonly updateAddress = (id: Id, update: CompanyUpdate) => {
    return this.client.put<Company>(`/companies/${id}/address`, {body: update})
  }

  readonly getResponseRate = (id: Id) => {
    return this.client.get<number>(`/companies/${id}/response-rate`)
  }

  readonly saveUndeliveredDocument = (siret: string, returnedDate: Date) => {
    return this.client.post<Event>(`/companies/${siret}/undelivered-document`, {
      body: {returnedDate: dateToApiDate(returnedDate)},
    })
  }

  readonly create = (company: CompanyCreation) => {
    return this.client.post<Company>(`/companies`, {body: company})
  }

  readonly downloadActivationDocument = (companyIds: Id[]) => {
    return this.client
      .postGetPdf(`/companies/activation-document`, {body: {companyIds}})
      .then(directDownloadBlob(`signalement_depot_${format(new Date(), 'ddMMyy')}`))
  }

  readonly getHosts = (id: Id) => {
    return this.client.get<string[]>(`/companies/hosts/${id}`)
  }

  readonly getAccessibleByPro = () => {
    return this.client.get<CompanyWithAccessLevel[]>(`/companies/connected-user`)
  }

  readonly fetchToActivate = () => {
    return this.client.get<CompanyToActivate[]>(`/companies/to-activate`).then(_ =>
      _.map(_ => {
        _.lastNotice = _.lastNotice ? new Date(_.lastNotice) : undefined
        _.tokenCreation = new Date(_.tokenCreation)
        _.company.creationDate = new Date(_.company.creationDate)
        return _
      }),
    )
  }

  readonly confirmCompaniesPosted = (ids: Id[]) => {
    return this.client.post<void>(`/companies/companies-posted`, {body: {companyIds: ids}})
  }

  private static readonly mapCompany = (company: {[key in keyof CompanyWithReportsCount]: any}): CompanyWithReportsCount => ({
    ...company,
    creationDate: new Date(company.creationDate),
  })
}
