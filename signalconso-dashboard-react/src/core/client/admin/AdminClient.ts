import {ApiClientApi} from '../ApiClient'

export class AdminClient {
  constructor(private client: ApiClientApi) {}

  readonly getEmailCodes = () => {
    return this.client.get<string[]>(`/admin/test-email`)
  }

  readonly sendTestEmail = (templateRef: string, to: string) => {
    return this.client.post<void>(`/admin/test-email`, {qs: {templateRef, to}})
  }
}
