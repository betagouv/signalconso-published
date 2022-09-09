import {
  CompanyAccessClient,
  CompanyClient,
  ConstantClient,
  EventClient,
  ReportedPhoneClient,
  ReportsClient,
  SubscriptionClient,
  UserClient,
  WebsiteClient,
} from '.'
import {ApiClientApi} from './ApiClient'
import {AsyncFileClient} from './async-file/AsyncFileClient'
import {CompanyAccessTokenClient} from './company-access-token/CompanyAccessTokenClient'
import {SecuredFileClient} from './file/SecuredFileClient'
import {ReportBlockedNotificationClient} from './blocked-report-notifications/ReportBlockedNotificationClient'
import {AccessesClient} from './accesses/AccessesClient'
import {CompaniesDbSyncClient} from './companies-db-sync/CompaniesDbSyncClient'
import {StatsClient} from './stats/StatsClient'
import {AdminClient} from './admin/AdminClient'
import {ConsumerEmailValidationClient} from './consumer-email-validation/ConsumerEmailValidationClient'

export class SignalConsoSecuredSdk {
  constructor(private client: ApiClientApi) {}

  readonly admin = new AdminClient(this.client)
  readonly accesses = new AccessesClient(this.client)
  readonly website = new WebsiteClient(this.client)
  readonly reportedPhone = new ReportedPhoneClient(this.client)
  readonly constant = new ConstantClient(this.client)
  readonly subscription = new SubscriptionClient(this.client)
  readonly company = new CompanyClient(this.client)
  readonly consumerEmailValidation = new ConsumerEmailValidationClient(this.client)
  readonly stats = new StatsClient(this.client)
  readonly companyAccess = new CompanyAccessClient(this.client)
  readonly companyAccessToken = new CompanyAccessTokenClient(this.client)
  readonly reports = new ReportsClient(this.client)
  readonly events = new EventClient(this.client)
  readonly asyncFiles = new AsyncFileClient(this.client)
  readonly user = new UserClient(this.client)
  readonly document = new SecuredFileClient(this.client)
  readonly reportBlockedNotification = new ReportBlockedNotificationClient(this.client)
  readonly companiesDbSync = new CompaniesDbSyncClient(this.client)
}
