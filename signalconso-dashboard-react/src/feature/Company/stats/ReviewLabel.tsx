import {Box, BoxProps, Icon, Tooltip} from '@mui/material'
import {Emoticon} from '../../../shared/Emoticon/Emoticon'
import * as React from 'react'

interface Props extends BoxProps {
  tooltip: string
  children: React.ReactNode
}

export const ReviewLabel = ({tooltip, children, ...props}: Props) => {
  return (
    <Box sx={{display: 'flex', alignItems: 'center'}}>
      <Emoticon sx={{fontSize: 30}} {...props}>
        {children}
      </Emoticon>
      <Tooltip title={tooltip}>
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
    </Box>
  )
}
