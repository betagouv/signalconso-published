import * as _Sentry from '@sentry/react'
import {config} from '../../conf/config'
import {Integrations} from '@sentry/tracing'

export class Sentry {
  static readonly init = () => {
    if (config.sentry_dns) {
      _Sentry.init({
        dsn: config.sentry_dns,
        integrations: [new Integrations.BrowserTracing()],
        tracesSampleRate: config.sentry_traceRate,
      })
    }
  }
}
