import {useI18n} from '../../core/i18n'
import {Box, Icon} from '@mui/material'
import {lighten} from '@mui/system/colorManipulator'
import {IconBtn} from '../../alexlibs/mui-extension'
import React, {ReactNode} from 'react'

interface DatatableToolbarProps {
  onClear?: () => void
  children?: ReactNode
  actions?: ReactNode
  open?: boolean
}

export const DatatableToolbar = ({open, onClear, children, actions}: DatatableToolbarProps) => {
  const {m} = useI18n()
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        pr: 1,
        pl: 1,
        zIndex: 2,
        overflow: 'hidden',
        transition: t => t.transitions.create('all'),
        position: 'absolute',
        bottom: 0,
        right: 0,
        left: 0,
        opacity: open ? 1 : 0,
        height: open ? 'calc(100% + 2px)' : 0,
        background: t => lighten(t.palette.primary.main, 0.86),
        borderTopRightRadius: t => t.shape.borderRadius + 'px',
        borderTopLeftRadius: t => t.shape.borderRadius + 'px',
        margin: `-1px`,
        border: t => `2px solid ${t.palette.primary.main}`,
      }}
    >
      {onClear && (
        <IconBtn sx={{mr: 1}} color="primary" onClick={onClear}>
          <Icon>clear</Icon>
        </IconBtn>
      )}
      {children}
      {actions && <Box sx={{marginLeft: 'auto'}}>{actions}</Box>}
    </Box>
  )
}
