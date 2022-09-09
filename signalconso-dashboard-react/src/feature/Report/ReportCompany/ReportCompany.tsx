import {Panel, PanelBody, PanelHead} from '../../../shared/Panel'
import {AddressComponent} from '../../../shared/Address/Address'

import {Txt} from '../../../alexlibs/mui-extension'
import {Box, Icon, IconButton, useTheme} from '@mui/material'
import {SelectCompanyDialog} from '../../../shared/SelectCompany/SelectCompanyDialog'
import {ScButton} from '../../../shared/Button/Button'
import React from 'react'
import {useReportContext} from '../../../core/context/ReportContext'
import {useI18n} from '../../../core/i18n'
import {siteMap} from '../../../core/siteMap'
import {NavLink} from 'react-router-dom'
import {styleUtils, sxUtils} from '../../../core/theme'
import {Report} from '../../../core/client/report/Report'
import {ScOption} from 'core/helper/ScOption'

interface Props {
  report: Report
  canEdit?: boolean
}

export const ReportCompany = ({report, canEdit}: Props) => {
  const _report = useReportContext()
  const {m} = useI18n()
  const theme = useTheme()
  return (
    <Panel stretch>
      <PanelHead
        action={
          canEdit && (
            <SelectCompanyDialog
              siret={report.companySiret}
              onChange={company => {
                _report.updateCompany.fetch({}, report.id, company)
              }}
            >
              <ScButton icon="edit" color="primary" loading={_report.updateCompany.loading}>
                {m.edit}
              </ScButton>
            </SelectCompanyDialog>
          )
        }
      >
        {report.companyId ? (
          <NavLink to={siteMap.logged.company(report.companyId)}>
            {m.company}
            <IconButton size="small" sx={{ml: 1}}>
              <Icon>open_in_new</Icon>
            </IconButton>
          </NavLink>
        ) : (
          <> {m.company} </>
        )}
      </PanelHead>
      <PanelBody
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <Box sx={sxUtils.fontBig} style={{marginBottom: theme.spacing(1 / 2)}}>
            {report.companySiret}
          </Box>
          <Box
            sx={{
              color: t => t.palette.text.secondary,
              fontSize: t => styleUtils(t).fontSize.small,
            }}
          >
            <Box sx={{fontWeight: t => t.typography.fontWeightBold}}>{report.companyName}</Box>
            <AddressComponent address={report.companyAddress} />
          </Box>
          <div>{report.vendor}</div>
          {ScOption.from(report.websiteURL)
            .map(_ => (
              <Txt link block sx={{mt: 1}}>
                <a href={_} target="_blank">
                  {_}
                </a>
              </Txt>
            ))
            .toUndefined()}
        </div>
        <Icon
          sx={{
            fontSize: 64,
            color: t => t.palette.divider,
          }}
        >
          store
        </Icon>
      </PanelBody>
    </Panel>
  )
}
