import {Txt} from '../../alexlibs/mui-extension'
import * as React from 'react'
import {Box, BoxProps, Tooltip} from '@mui/material'
import {useMemoFn} from '../../alexlibs/react-hooks-lib'
import {DetailInputValue} from '../../core/client/report/Report'

interface Props extends BoxProps {
  input: DetailInputValue[]
  lines?: number
  hideTooltip?: boolean
}

export const ReportDetailValues = ({input, lines = 2, hideTooltip, sx, ...props}: Props) => {
  const description = useMemoFn(input, _ => _.find(_ => _.label === 'Description :')?.value)

  return (
    <Tooltip
      hidden={hideTooltip}
      title={input.map((detail, i) => (
        <div key={i}>
          <Box
            component="span"
            dangerouslySetInnerHTML={{__html: detail.label}}
            sx={{fontWeight: t => t.typography.fontWeightBold}}
          />
          &nbsp;
          <Box component="span" dangerouslySetInnerHTML={{__html: detail.value}} sx={{color: t => t.palette.text.secondary}} />
        </div>
      ))}
    >
      <Box
        {...props}
        sx={{
          display: '-webkit-box',
          WebkitLineClamp: lines,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
          ...sx,
        }}
      >
        {description ||
          input.map((_, i) => (
            <span key={i}>
              <Txt bold>{_.label}</Txt> <span dangerouslySetInnerHTML={{__html: _.value}} />
              <br />
            </span>
          ))}
      </Box>
    </Tooltip>
  )
}
