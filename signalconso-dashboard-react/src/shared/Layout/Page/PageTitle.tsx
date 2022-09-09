import * as React from 'react'
import {ReactNode} from 'react'
import {Typography, TypographyProps} from '@mui/material'

interface Props extends TypographyProps {
  action?: ReactNode
}

export const PageTitle = ({action, children, sx, ...props}: Props) => {
  return (
    <Typography
      variant="h5"
      sx={{
        mt: 1,
        mb: 3,
        display: 'flex',
        alignItems: 'center',
        ...sx,
      }}
      {...props}
    >
      {children}
      {action && <div style={{marginLeft: 'auto'}}>{action}</div>}
    </Typography>
  )
}
