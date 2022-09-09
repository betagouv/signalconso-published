import {ApiClientApi} from '../ApiClient'

export class AccessesClient {
  constructor(private client: ApiClientApi) {}

  readonly acceptToken = (siret: string, token: string) => {
    return this.client.post<void>(`/accesses/${siret}/token/accept`, {body: {token}})
  }
}
