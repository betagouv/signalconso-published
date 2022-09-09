import {Icon} from '@mui/material'
import * as React from 'react'

export interface DotProps {
  className?: string
}

export const Dot = ({className}: DotProps) => {
  return (
    <Icon
      className={className}
      sx={{
        fontSize: '9px !important',
        verticalAlign: 'middle',
        mx: 1,
      }}
    >
      fiber_manual_record
    </Icon>
  )
}
