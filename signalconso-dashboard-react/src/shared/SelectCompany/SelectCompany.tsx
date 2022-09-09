import {ScInput} from '../Input/ScInput'
import {IconBtn} from '../../alexlibs/mui-extension'
import {Divider, Icon} from '@mui/material'
import {SelectCompanyList} from './SelectCompanyList'
import React, {useEffect, useState} from 'react'
import {useI18n} from '../../core/i18n'
import {useCompaniesContext} from '../../core/context/CompaniesContext'

import {useEffectFn} from '../../alexlibs/react-hooks-lib'
import {CompanySearchResult} from '../../core/client/company/Company'
import {Id} from '../../core/model'
import {ScOption} from 'core/helper/ScOption'

export interface SelectCompanyProps {
  siret?: Id
  onChange: (_: CompanySearchResult) => void
}

export const SelectCompany = ({siret, onChange}: SelectCompanyProps) => {
  const {m} = useI18n()
  const _company = useCompaniesContext().searchByIdentity
  const [inputValue, setInputValue] = useState<Id | undefined>(siret)

  const search = () => {
    if (inputValue) _company.fetch({}, inputValue)
  }

  useEffect(() => {
    _company.clearCache()
    ScOption.from(siret)
      .filter(x => x === inputValue)
      .map(setInputValue)
  }, [siret])

  return (
    <>
      <ScInput
        sx={{
          mb: 1.5,
          minWidth: 280,
        }}
        error={!!_company.error}
        helperText={_company.error ? _company.error.message : undefined}
        fullWidth
        value={inputValue ?? ''}
        placeholder={m.companySearchLabel}
        onChange={e => setInputValue(e.target.value)}
        InputProps={{
          endAdornment: (
            <>
              <IconBtn
                onClick={() => {
                  setInputValue(undefined)
                  _company.clearCache()
                }}
              >
                <Icon>clear</Icon>
              </IconBtn>
              <Divider sx={{my: 0.25}} orientation="vertical" variant="middle" flexItem />
              <IconBtn loading={_company.loading} color="primary" sx={{mr: -1.5}} onClick={search}>
                <Icon>search</Icon>
              </IconBtn>
            </>
          ),
        }}
      />
      {_company.entity && <SelectCompanyList companies={_company.entity} onChange={onChange} />}
    </>
  )
}
