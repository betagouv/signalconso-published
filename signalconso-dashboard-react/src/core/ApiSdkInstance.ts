import {config} from '../conf/config'
import {SignalConsoSecuredSdk} from './client/SignalConsoSecuredSdk'
import {ApiClient} from './client/ApiClient'
import {SignalConsoPublicSdk} from './client/SignalConsoPublicSdk'

export type SignalConsoApiSdk = ReturnType<typeof makeSecuredSdk>

const headers = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
}

const baseUrl = config.apiBaseUrl + '/api'

export const apiPublicSdk = new SignalConsoPublicSdk(
  new ApiClient({
    baseUrl,
    headers,
  }),
)

export const makeSecuredSdk = (token: string) => ({
  public: apiPublicSdk,
  publicConnected: new SignalConsoPublicSdk(
    new ApiClient({
      baseUrl,
      headers: {...headers, 'X-Auth-Token': token},
    }),
  ),
  secured: new SignalConsoSecuredSdk(
    new ApiClient({
      baseUrl,
      headers: {...headers, 'X-Auth-Token': token},
    }),
  ),
})
