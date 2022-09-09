import {Box, Icon, Tooltip, useTheme} from '@mui/material'
import {Txt} from 'alexlibs/mui-extension'
import {ScOption} from 'core/helper/ScOption'
import {ReportReferenceNumber} from 'feature/Report/ReportReferenceNumber'

import {Report} from '../../../core/client/report/Report'
import {useReportContext} from '../../../core/context/ReportContext'
import {capitalize} from '../../../core/helper'
import {useI18n} from '../../../core/i18n'
import {styleUtils, sxUtils} from '../../../core/theme'
import {ScButton} from '../../../shared/Button/Button'
import {Panel, PanelBody, PanelHead} from '../../../shared/Panel'
import {EditConsumerDialog} from './EditConsumerDialog'

interface Props {
  report: Report
  canEdit?: boolean
}

export const ReportConsumer = ({report, canEdit}: Props) => {
  const _report = useReportContext()
  const {m} = useI18n()
  const theme = useTheme()

  return (
    <Panel stretch>
      <PanelHead
        action={
          canEdit && (
            <EditConsumerDialog report={report} onChange={consumer => _report.updateConsumer.fetch({}, report.id, consumer)}>
              <ScButton icon="edit" color="primary" loading={_report.updateConsumer.loading}>
                {m.edit}
              </ScButton>
            </EditConsumerDialog>
          )
        }
      >
        {m.consumer}
      </PanelHead>
      <PanelBody
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <Box sx={{fontSize: t => styleUtils(t).fontSize.big}}>
            {ScOption.from(report.firstName)
              .map(_ => capitalize(_))
              .getOrElse('')}
            &nbsp;
            {ScOption.from(report.lastName)
              .map(_ => _.toLocaleUpperCase())
              .getOrElse('')}
          </Box>
          <Box sx={{color: t => t.palette.text.secondary}}>{report.email}</Box>
          {report.consumerPhone && <Box sx={{color: t => t.palette.text.secondary}}>{report.consumerPhone}</Box>}
          <ReportReferenceNumber consumerReferenceNumber={report.consumerReferenceNumber} />
          {!report.contactAgreement && (
            <Box sx={{color: t => t.palette.error.main}} style={{marginTop: theme.spacing(0.5)}}>
              <Icon sx={sxUtils.inlineIcon}>warning</Icon>
              &nbsp;
              {m.reportConsumerWantToBeAnonymous}
            </Box>
          )}
        </div>
        <Icon
          sx={{
            fontSize: 64,
            color: t => t.palette.divider,
          }}
        >
          person
        </Icon>
      </PanelBody>
    </Panel>
  )
}
