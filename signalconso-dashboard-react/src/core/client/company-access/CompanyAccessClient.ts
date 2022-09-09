import {CompanyAccess, CompanyAccessLevel} from './CompanyAccess'
import {ApiClientApi} from '../ApiClient'
import {Id} from '../../model'

export class CompanyAccessClient {
  constructor(private client: ApiClientApi) {}

  readonly fetch = (siret: string) => {
    return this.client.get<CompanyAccess[]>(`/accesses/${siret}`)
  }

  readonly count = (siret: string) => {
    return this.client.get<number>(`/accesses/${siret}/count`)
  }

  readonly update = (siret: string, userId: string, level: CompanyAccessLevel) => {
    return this.client.put<CompanyAccess>(`/accesses/${siret}/${userId}`, {body: {level}}).then(_ => ({..._, level: level}))
  }

  readonly remove = (siret: string, userId: Id) => {
    return this.client.delete<void>(`/accesses/${siret}/${userId}`)
  }
}
