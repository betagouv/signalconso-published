import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UseAsync} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {useAsync} from '../../alexlibs/react-hooks-lib'
import {SignalConsoSecuredSdk} from '../client/SignalConsoSecuredSdk'

export interface AccessesContextProps {
  acceptToken: UseAsync<SignalConsoSecuredSdk['accesses']['acceptToken']>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<AccessesContextProps> = {}

const AccessesContext = React.createContext<AccessesContextProps>(defaultContext as AccessesContextProps)

export const AccessesProvider = ({api, children}: Props) => {
  const acceptToken = useAsync(api.secured.accesses.acceptToken)

  return (
    <AccessesContext.Provider
      value={{
        acceptToken,
      }}
    >
      {children}
    </AccessesContext.Provider>
  )
}

export const useAccessesContext = (): AccessesContextProps => {
  return useContext<AccessesContextProps>(AccessesContext)
}
