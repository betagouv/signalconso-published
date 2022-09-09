import {Box} from '@mui/material'
import {ReactNode} from 'react'
import {WidgetBody} from './WidgetBody'

interface Props {
  children: ReactNode
}

export const WidgetValue = ({children}: Props) => {
  return (
    <WidgetBody>
      <Box
        sx={{
          fontSize: 36,
          lineHeight: 1,
        }}
      >
        {children}
      </Box>
    </WidgetBody>
  )
}
