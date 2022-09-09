import {useI18n} from '../../core/i18n'
import {Panel} from '../../shared/Panel'
import {Datatable} from '../../shared/Datatable/Datatable'
import React, {useEffect, useMemo, useState} from 'react'
import {useCompaniesContext} from '../../core/context/CompaniesContext'
import {Badge, Box, Icon, InputBase, ListItemIcon, ListItemText, MenuItem, Tooltip} from '@mui/material'
import {NavLink} from 'react-router-dom'
import {siteMap} from '../../core/siteMap'
import {ScButton} from '../../shared/Button/Button'
import {styleUtils, sxUtils} from '../../core/theme'
import {Fender, IconBtn} from '../../alexlibs/mui-extension'
import {mapArrayFromQuerystring, useQueryString} from '../../core/helper/useQueryString'
import {DebouncedInput} from '../../shared/DebouncedInput/DebouncedInput'
import {useToast} from '../../core/toast'
import {AddressComponent} from '../../shared/Address/Address'
import {SelectCompanyDialog} from '../../shared/SelectCompany/SelectCompanyDialog'
import {EditAddressDialog} from './EditAddressDialog'
import {useLogin} from '../../core/context/LoginContext'
import {ClipboardApi} from '../../alexlibs/ts-utils/browser/clipboard/ClipboardApi'
import {CompaniesRegisteredFilters} from './CompaniesRegisteredFilters'
import {ScMenu} from '../../shared/Menu/Menu'
import {Txt} from '../../alexlibs/mui-extension'
import {Company, CompanySearch} from '../../core/client/company/Company'
import {cleanObject} from '../../core/helper'
import {PaginatedSearch} from '../../core/model'
import {ScOption} from 'core/helper/ScOption'

export interface CompanySearchQs extends PaginatedSearch<any> {
  departments?: string[] | string
  activityCodes?: string[] | string
  identity?: string
}

