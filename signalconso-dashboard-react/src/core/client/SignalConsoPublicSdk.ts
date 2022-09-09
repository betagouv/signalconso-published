import {ApiClientApi} from './ApiClient'
import {AuthenticateClient, FileClient, PublicConstantClient} from '.'
import {PublicCompanyClient} from './company/PublicCompanyClient'
import {PublicUserClient} from './user/PublicUserClient'
import {PublicReportClient} from './report/PublicReportClient'

export class SignalConsoPublicSdk {
  constructor(private client: ApiClientApi) {}

  readonly company = new PublicCompanyClient(this.client)
  readonly report = new PublicReportClient(this.client)
  readonly user = new PublicUserClient(this.client)
  readonly constant = new PublicConstantClient(this.client)
  readonly authenticate = new AuthenticateClient(this.client)
  readonly document = new FileClient(this.client)
}
