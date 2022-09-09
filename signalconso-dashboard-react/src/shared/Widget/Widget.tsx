import {Panel, PanelBody} from '../Panel'
import {Txt} from '../../alexlibs/mui-extension'
import * as React from 'react'
import {ReactNode} from 'react'
import {Icon} from '@mui/material'
import {NavLink} from 'react-router-dom'
import {styleUtils} from '../../core/theme'

interface Props {
  title: ReactNode
  children: ReactNode
  loading?: boolean
  to?: string
}

export const Widget = ({loading, title, to, children}: Props) => {
  const body = (
    <Panel
      sx={{
        ...(to && {
          transition: t => t.transitions.create('all'),
          '&:hover': {
            borderColor: t => t.palette.background.default,
            boxShadow: t => t.shadows[3],
          },
        }),
      }}
    >
      <PanelBody>
        <Txt
          block
          uppercase
          color="hint"
          size="small"
          gutterBottom
          sx={{
            display: 'flex',
            alignItems: 'center',
          }}
        >
          {title}
          {to && (
            <Icon
              fontSize="inherit"
              sx={{
                ml: 1,
                color: t => t.palette.text.disabled,
                fontSize: t => styleUtils(t).fontSize.big,
              }}
            >
              open_in_new
            </Icon>
          )}
        </Txt>
        {children}
      </PanelBody>
    </Panel>
  )
  return to ? <NavLink to={to}>{body}</NavLink> : body
}
