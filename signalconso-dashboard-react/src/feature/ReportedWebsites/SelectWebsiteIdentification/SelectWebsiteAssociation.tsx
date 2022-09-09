import React, {useState} from 'react'
import {useI18n} from '../../../core/i18n'
import {ScDialog} from '../../../shared/Confirm/ScDialog'
import {SelectCountry} from '../../../shared/SelectCountry/SelectCountry'
import {useReportedWebsiteWithCompanyContext} from '../../../core/context/ReportedWebsitesContext'
import {useToast} from '../../../core/toast'
import {ChipCountry} from './ChipCountry'
import {ChipCompany} from './ChipCompany'
import {BoxProps} from '@mui/material'
import {ChipNoAssociation} from './ChipNoAssociation'
import {ScRadioGroup} from '../../../shared/RadioGroup/RadioGroup'
import {ScRadioGroupItem} from '../../../shared/RadioGroup/RadioGroupItem'
import {Enum} from '../../../alexlibs/ts-utils'
import {Txt} from '../../../alexlibs/mui-extension'
import {fnSwitch} from '../../../alexlibs/ts-utils'
import {ScButton} from '../../../shared/Button/Button'
import {useEffectFn} from '../../../alexlibs/react-hooks-lib'
import {SelectCompany} from '../../../shared/SelectCompany/SelectCompany'
import {Company} from '../../../core/client/company/Company'
import {WebsiteUpdateCompany} from '../../../core/client/website/Website'
import {Country} from '../../../core/client/constant/Country'
import {Id} from '../../../core/model'

interface Website {
  id: Id
  company?: Company
  companyCountry?: Country
}

interface Props extends Omit<BoxProps, 'onChange'> {
  website: Website
  onChange: () => void
}

export enum AssociationType {
  COMPANY = 'COMPANY',
  COUNTRY = 'COUNTRY',
}

export const SelectWebsiteAssociation = ({onChange, website, ...props}: Props) => {
  const {m} = useI18n()
  const {toastError, toastInfo, toastSuccess} = useToast()
  const [selectedAssociation, setSelectedAssociation] = useState<AssociationType>(
    website.companyCountry ? AssociationType.COUNTRY : AssociationType.COMPANY,
  )
  const _updateCompany = useReportedWebsiteWithCompanyContext().updateCompany
  const _updateCountry = useReportedWebsiteWithCompanyContext().updateCountry
  const [company, setCompany] = useState<WebsiteUpdateCompany | undefined>(website.company)
  const [country, setCountry] = useState<Country | undefined>(website.companyCountry)

  useEffectFn(_updateCompany.error, toastError)
  useEffectFn(_updateCountry.error, toastError)

  const updateCompany = async (close: () => void) => {
    if (company) {
      if (website.company && website.company.siret === company.siret) {
        toastInfo(m.alreadySelectedCompany(company.name))
      } else {
        _updateCompany
          .fetch({}, website.id, {
            siret: company.siret,
            name: company.name,
            address: company.address,
            activityCode: company.activityCode,
            isHeadOffice: company.isHeadOffice,
            isOpen: company.isOpen,
          })
          .then(_ => onChange())
          .then(_ => toastSuccess(m.websiteEdited))
          .then(_ => close())
      }
    }
  }

  const updateCountry = async (close: () => void) => {
    if (country) {
      if (country === website.companyCountry) {
        toastInfo(m.alreadySelectedCountry(country?.name))
      } else {
        await _updateCountry.fetch({}, website.id, country)
        toastSuccess(m.websiteEdited)
        close()
        onChange()
      }
    }
  }

  return (
    <ScDialog
      PaperProps={{style: {overflow: 'visible'}}}
      maxWidth="sm"
      title={m.companyWebsiteAssociation}
      content={_ => (
        <>
          <Txt block sx={{mb: 1}}>
            {m.attachTo}
          </Txt>
          <ScRadioGroup sx={{mb: 2}} dense inline value={selectedAssociation} onChange={setSelectedAssociation}>
            {Enum.keys(AssociationType).map(_ => (
              <ScRadioGroupItem key={_} value={_} title={m.attachToType[_]} />
            ))}
          </ScRadioGroup>

          {selectedAssociation &&
            fnSwitch(selectedAssociation, {
              [AssociationType.COMPANY]: () => (
                <SelectCompany
                  siret={company?.siret}
                  onChange={companyChanged => {
                    setCompany(companyChanged)
                    setCountry(undefined)
                  }}
                />
              ),
              [AssociationType.COUNTRY]: () => (
                <SelectCountry
                  country={website.companyCountry}
                  onChange={companyCountry => {
                    setCompany(undefined)
                    setCountry(companyCountry)
                  }}
                />
              ),
            })}
        </>
      )}
      overrideActions={close => (
        <>
          <ScButton onClick={close}>{m.close}</ScButton>
          {selectedAssociation &&
            fnSwitch(selectedAssociation, {
              [AssociationType.COMPANY]: () => (
                <ScButton loading={_updateCompany.loading} disabled={!company} onClick={() => updateCompany(close)}>
                  {m.confirm}
                </ScButton>
              ),
              [AssociationType.COUNTRY]: () => (
                <ScButton loading={_updateCountry.loading} disabled={!country} onClick={() => updateCountry(close)}>
                  {m.confirm}
                </ScButton>
              ),
            })}
        </>
      )}
    >
      {website.companyCountry ? (
        <ChipCountry onClick={props.onClick} country={website.companyCountry} />
      ) : website.company ? (
        <ChipCompany onClick={props.onClick} company={website.company} />
      ) : (
        <ChipNoAssociation />
      )}
    </ScDialog>
  )
}
