import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UseFetcher, useFetcher} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {ApiError} from '../client/ApiClient'

export interface EventContextProps {
  reportEvents: UseFetcher<SignalConsoApiSdk['secured']['events']['getByReportId'], ApiError>
  companyEvents: UseFetcher<SignalConsoApiSdk['secured']['events']['getBySiret'], ApiError>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<EventContextProps> = {}

const EventContext = React.createContext<EventContextProps>(defaultContext as EventContextProps)

export const EventProvider = ({api, children}: Props) => {
  const reportEvents = useFetcher(api.secured.events.getByReportId)
  const companyEvents = useFetcher(api.secured.events.getBySiret)

  return (
    <EventContext.Provider
      value={{
        reportEvents,
        companyEvents,
      }}
    >
      {children}
    </EventContext.Provider>
  )
}

export const useEventContext = (): EventContextProps => {
  return useContext<EventContextProps>(EventContext)
}