export const CompaniesRegistered = () => {
  const {m, formatLargeNumber} = useI18n()
  const _companies = useCompaniesContext().activated
  const _companyUpdateAddress = useCompaniesContext().updateAddress
  const _companyCreate = useCompaniesContext().create
  const {toastError, toastErrorIfDefined, toastSuccess} = useToast()
  const {connectedUser} = useLogin()
  const [sortByResponseRate, setSortByResponseRate] = useState<'asc' | 'desc' | undefined>()

  const queryString = useQueryString<Partial<CompanySearch>, Partial<CompanySearchQs>>({
    toQueryString: _ => _,
    fromQueryString: mapArrayFromQuerystring(['activityCodes', 'departments']),
  })

  useEffect(() => {
    _companies.updateFilters({..._companies.initialFilters, ...queryString.get()})
  }, [])

  useEffect(() => {
    queryString.update(cleanObject(_companies.filters))
  }, [_companies.filters])

  useEffect(() => {
    toastErrorIfDefined(_companies.error)
    toastErrorIfDefined(_companyCreate.error)
  }, [_companies.error, _companyCreate.error])

  const copyAddress = (c: Company) => {
    const a = c.address
    const address = `${c.name} - ${a.number} ${a.street} ${a.addressSupplement} ${a.postalCode} ${a.city} (${c.siret})`
    ClipboardApi.copy(address.replaceAll('undefined', '').replaceAll(/[\s]{1,}/g, ' '))
    toastSuccess(m.addressCopied)
  }

  const data = useMemo(() => {
    if (sortByResponseRate && _companies.list)
      return [..._companies.list.entities].sort(
        (a, b) => (a.responseRate - b.responseRate) * (sortByResponseRate === 'desc' ? -1 : 1),
      )
    return _companies.list?.entities
  }, [_companies.list?.entities, sortByResponseRate])

  return (
    <Panel>
      <Datatable
        id="companiesregistered"
        header={
          <DebouncedInput
            value={_companies.filters.identity ?? ''}
            onChange={value => _companies.updateFilters(prev => ({...prev, identity: value}))}
          >
            {(value, onChange) => (
              <InputBase
                value={value}
                placeholder={m.companiesSearchPlaceholder}
                fullWidth
                onChange={e => onChange(e.target.value)}
              />
            )}
          </DebouncedInput>
        }
        actions={
          <>
            <CompaniesRegisteredFilters
              filters={_companies.filters}
              updateFilters={_ => {
                _companies.updateFilters(prev => ({...prev, ..._}))
              }}
            >
              <Tooltip title={m.advancedFilters}>
                <IconBtn color="primary">
                  <Icon>filter_list</Icon>
                </IconBtn>
              </Tooltip>
            </CompaniesRegisteredFilters>
            <Tooltip title={m.removeAllFilters}>
              <IconBtn color="primary" onClick={_companies.clearFilters}>
                <Icon>clear</Icon>
              </IconBtn>
            </Tooltip>
          </>
        }
        sort={{
          sortableColumns: ['responseRate'],
          sortBy: sortByResponseRate ? 'responseRate' : undefined,
          orderBy: sortByResponseRate,
          onSortChange: _ => setSortByResponseRate(_.sortBy === 'responseRate' ? _.orderBy : undefined),
        }}
        loading={_companies.fetching}
        data={data}
        paginate={{
          offset: _companies.filters.offset,
          limit: _companies.filters.limit,
          onPaginationChange: pagination => _companies.updateFilters(prev => ({...prev, ...pagination})),
        }}
        total={_companies.list?.totalCount}
        getRenderRowKey={_ => _.id}
        showColumnsToggle={true}
        columns={[
          {
            head: m.name,
            id: 'siret',
            sx: _ => ({
              lineHeight: 1.4,
              maxWidth: 170,
            }),
            render: _ => (
              <Tooltip title={_.name}>
                <span>
                  <NavLink to={siteMap.logged.company(_.id)}>
                    <Txt link sx={{fontWeight: 'bold', marginBottom: '-1px'}}>
                      {_.name}
                    </Txt>
                  </NavLink>
                  <br />
                  <Box
                    component="span"
                    sx={{
                      fontSize: t => styleUtils(t).fontSize.small,
                      color: t => t.palette.text.disabled,
                    }}
                  >
                    {_.siret}
                  </Box>
                </span>
              </Tooltip>
            ),
          },
          {
            head: m.address,
            id: 'address',
            sx: _ => ({maxWidth: 260, ...sxUtils.truncate}),
            render: _ => (
              <Tooltip title={<AddressComponent address={_.address} />}>
                <span>
                  <AddressComponent address={_.address} />
                </span>
              </Tooltip>
            ),
          },
          {
            head: m.postalCodeShort,
            id: 'postalCode',
            render: _ => (
              <>
                <span>{_.address.postalCode?.slice(0, 2)}</span>
                <Box component="span" sx={{color: t => t.palette.text.disabled}}>
                  {_.address.postalCode?.slice(2, 5)}
                </Box>
              </>
            ),
          },
          {
            head: m.reports,
            id: 'count',
            sx: _ => ({textAlign: 'right'}),
            render: _ => (
              <NavLink to={siteMap.logged.reports({siretSirenList: [_.siret], departments: _companies.filters.departments})}>
                <ScButton color="primary">{formatLargeNumber(_.count)}</ScButton>
              </NavLink>
            ),
          },
          {
            head: m.responseRate,
            id: 'responseRate',
            sx: _ => ({textAlign: 'right'}),
            render: _ => (
              <Box
                component="span"
                sx={{
                  fontWeight: t => t.typography.fontWeightBold,
                  ...(_.responseRate > 50
                    ? {
                        color: t => t.palette.success.light,
                      }
                    : _.responseRate === 0
                    ? {
                        color: t => t.palette.error.light,
                      }
                    : {
                        color: t => t.palette.warning.light,
                      }),
                }}
              >
                {_.responseRate} %
              </Box>
            ),
          },
          {
            head: m.activityCode,
            id: 'activityCode',
            sx: _ => ({textAlign: 'right'}),
            render: _ => <span>{_?.activityCode}</span>,
          },
          {
            head: '',
            id: 'actions',
            stickyEnd: true,
            sx: _ => sxUtils.tdActions,
            render: _ => (
              <>
                <Badge color="error" badgeContent=" " variant="dot" overlap="circular">
                  <NavLink to={siteMap.logged.company(_.id)}>
                    <IconBtn color="primary">
                      <Icon>query_stats</Icon>
                    </IconBtn>
                  </NavLink>
                </Badge>
                <ScMenu>
                  <NavLink to={siteMap.logged.companyAccesses(_.siret)}>
                    <MenuItem>
                      <ListItemIcon>
                        <Icon>vpn_key</Icon>
                      </ListItemIcon>
                      <ListItemText>{m.handleAccesses}</ListItemText>
                    </MenuItem>
                  </NavLink>
                  <MenuItem onClick={() => copyAddress(_)}>
                    <ListItemIcon>
                      <Icon>content_copy</Icon>
                    </ListItemIcon>
                    <ListItemText>{m.copyAddress}</ListItemText>
                  </MenuItem>
                  {connectedUser.isAdmin && (
                    <EditAddressDialog
                      address={_.address}
                      onChangeError={_companyUpdateAddress.error?.message}
                      onChange={form => {
                        const {activationDocumentRequired = false, ...address} = form
                        return _companyUpdateAddress
                          .fetch({}, _.id, {address, activationDocumentRequired})
                          .then(() => toastSuccess(m.editedAddress))
                      }}
                    >
                      <MenuItem>
                        <ListItemIcon>
                          <Icon>edit</Icon>
                        </ListItemIcon>
                        <ListItemText>{m.editAddress}</ListItemText>
                      </MenuItem>
                    </EditAddressDialog>
                  )}
                </ScMenu>
              </>
            ),
          },
        ]}
        renderEmptyState={
          <Fender title={m.noCompanyFound} icon="store" sx={{margin: 'auto', mt: 1, mb: 2}}>
            {connectedUser.isAdmin && (
              <SelectCompanyDialog
                onChange={company => {
                  const {siret, name, address, activityCode, isOpen, isHeadOffice} = company
                  if (name && address && siret) {
                    _companyCreate
                      .fetch({}, {siret, name, address, activityCode, isOpen, isHeadOffice})
                      .then(() => toastSuccess(m.companyCreated))
                  } else {
                    toastError({message: m.cannotCreateCompanyMissingInfo})
                  }
                }}
              >
                <ScButton variant="contained" color="primary" icon="add" sx={{mt: 1}}>
                  {m.registerACompany}
                </ScButton>
              </SelectCompanyDialog>
            )}
          </Fender>
        }
      />
    </Panel>
  )
}
