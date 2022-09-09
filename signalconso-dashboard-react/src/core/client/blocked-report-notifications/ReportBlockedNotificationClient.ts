import {BlockedReportNotification} from './BlockedReportNotification'
import {ApiClientApi} from '../ApiClient'
import {Id} from '../../model'

export class ReportBlockedNotificationClient {
  constructor(private client: ApiClientApi) {}

  readonly fetch = () => {
    return this.client
      .get<BlockedReportNotification[]>(`/report-blocked-notification`)
      .then(result => result.map(_ => ({..._, dateCreation: new Date(_.dateCreation)})))
  }

  readonly create = (companyIds: Id[]) => {
    return this.client.post<BlockedReportNotification[]>(`/report-blocked-notification`, {body: {companyIds}})
  }

  readonly delete = (companyIds: Id[]) => {
    return this.client.post<void>(`/report-blocked-notification/delete`, {body: {companyIds}})
  }
}
