import {ReactNode} from 'react'
import {Box} from '@mui/material'

interface Props {
  children: ReactNode
}

export const widgetBodyHeight = 44

export const WidgetBody = ({children}: Props) => {
  return (
    <Box
      sx={{
        height: widgetBodyHeight,
        display: 'flex',
        alignItems: 'center',
      }}
    >
      {children}
    </Box>
  )
}
