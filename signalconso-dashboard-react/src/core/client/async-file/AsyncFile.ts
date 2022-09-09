import {Id} from '../../model'

export interface AsyncFile {
  id: Id
  creationDate: Date
  filename: string
  url: string
  kind: AsyncFileKind
  status: AsyncFileStatus
}

export enum AsyncFileStatus {
  Loading = 'Loading',
  Failed = 'Failed',
  Successed = 'Successed',
}

export enum AsyncFileKind {
  ReportedPhones = 'ReportedPhones',
  Reports = 'Reports',
  ReportedWebsites = 'ReportedWebsites',
}
