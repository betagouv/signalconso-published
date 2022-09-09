import React, {ReactNode} from 'react'
import {Box, Icon} from '@mui/material'
import {Txt} from '../../alexlibs/mui-extension'

interface Props {
  icon?: string
  children?: ReactNode
  title?: string
  description?: ReactNode
}

export const SettingRow = ({title, description, icon, children}: Props) => {
  return (
    <Box
      sx={{
        py: 1.5,
        px: 2,
        display: 'flex',
        alignItems: 'center',
        '&:not(:last-of-type)': {
          borderBottom: t => '1px solid ' + t.palette.divider,
        },
      }}
    >
      <Icon
        sx={{
          color: t => t.palette.text.secondary,
          mr: 2,
        }}
      >
        {icon}
      </Icon>
      <Box sx={{flex: 1}}>
        <Txt block size="big">
          {title}
        </Txt>
        {description && (
          <Txt block color="hint">
            {description}
          </Txt>
        )}
      </Box>
      <div>{children}</div>
    </Box>
  )
}
