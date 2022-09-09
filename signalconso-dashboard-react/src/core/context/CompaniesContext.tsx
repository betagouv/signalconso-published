import * as React from 'react'
import {ReactNode, useContext} from 'react'
import {UseFetcher, useFetcher, usePaginate, UsePaginate} from '../../alexlibs/react-hooks-lib'

import {SignalConsoApiSdk} from '../ApiSdkInstance'

import {useScPaginate} from '../../shared/usePaginate/usePaginate'
import {CompanySearch, CompanyToActivate, CompanyUpdate, CompanyWithReportsCount} from '../client/company/Company'
import {Address, Id, PaginatedFilters} from '../model'
import {ApiError} from '../client/ApiClient'
import {paginateData} from '../helper'

type Sdk = SignalConsoApiSdk['secured']['company']

export interface CompaniesContextProps {
  activated: UsePaginate<CompanyWithReportsCount, CompanySearch>
  toActivate: UsePaginate<CompanyToActivate, PaginatedFilters>
  create: UseFetcher<Sdk['create'], ApiError>
  updateAddress: UseFetcher<Sdk['updateAddress'], ApiError>
  downloadActivationDocument: UseFetcher<Sdk['downloadActivationDocument'], ApiError>
  confirmCompaniesPosted: UseFetcher<Sdk['confirmCompaniesPosted'], ApiError>
  searchByIdentity: UseFetcher<SignalConsoApiSdk['public']['company']['searchCompaniesByIdentity'], ApiError>
  accessibleByPro: UseFetcher<Sdk['getAccessibleByPro'], ApiError>
  saveUndeliveredDocument: UseFetcher<Sdk['saveUndeliveredDocument'], ApiError>
  byId: UseFetcher<(id: Id) => Promise<CompanyWithReportsCount>, ApiError>
  hosts: UseFetcher<Sdk['getHosts'], ApiError>
  responseRate: UseFetcher<Sdk['getResponseRate'], ApiError>
}

interface Props {
  children: ReactNode
  api: SignalConsoApiSdk
}

const defaultContext: Partial<CompaniesContextProps> = {}

const CompaniesContext = React.createContext<CompaniesContextProps>(defaultContext as CompaniesContextProps)

export const CompaniesProvider = ({api, children}: Props) => {
  const activated = useScPaginate<CompanyWithReportsCount, CompanySearch>(api.secured.company.search, {
    limit: 10,
    offset: 0,
  })
  const toActivate = usePaginate<CompanyToActivate, PaginatedFilters>(
    (filter: PaginatedFilters) =>
      api.secured.company
        .fetchToActivate()
        .then(_ => _.sort((a, b) => b.tokenCreation.getTime() - a.tokenCreation.getTime()))
        .then(paginateData(filter.limit, filter.offset)),
    {limit: 250, offset: 0},
  )
  const create = useFetcher(api.secured.company.create)
  const updateAddress = useFetcher(api.secured.company.updateAddress)
  const searchByIdentity = useFetcher(api.public.company.searchCompaniesByIdentity)
  const downloadActivationDocument = useFetcher(api.secured.company.downloadActivationDocument)
  const confirmCompaniesPosted = useFetcher(api.secured.company.confirmCompaniesPosted)
  const saveUndeliveredDocument = useFetcher(api.secured.company.saveUndeliveredDocument)
  const accessibleByPro = useFetcher(api.secured.company.getAccessibleByPro)
  const byId = useFetcher((id: Id) => api.secured.company.byId(id).then(_ => _.entities[0]))
  const hosts = useFetcher(api.secured.company.getHosts)
  const responseRate = useFetcher(api.secured.company.getResponseRate)

  const updateRegisteredCompanyAddress = (id: Id, address: Address) => {
    activated.setEntity(companies => {
      if (!companies) return companies
      const company = companies?.entities.find(company => company.id === id)
      if (company) {
        company.address = address
        return {...companies}
      }
      return companies
    })
  }

  return (
    <CompaniesContext.Provider
      value={{
        updateAddress: {
          ...updateAddress,
          fetch: (p: {force?: boolean; clean?: boolean} = {}, id: Id, update: CompanyUpdate) => {
            return updateAddress.fetch(p, id, update).then(_ => {
              updateRegisteredCompanyAddress(id, update.address)
              return _
            })
          },
        },
        create,
        activated,
        toActivate,
        searchByIdentity,
        downloadActivationDocument,
        confirmCompaniesPosted,
        accessibleByPro,
        saveUndeliveredDocument,
        byId,
        hosts,
        responseRate,
      }}
    >
      {children}
    </CompaniesContext.Provider>
  )
}

export const useCompaniesContext = (): CompaniesContextProps => {
  return useContext<CompaniesContextProps>(CompaniesContext)
}
