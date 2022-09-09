import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UseFetcher, useFetcher, UsePaginate} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {useScPaginate} from '../../shared/usePaginate/usePaginate'
import {ApiError} from '../client/ApiClient'
import {User, UserSearch} from '../client/user/User'

export interface UsersContextProps {
  searchDgccrf: UsePaginate<User, UserSearch>
  dgccrfPending: UseFetcher<SignalConsoApiSdk['secured']['user']['fetchPendingDGCCRF'], ApiError>
  invite: UseFetcher<SignalConsoApiSdk['secured']['user']['inviteDGCCRF'], ApiError>
  changePassword: UseFetcher<SignalConsoApiSdk['secured']['user']['changePassword'], ApiError>
  activate: UseFetcher<SignalConsoApiSdk['public']['user']['activateAccount'], ApiError>
  fetchTokenInfo: UseFetcher<SignalConsoApiSdk['public']['user']['fetchTokenInfo'], ApiError>
  getConnectedUser: UseFetcher<SignalConsoApiSdk['secured']['user']['fetchConnectedUser']>
  forceValidateEmail: UseFetcher<SignalConsoApiSdk['secured']['user']['forceValidateEmail']>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<UsersContextProps> = {}

const UsersContext = React.createContext<UsersContextProps>(defaultContext as UsersContextProps)

export const UsersProvider = ({api, children}: Props) => {
  const searchDgccrf = useScPaginate<User, UserSearch, ApiError>(api.secured.user.fetchDGCCRF, {
    limit: 10,
    offset: 0,
  })
  const changePassword = useFetcher(api.secured.user.changePassword)
  const forceValidateEmail = useFetcher(api.secured.user.forceValidateEmail)
  const activate = useFetcher(api.public.user.activateAccount)
  const dgccrfPending = useFetcher(api.secured.user.fetchPendingDGCCRF)
  const invite = useFetcher(api.secured.user.inviteDGCCRF)
  const fetchTokenInfo = useFetcher(api.public.user.fetchTokenInfo)
  const getConnectedUser = useFetcher(api.secured.user.fetchConnectedUser)
  return (
    <UsersContext.Provider
      value={{
        searchDgccrf,
        dgccrfPending,
        invite,
        changePassword,
        activate,
        fetchTokenInfo,
        getConnectedUser,
        forceValidateEmail,
      }}
    >
      {children}
    </UsersContext.Provider>
  )
}

export const useUsersContext = (): UsersContextProps => {
  return useContext<UsersContextProps>(UsersContext)
}
