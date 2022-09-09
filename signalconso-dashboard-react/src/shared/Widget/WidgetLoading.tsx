import * as React from 'react'
import {Box, Skeleton} from '@mui/material'
import {widgetBodyHeight} from './WidgetBody'

export const WidgetLoading = () => {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        height: widgetBodyHeight,
      }}
    >
      <Skeleton height={46} width="60%" />
    </Box>
  )
}
