import {ScRadioGroupItem} from '../RadioGroup/RadioGroupItem'
import {Txt} from '../../alexlibs/mui-extension'
import {Icon} from '@mui/material'
import {ScRadioGroup} from '../RadioGroup/RadioGroup'
import React from 'react'
import {useI18n} from '../../core/i18n'
import {AddressComponent} from '../Address/Address'
import {sxUtils} from '../../core/theme'
import {Report} from '../../core/client/report/Report'
import {CompanySearchResult} from '../../core/client/company/Company'

interface Props {
  companies: CompanySearchResult[]
  onChange: (_: CompanySearchResult) => void
}

export const SelectCompanyList = ({companies, onChange}: Props) => {
  const {m} = useI18n()

  return (
    <ScRadioGroup>
      {companies.map(company => {
        const isGovernment = Report.isGovernmentCompany(company)
        return (
          <ScRadioGroupItem key={company.siret} value={company.siret!} sx={{maxWidth: 400}} onClick={() => onChange(company)}>
            <Txt truncate block bold>
              {company.name}
            </Txt>
            {company.brand && <Txt block>{company.brand}</Txt>}
            {company.isHeadOffice && (
              <Txt color="primary">
                <Icon sx={sxUtils.inlineIcon}>business</Icon>
                &nbsp;
                {m.isHeadOffice}
              </Txt>
            )}
            {!company.isHeadOffice && company.activityLabel && (
              <Txt color="hint">
                <Icon sx={sxUtils.inlineIcon}>label</Icon>
                &nbsp;
                {company.activityLabel}
              </Txt>
            )}
            {isGovernment && (
              <Txt color="error" bold>
                <Icon sx={sxUtils.inlineIcon}>error</Icon>
                {m.governmentCompany}
              </Txt>
            )}
            {company.address && (
              <Txt color="hint" block size="small" sx={{mt: 1 / 2}}>
                <AddressComponent address={company.address} />
              </Txt>
            )}
          </ScRadioGroupItem>
        )
      })}
    </ScRadioGroup>
  )
}
