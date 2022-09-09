import {Badge, Checkbox, Icon, IconButtonProps, Menu, MenuItem, Tooltip} from '@mui/material'
import React from 'react'
import {IconBtn} from '../../alexlibs/mui-extension'
import {DatatableColumnProps} from './Datatable'

interface Props extends Omit<IconButtonProps, 'onChange'> {
  // Hack because there is no way to make TS understand that the key of an object can
  // only be a string ({[key: string]: string} does not work...)
  columns: (Omit<DatatableColumnProps<any>, 'id'> & {id: string})[]
  hiddenColumns: string[]
  onChange: (_: string[]) => void
  title?: string
}

export const DatatableColumnToggle = ({className, title, columns, hiddenColumns, onChange, ...props}: Props) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget)
  }

  return (
    <>
      <Tooltip title={title ?? ''}>
        <IconBtn {...props} color="primary" onClick={handleClick}>
          <Badge
            color="error"
            badgeContent={columns.length === hiddenColumns.length ? '!' : columns.length - hiddenColumns.length}
            invisible={hiddenColumns.length === 0}
          >
            <Icon color={columns.length === hiddenColumns.length ? 'error' : undefined}>table_chart</Icon>
          </Badge>
        </IconBtn>
      </Tooltip>
      <Menu anchorEl={anchorEl} keepMounted open={!!anchorEl} onClose={() => setAnchorEl(null)}>
        {columns.map(col => {
          const checked = !hiddenColumns.includes(col.id)
          return (
            <MenuItem
              dense
              key={col.id}
              onClick={() => onChange(checked ? [...hiddenColumns, col.id] : hiddenColumns.filter(_ => _ !== col.id))}
            >
              <Checkbox
                sx={{
                  pl: 0,
                  pb: 0.25,
                  pt: 0.25,
                }}
                checked={checked}
              />
              {col.head}
            </MenuItem>
          )
        })}
      </Menu>
    </>
  )
}
