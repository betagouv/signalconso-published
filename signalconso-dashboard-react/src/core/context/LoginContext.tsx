import React, {Dispatch, ReactNode, SetStateAction, useContext, useEffect, useState} from 'react'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {Roles, UserWithPermission} from '../client/authenticate/Authenticate'

const LoginContext = React.createContext({} as any)

interface LoginProviderProps {
  apiSdk: SignalConsoApiSdk
  token: string
  connectedUser: UserWithPermission
  onLogout: () => void
  children: ReactNode
}

interface UseLoginProps {
  connectedUser: UserWithPermission & {isDGCCRF: boolean; isPro: boolean; isNotPro: boolean; isAdmin: boolean}
  setConnectedUser: Dispatch<SetStateAction<UserWithPermission>>
  logout: () => void
  apiSdk: SignalConsoApiSdk
  token: string
}

export const LoginProvider = ({apiSdk, token, connectedUser: _connectedUser, onLogout, children}: LoginProviderProps) => {
  const [connectedUser, setConnectedUser] = useState<UserWithPermission>(_connectedUser)

  useEffect(() => {
    setConnectedUser(_connectedUser)
  }, [_connectedUser])

  return (
    <LoginContext.Provider
      value={{
        setConnectedUser,
        connectedUser: {
          ...connectedUser,
          isDGCCRF: connectedUser.role === Roles.DGCCRF,
          isPro: connectedUser.role === Roles.Pro,
          isNotPro: connectedUser.role !== Roles.Pro,
          isAdmin: connectedUser.role === Roles.Admin,
        },
        logout: onLogout,
        token,
        apiSdk,
      }}
    >
      {children}
    </LoginContext.Provider>
  )
}

export const useLogin = (): UseLoginProps => {
  return useContext(LoginContext)
}
