import * as React from 'react'
import {CardContent as MuiCardContent, CardContentProps} from '@mui/material'

interface Props extends CardContentProps {}

export const PanelBody = ({sx, children, ...other}: Props) => {
  return (
    <MuiCardContent
      {...other}
      sx={{
        borderRadius: '2px',
        p: 2,
        m: 0,
        '&:last-child': {
          pb: 2,
        },
        ...sx,
      }}
    >
      {children}
    </MuiCardContent>
  )
}
