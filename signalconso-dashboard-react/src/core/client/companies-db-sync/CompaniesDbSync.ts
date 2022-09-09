import {Id} from '../../model'

export interface CompaniesDbSyncInfo {
  id: Id
  fileName: string
  fileUrl: string
  linesCount: number
  linesDone: number
  startedAt: Date
  endedAt?: Date
  errors?: string
}

export interface CompaniesDbSyncInfos {
  etablissementImportInfo?: CompaniesDbSyncInfo
  uniteLegaleInfo?: CompaniesDbSyncInfo
}
