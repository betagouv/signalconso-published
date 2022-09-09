import React, {EventHandler, forwardRef, ReactElement, ReactNode, SyntheticEvent, useState} from 'react'
import {Icon, Menu} from '@mui/material'
import {IconBtn} from '../../alexlibs/mui-extension'

interface Props {
  icon?: string
  button?: ReactElement<any>
  children: ReactNode
  onClick?: EventHandler<SyntheticEvent<any>>
}

export const ScMenu = forwardRef(({icon = 'more_vert', children, button, onClick}: Props, ref: any) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const open = Boolean(anchorEl)

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }
  const handleClose = () => {
    setAnchorEl(null)
  }

  return (
    <>
      {button ? (
        React.cloneElement(button, {
          onClick: (event: React.MouseEvent<HTMLElement>) => {
            if (button.props.onClick) button.props.onClick(event)
            if (onClick) onClick(event)
            handleClick(event)
          },
        })
      ) : (
        <IconBtn onClick={handleClick} color="primary">
          <Icon>{icon}</Icon>
        </IconBtn>
      )}
      <Menu anchorEl={anchorEl} open={open} onClose={handleClose}>
        {children}
      </Menu>
    </>
  )
})
