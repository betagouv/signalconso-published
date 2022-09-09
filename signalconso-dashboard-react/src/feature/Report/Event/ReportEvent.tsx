import React from 'react'
import {useI18n} from '../../../core/i18n'
import {ReportEventIcon} from './ReportEventIcon'
import {Box, Icon} from '@mui/material'
import {combineSx, styleUtils, sxUtils} from '../../../core/theme'
import {makeSx} from '../../../alexlibs/mui-extension'
import {ReportEvent} from '../../../core/client/event/Event'

export interface ReportComponentEventProps {
  event: ReportEvent
}

const css = makeSx({
  root: {
    display: 'flex',
    mb: 1.5,
    '&:last-of-type': {
      mb: 0,
      '& $body': {
        border: 'none',
        pb: 0,
      },
    },
  },
  body: {
    flex: 1,
    pt: 0,
    pr: 2,
    pb: 1.5,
    pl: 0,
    fontSize: t => styleUtils(t).fontSize.small,
    borderBottom: t => '1px solid ' + t.palette.divider,
  },
  icon: {
    mt: 0,
    mr: 2,
    mb: 2,
    ml: 0,
  },
  head: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  title: {
    fontWeight: t => t.typography.fontWeightBold,
    fontSize: t => styleUtils(t).fontSize.normal,
  },
  date: {
    whiteSpace: 'nowrap',
    color: t => t.palette.text.disabled,
    mt: 0.5,
  },
})

export const ReportEventComponent = ({event}: ReportComponentEventProps) => {
  const {m, formatDate, formatDateTime} = useI18n()

  return (
    <Box sx={css.root} key={event.data.id}>
      <ReportEventIcon sx={css.icon} action={event.data.action} />
      <Box sx={css.body}>
        <Box sx={css.head}>
          <Box sx={css.title}>{event.data.action}</Box>
          <Box sx={css.date}>{formatDateTime(event.data.creationDate)}</Box>
        </Box>

        {event.user && (
          <div>
            <Icon sx={combineSx(sxUtils.inlineIcon, {color: t => t.palette.text.secondary})}>person</Icon>
            &nbsp;
            {event.user.firstName} {event.user.lastName} {event.user.role}
          </div>
        )}
        <Box sx={{color: t => t.palette.text.secondary}}>{(event.data.details as any)?.description}</Box>
      </Box>
    </Box>
  )
}
