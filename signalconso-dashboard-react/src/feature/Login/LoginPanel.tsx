import {Box} from '@mui/material'
import {styleUtils} from '../../core/theme'
import * as React from 'react'
import {ReactNode} from 'react'
import {Panel, PanelBody} from '../../shared/Panel'

interface Props {
  title: string
  children: ReactNode
}

export const LoginPanel = ({title, children}: Props) => {
  return (
    <Panel>
      <PanelBody sx={{p: 4}}>
        <Box
          component="h1"
          sx={{
            mt: 0,
            mb: 3,
            textAlign: 'center',
            fontSize: t => styleUtils(t).fontSize.bigTitle,
          }}
        >
          {title}
        </Box>
        {children}
      </PanelBody>
    </Panel>
  )
}
