import {ApiClientApi} from '../ApiClient'
import {CompaniesDbSyncInfo, CompaniesDbSyncInfos} from './CompaniesDbSync'
import {Shape} from '../../helper'

export class CompaniesDbSyncClient {
  constructor(private client: ApiClientApi) {}

  readonly startEtablissementFile = () => this.client.post<void>(`enterprises-sync/start-etablissement`)

  readonly startUniteLegaleFile = () => this.client.post<void>(`enterprises-sync/start-unitelegale`)

  readonly cancelAllFiles = () => this.client.post<void>(`enterprises-sync/cancel`)

  readonly cancelEtablissementFile = () => this.client.post<void>(`enterprises-sync/cancel-etablissement`)

  readonly cancelUniteLegaleFile = () => this.client.post<void>(`enterprises-sync/cancel-unitelegale`)

  readonly getInfo = (): Promise<CompaniesDbSyncInfos> => {
    return this.client.get<Shape<CompaniesDbSyncInfos>>(`enterprises-sync/info`).then(_ => ({
      etablissementImportInfo: CompaniesDbSyncClient.mapCompaniesDbSyncInfo(_.etablissementImportInfo),
      uniteLegaleInfo: CompaniesDbSyncClient.mapCompaniesDbSyncInfo(_.uniteLegaleInfo),
    }))
  }

  private static readonly mapCompaniesDbSyncInfo = (_?: Shape<CompaniesDbSyncInfo>): CompaniesDbSyncInfo | undefined =>
    _
      ? {
          ..._,
          startedAt: new Date(_.startedAt),
          ...(_.endedAt ? {endedAt: new Date(_.endedAt)} : {}),
        }
      : undefined
}
