import {useCrudList} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../../core/ApiSdkInstance'
import {CompanyAccess, CompanyAccessLevel} from '../../core/client/company-access/CompanyAccess'
import {CompanyAccessToken} from '../../core/client/company-access-token/CompanyAccessToken'
import {ApiError} from '../../core/client/ApiClient'
import {Id} from '../../core/model'

export const useCompanyAccess = (api: SignalConsoApiSdk, siret: string) => {
  const crudAccessR = () => api.secured.companyAccess.fetch(siret)
  const crudAccessU = (userId: string, level: CompanyAccessLevel) => api.secured.companyAccess.update(siret, userId, level)
  const crudAccessD = (userId: string) => api.secured.companyAccess.remove(siret, userId)
  const crudAccess = useCrudList<
    CompanyAccess,
    'userId',
    {
      r: typeof crudAccessR
      u: typeof crudAccessU
      d: typeof crudAccessD
    },
    ApiError
  >('userId', {
    r: crudAccessR,
    u: crudAccessU,
    d: crudAccessD,
  })

  const crudTokenC = (email: string, level: CompanyAccessLevel) => api.secured.companyAccessToken.create(siret, email, level)
  const crudTokenR = () => api.secured.companyAccessToken.fetch(siret)
  const crudTokenD = (id: Id) => api.secured.companyAccessToken.remove(siret, id)

  const crudToken = useCrudList<
    CompanyAccessToken,
    'id',
    {
      c: typeof crudTokenC
      r: typeof crudTokenR
      d: typeof crudTokenD
    }
  >('id', {
    c: crudTokenC,
    r: crudTokenR,
    d: crudTokenD,
  })

  return {crudAccess, crudToken}
}
