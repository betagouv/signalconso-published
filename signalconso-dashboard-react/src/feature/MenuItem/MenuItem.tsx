import {Checkbox, MenuItem, MenuItemProps} from '@mui/material'

export interface ScMenuItemProps<T> extends Omit<MenuItemProps, 'value'> {
  withCheckbox?: boolean
  checked?: boolean
  value: T
}

export const ScMenuItem = <T,>({withCheckbox, checked, children, value, ...other}: ScMenuItemProps<T>) => {
  return (
    <MenuItem {...other}>
      {withCheckbox && <Checkbox size="small" checked={checked} />}
      {children}
    </MenuItem>
  )
}
