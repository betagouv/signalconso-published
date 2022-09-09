import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UseAsync, UseFetcher, useFetcher} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {useAsync} from '../../alexlibs/react-hooks-lib'
import {uniqBy} from '../lodashNamedExport'
import {SignalConsoSecuredSdk} from '../client/SignalConsoSecuredSdk'
import {BlockedReportNotification, Id} from '../model'

export interface BlockedReportNotificationContextProps {
  list: UseFetcher<SignalConsoSecuredSdk['reportBlockedNotification']['fetch']>
  create: UseAsync<SignalConsoSecuredSdk['reportBlockedNotification']['create']>
  remove: UseAsync<SignalConsoSecuredSdk['reportBlockedNotification']['delete']>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<BlockedReportNotificationContextProps> = {}

const BlockedReportNotificationContext = React.createContext<BlockedReportNotificationContextProps>(
  defaultContext as BlockedReportNotificationContextProps,
)

export const BlockedReportNotificationProvider = ({api, children}: Props) => {
  const fetch = useFetcher(api.secured.reportBlockedNotification.fetch)
  const create = useAsync((companyIds: Id[]) => {
    const newBlocked: BlockedReportNotification[] = companyIds.map(companyId => ({companyId, dateCreation: new Date()}))
    fetch.setEntity(prev => uniqBy([...(prev ?? []), ...newBlocked], _ => _.companyId))
    return api.secured.reportBlockedNotification.create(companyIds)
  })
  const remove = useAsync((companyIds: Id[]) => {
    fetch.setEntity(currentCompanyIds => currentCompanyIds?.filter(_ => !companyIds.includes(_.companyId)))
    return api.secured.reportBlockedNotification.delete(companyIds)
  })

  return (
    <BlockedReportNotificationContext.Provider
      value={{
        list: fetch,
        create,
        remove,
      }}
    >
      {children}
    </BlockedReportNotificationContext.Provider>
  )
}

export const useBlockedReportNotificationContext = (): BlockedReportNotificationContextProps => {
  return useContext<BlockedReportNotificationContextProps>(BlockedReportNotificationContext)
}
