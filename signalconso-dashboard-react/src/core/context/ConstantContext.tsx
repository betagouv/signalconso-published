import * as React from 'react'
import {ReactNode, useContext, useMemo} from 'react'
import {UseFetcher, useFetcher} from '../../alexlibs/react-hooks-lib'
import {SignalConsoApiSdk} from '../ApiSdkInstance'
import {ApiError} from '../client/ApiClient'

export interface ConstantContextProps {
  regions: UseFetcher<SignalConsoApiSdk['public']['constant']['getRegions'], ApiError>
  countries: UseFetcher<SignalConsoApiSdk['public']['constant']['getCountries'], ApiError>
  categories: UseFetcher<SignalConsoApiSdk['public']['constant']['getCategories'], ApiError>
  departmentsIndex?: {[key: string]: string}
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<ConstantContextProps> = {}

const ConstantContext = React.createContext<ConstantContextProps>(defaultContext as ConstantContextProps)

export const ConstantProvider = ({api, children}: Props) => {
  const _regions = useFetcher(api.public.constant.getRegions)
  const _countries = useFetcher(api.public.constant.getCountries)
  const _categories = useFetcher(api.public.constant.getCategories)
  const departmentsIndex = useMemo(
    () => _regions.entity?.flatMap(_ => _.departments).reduce((acc, dep) => ({...acc, [dep.code]: dep.label}), {}),
    [_regions.entity],
  )
  return (
    <ConstantContext.Provider
      value={{
        regions: _regions,
        countries: _countries,
        categories: _categories,
        departmentsIndex,
      }}
    >
      {children}
    </ConstantContext.Provider>
  )
}

export const useConstantContext = (): ConstantContextProps => {
  return useContext<ConstantContextProps>(ConstantContext)
}
