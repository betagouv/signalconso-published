import * as React from 'react'
import {forwardRef} from 'react'
import {Card, CardProps, LinearProgress} from '@mui/material'

export interface PanelProps extends CardProps {
  loading?: boolean
  hoverable?: boolean
  stretch?: boolean
  elevation?: number
}

export const Panel = forwardRef(({elevation = 0, hoverable, loading, children, stretch, sx, ...other}: PanelProps, ref: any) => {
  return (
    <Card
      ref={ref}
      elevation={elevation}
      sx={{
        borderRadius: t => t.shape.borderRadius + 'px',
        mb: 2,
        ...(hoverable && {
          cursor: 'pointer',
          transition: t => t.transitions.create('all'),
          '&:hover': {
            transform: 'scale(1.01)',
            boxShadow: t => t.shadows[4],
          },
        }),
        ...(stretch && {
          display: 'flex',
          flexDirection: 'column',
          height: t => `calc(100% - ${t.spacing(2)})`,
        }),
        ...(elevation === 0 && {
          border: t => `1px solid ${t.palette.divider}`,
        }),
        ...sx,
      }}
      {...other}
    >
      {loading && <LinearProgress sx={{mb: '-4px'}} />}
      {children}
    </Card>
  )
})
