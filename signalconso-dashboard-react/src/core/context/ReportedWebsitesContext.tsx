import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UseFetcher, useFetcher, UsePaginate} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {useScPaginate} from '../../shared/usePaginate/usePaginate'
import {
  DepartmentDivision,
  IdentificationStatus,
  InvestigationStatus,
  Practice,
  WebsiteWithCompany,
  WebsiteWithCompanySearch,
} from '../client/website/Website'
import {ApiError} from '../client/ApiClient'

export interface ReportedWebsiteWithCompanyContextProps {
  getWebsiteWithCompany: UsePaginate<WebsiteWithCompany, WebsiteWithCompanySearch>
  remove: UseFetcher<SignalConsoApiSdk['secured']['website']['remove'], ApiError>
  update: UseFetcher<SignalConsoApiSdk['secured']['website']['updateStatus'], ApiError>
  updateCompany: UseFetcher<SignalConsoApiSdk['secured']['website']['updateCompany'], ApiError>
  updateCountry: UseFetcher<SignalConsoApiSdk['secured']['website']['updateCountry'], ApiError>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<ReportedWebsiteWithCompanyContextProps> = {}

const ReportedWebsitesContext = React.createContext<ReportedWebsiteWithCompanyContextProps>(
  defaultContext as ReportedWebsiteWithCompanyContextProps,
)

export const ReportedWebsitesProvider = ({api, children}: Props) => {
  const listReportedWebsiteWithCompany = useScPaginate<WebsiteWithCompany, WebsiteWithCompanySearch>(api.secured.website.list, {
    limit: 10,
    offset: 0,
    identificationStatus: [IdentificationStatus.NotIdentified],
  })

  const remove = useFetcher(api.secured.website.remove)
  const update = useFetcher(api.secured.website.updateStatus)
  const updateCompany = useFetcher(api.secured.website.updateCompany)
  const updateCountry = useFetcher(api.secured.website.updateCountry)

  return (
    <ReportedWebsitesContext.Provider
      value={{
        getWebsiteWithCompany: listReportedWebsiteWithCompany,
        remove,
        update,
        updateCompany,
        updateCountry,
      }}
    >
      {children}
    </ReportedWebsitesContext.Provider>
  )
}

export const useReportedWebsiteWithCompanyContext = (): ReportedWebsiteWithCompanyContextProps => {
  return useContext<ReportedWebsiteWithCompanyContextProps>(ReportedWebsitesContext)
}
