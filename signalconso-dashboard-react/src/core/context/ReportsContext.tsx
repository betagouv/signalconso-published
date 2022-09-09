import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UsePaginate} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {useScPaginate} from '../../shared/usePaginate/usePaginate'
import {PaginatedFilters, ReportSearch, ReportSearchResult} from '../model'

export interface ReportsContextProps extends UsePaginate<ReportSearchResult, ReportSearch & PaginatedFilters> {
  extract: (_?: ReportSearch) => Promise<void>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<ReportsContextProps> = {}

const ReportsContext = React.createContext<ReportsContextProps>(defaultContext as ReportsContextProps)

export const ReportsProvider = ({api, children}: Props) => {
  const _paginate = useScPaginate<ReportSearchResult, ReportSearch & PaginatedFilters>(api.secured.reports.search, {
    limit: 10,
    offset: 0,
  })

  return (
    <ReportsContext.Provider
      value={{
        ..._paginate,
        extract: () => api.secured.reports.extract(_paginate.filters),
      }}
    >
      {children}
    </ReportsContext.Provider>
  )
}

export const useReportsContext = (): ReportsContextProps => {
  return useContext<ReportsContextProps>(ReportsContext)
}
