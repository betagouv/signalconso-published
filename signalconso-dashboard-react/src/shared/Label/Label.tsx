import * as React from 'react'
import {CSSProperties, ReactNode} from 'react'
import {Paper, PaperProps, Theme, useTheme} from '@mui/material'
import {alpha} from '@mui/material/styles'
import {styleUtils} from '../../core/theme'

export type LabelColor = 'error' | 'warning' | 'info' | 'success' | 'disable'

export interface LabelProps extends PaperProps {
  type?: LabelColor
  className?: string
  children: ReactNode
  fullWidth?: boolean
  dense?: boolean
  inSelectOptions?: boolean
  elevation?: number
}

const colorize = (color: string): CSSProperties => ({
  background: alpha(color, 0.14),
  color: color,
})

const colors = (t: Theme) => ({
  error: {
    ...colorize(styleUtils(t).color.error),
  },
  warning: {
    ...colorize(styleUtils(t).color.warning),
  },
  success: {
    ...colorize(styleUtils(t).color.success),
  },
  info: {
    ...colorize(styleUtils(t).color.info),
  },
  disable: {
    ...colorize(t.palette.text.disabled),
  },
})

export const Label = ({type, children, sx, fullWidth, dense, elevation = 0, inSelectOptions, ...props}: LabelProps) => {
  const t = useTheme()
  return (
    <Paper
      elevation={elevation}
      sx={{
        whiteSpace: 'nowrap',
        borderRadius: 40,
        py: 1 / 1.5,
        px: 2,
        fontWeight: 'bold',
        letterSpacing: '1px',
        display: 'inline-flex',
        minHeight: 24,
        alignItems: 'center',
        transition: t => t.transitions.create('all'),
        ...colorize(t.palette.text.disabled),
        ...(type && colors(t)[type]),
        // ...elevation > 0 && {
        //   border: 'none'
        // },
        ...(fullWidth && {
          width: '100%',
        }),
        ...(dense && {
          fontWeight: '500' as any,
          fontSize: t => styleUtils(t).fontSize.small,
          py: 0,
          px: 1,
        }),
        ...(inSelectOptions && {
          marginTop: -10,
          marginBottom: -10,
        }),
        ...sx,
      }}
      {...props}
    >
      {children}
    </Paper>
  )
}
