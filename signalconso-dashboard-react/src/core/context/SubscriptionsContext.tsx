import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {CrudListCRUD, useCrudList} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {SignalConsoSecuredSdk} from '../client/SignalConsoSecuredSdk'
import {ApiError} from '../client/ApiClient'
import {Subscription} from '../client/subscription/Subscription'

export interface SubscriptionsContextProps
  extends CrudListCRUD<
    Subscription,
    'id',
    {
      c: SignalConsoSecuredSdk['subscription']['create']
      r: SignalConsoSecuredSdk['subscription']['list']
      u: SignalConsoSecuredSdk['subscription']['update']
      d: SignalConsoSecuredSdk['subscription']['remove']
    },
    ApiError
  > {}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<SubscriptionsContextProps> = {}

const SubscriptionsContext = React.createContext<SubscriptionsContextProps>(defaultContext as SubscriptionsContextProps)

export const SubscriptionsProvider = ({api, children}: Props) => {
  const crud = useCrudList('id', {
    c: api.secured.subscription.create,
    r: api.secured.subscription.list,
    u: api.secured.subscription.update,
    d: api.secured.subscription.remove,
  })

  return <SubscriptionsContext.Provider value={crud}>{children}</SubscriptionsContext.Provider>
}

export const useSubscriptionsContext = (): SubscriptionsContextProps => {
  return useContext<SubscriptionsContextProps>(SubscriptionsContext)
}
