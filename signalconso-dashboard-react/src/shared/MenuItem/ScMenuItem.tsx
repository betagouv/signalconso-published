import {Checkbox, MenuItem, MenuItemProps} from '@mui/material'
import React from 'react'

export interface ScMenuItemProps<T> extends Omit<MenuItemProps, 'value'> {
  value?: T
  withCheckbox?: boolean
  checked?: boolean
}

export const ScMenuItem = <T,>({checked, withCheckbox, value, children, ...props}: ScMenuItemProps<T>) => {
  return (
    <MenuItem {...props} value={'' + value}>
      <Checkbox size="small" style={{paddingLeft: 0, paddingTop: 0, paddingBottom: 0}} checked={checked} />
      {children}
    </MenuItem>
  )
}
