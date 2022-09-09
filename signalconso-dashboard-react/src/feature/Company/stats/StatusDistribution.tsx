import * as React from 'react'
import {useI18n} from '../../../core/i18n'
import {Enum} from '../../../alexlibs/ts-utils'
import {Icon, Tooltip} from '@mui/material'
import {Panel, PanelBody, PanelHead} from '../../../shared/Panel'
import {HorizontalBarChart} from '../../../shared/HorizontalBarChart/HorizontalBarChart'
import {useMemoFn} from '../../../alexlibs/react-hooks-lib'
import {ReportStatus, ReportStatusPro} from '../../../core/client/report/Report'

interface Props<T extends ReportStatus | ReportStatusPro> {
  values: {[key in T]: number} | undefined
  loading?: boolean
  statusDesc: (status: T) => string
  statusShortLabel: (status: T) => string
  statusColor: (status: T) => string
}

export const StatusDistribution = <T extends ReportStatus | ReportStatusPro>({
  values,
  loading,
  statusDesc,
  statusShortLabel,
  statusColor,
}: Props<T>) => {
  const {m} = useI18n()

  const statusDistribution = useMemoFn(values, _ =>
    Enum.entries(_).map(([status, count]) => ({
      label: (
        <span>
          {statusShortLabel(status)}
          <Tooltip title={statusDesc(status)}>
            <Icon
              fontSize="small"
              sx={{
                verticalAlign: 'middle',
                color: t => t.palette.text.disabled,
                ml: 1,
              }}
            >
              help
            </Icon>
          </Tooltip>
        </span>
      ),
      value: count,
      color: statusColor(status) ?? undefined,
    })),
  )

  return (
    <Panel loading={loading}>
      <PanelHead>{m.status}</PanelHead>
      <PanelBody>
        <HorizontalBarChart data={statusDistribution} grid />
      </PanelBody>
    </Panel>
  )
}
