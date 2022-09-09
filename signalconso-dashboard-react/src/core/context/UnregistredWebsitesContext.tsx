import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UseFetcher, useFetcher, UsePaginate} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {useScPaginate} from '../../shared/usePaginate/usePaginate'
import {ApiHostWithReportCount, HostReportCountSearch} from '../client/website/Website'

export interface UnregistredWebsiteWithCompanyContextProps extends UsePaginate<ApiHostWithReportCount, HostReportCountSearch> {
  extractUnregistered: UseFetcher<() => Promise<void>>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<UnregistredWebsiteWithCompanyContextProps> = {}

const UnregistredWebsitesContext = React.createContext<UnregistredWebsiteWithCompanyContextProps>(
  defaultContext as UnregistredWebsiteWithCompanyContextProps,
)

export const UnregistredWebsitesProvider = ({api, children}: Props) => {
  const listUnregistred = useScPaginate<ApiHostWithReportCount, HostReportCountSearch>(api.secured.website.listUnregistered, {
    limit: 10,
    offset: 0,
    q: '',
  })

  const extractUnregistered = useFetcher(() => api.secured.website.extractUnregistered(listUnregistred.filters))

  return (
    <UnregistredWebsitesContext.Provider
      value={{
        ...listUnregistred,
        extractUnregistered,
      }}
    >
      {children}
    </UnregistredWebsitesContext.Provider>
  )
}

export const useUnregistredWebsiteWithCompanyContext = (): UnregistredWebsiteWithCompanyContextProps => {
  return useContext<UnregistredWebsiteWithCompanyContextProps>(UnregistredWebsitesContext)
}
