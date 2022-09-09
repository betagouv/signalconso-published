import {CompanyAccessToken} from './CompanyAccessToken'
import {CompanyAccessLevel} from '../company-access/CompanyAccess'
import {ApiClientApi} from '../ApiClient'
import {Id} from '../../model'

export class CompanyAccessTokenClient {
  constructor(private client: ApiClientApi) {}

  readonly fetch = (siret: string) => {
    return this.client.get<CompanyAccessToken[]>(`/accesses/${siret}/pending`)
  }

  readonly remove = (siret: string, tokenId: Id) => {
    return this.client.delete<void>(`/accesses/${siret}/token/${tokenId}`)
  }

  readonly create = (siret: string, email: string, level: CompanyAccessLevel): Promise<CompanyAccessToken> => {
    return (
      this.client
        .post<void>(`/accesses/${siret}/invitation`, {body: {email, level}})
        // Hack because the API don't return anything and create a CompanyAccessToken or a CompanyAccess
        .then(() => {
          const response: CompanyAccessToken = {
            id: '' + Math.floor(Math.random() * 10000),
            level,
            emailedTo: email,
            expirationDate: new Date(),
          }
          return response
        })
    )
  }
}
