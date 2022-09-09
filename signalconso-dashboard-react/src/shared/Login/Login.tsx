import React, {useEffect, useMemo, useState} from 'react'
import jwtDecode from 'jwt-decode'
import {localStorageObject} from '../../core/helper/localStorage'

type PromiseReturn<T> = T extends PromiseLike<infer U> ? U : T

type AsynFnResult<T extends (...args: any[]) => Promise<object>> = PromiseReturn<ReturnType<T>>

export type Fn = (...args: any[]) => Promise<any>

export interface LoginActionProps<F extends Function> {
  action: F
  loading?: boolean
  errorMsg?: string
}

export interface LoginExposedProps<L extends Fn, R extends Fn> {
  authResponse?: AsynFnResult<L>
  logout: () => void
  login: LoginActionProps<L>
  register: LoginActionProps<R>
  token?: string
  setToken: (_: AsynFnResult<L>) => void
  isCheckingToken: boolean
}

interface Props<L extends Fn, R extends Fn> {
  onLogin: L
  onRegister: R
  onLogout: () => void
  getTokenFromResponse: (_: AsynFnResult<L>) => string
  children: ({authResponse, login, logout, token}: LoginExposedProps<L, R>) => any
}

export const Login = <L extends Fn, R extends Fn>({
  onLogin,
  onRegister,
  onLogout,
  getTokenFromResponse,
  children,
}: Props<L, R>) => {
  const authenticationStorage = useMemo(() => localStorageObject<AsynFnResult<L>>('AuthUserSignalConso'), [])
  const [auth, setAuth] = useState<AsynFnResult<L>>()
  const [isLogging, setIsLogging] = useState(false)
  const [isRegistering, setIsRegistering] = useState(false)
  const [loginError, setLoginError] = useState<string | undefined>()
  const [registerError, setRegisterError] = useState<string | undefined>()
  const [isCheckingToken, setIsCheckingToken] = useState(true)

  useEffect(() => {
    const storedToken = authenticationStorage.get()
    if (storedToken) {
      checkToken(storedToken)
    } else {
      setIsCheckingToken(false)
    }
  }, [])

  // @ts-ignore
  const login: L = async (...args: any[]) => {
    try {
      setIsLogging(true)
      const auth = await onLogin(...args)
      setToken(auth)
      return auth
    } catch (e: any) {
      setLoginError(e)
      throw e
    } finally {
      setIsLogging(false)
    }
  }

  const setToken = (auth: AsynFnResult<L>) => {
    authenticationStorage.set(auth)
    setAuth(auth)
  }

  // @ts-ignore
  const register: R = async (...args: any[]) => {
    try {
      setIsRegistering(true)
      return onRegister(...args)
    } catch (e: any) {
      setRegisterError(e)
      throw e
    } finally {
      setIsRegistering(false)
    }
  }

  const logout = () => {
    setAuth(undefined)
    authenticationStorage.clear()
    onLogout()
  }

  const isTokenExpired = (token: string): boolean => {
    const expirationDate = (jwtDecode(token) as {exp: number}).exp
    return new Date().getTime() > expirationDate * 1000
  }

  const checkToken = async (auth: AsynFnResult<L>) => {
    setIsCheckingToken(true)
    if (isTokenExpired(getTokenFromResponse(auth))) {
    } else {
      setAuth(auth)
    }
    setIsCheckingToken(false)
  }

  return children({
    authResponse: auth,
    login: {
      action: login,
      loading: isLogging,
      errorMsg: loginError,
    },
    logout,
    register: {
      action: register,
      loading: isRegistering,
      errorMsg: registerError,
    },
    token: auth ? getTokenFromResponse(auth) : undefined,
    isCheckingToken,
    setToken,
  })
}
