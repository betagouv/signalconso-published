import {Box} from '@mui/material'
import React, {ReactNode} from 'react'

export interface DialogInputRowProps {
  icon?: string
  label: string | ReactNode
  children: ReactNode
}

export interface DialogInputRowExtraProps {
  children: ReactNode
}

export const DialogInputRow = ({icon, label, children}: DialogInputRowProps) => {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-start',
      }}
    >
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
        }}
      >
        {/*{icon && (*/}
        {/*  <Icon fontSize="small" sx={{*/}
        {/*    color: t => t.palette.text.disabled,*/}
        {/*    mr: 1*/}
        {/*  }}>{icon}</Icon>*/}
        {/*)}*/}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            minHeight: 50,
            color: t => t.palette.text.secondary,
            minWidth: 136,
            maxWidth: 136,
            flexWrap: 'wrap',
          }}
        >
          {label}
        </Box>
      </Box>
      <Box
        sx={{
          maxWidth: 500,
          width: '100%',
          minHeight: 50,
          flex: 1,
          overflow: 'hidden',
        }}
      >
        {children}
      </Box>
    </Box>
  )
}

export const DialogInputRowExtra = ({children}: DialogInputRowExtraProps) => {
  return (
    <Box
      sx={{
        pb: 2,
        mb: 2,
        borderBottom: t => `1px solid ${t.palette.divider}`,
      }}
    >
      {children}
    </Box>
  )
}
