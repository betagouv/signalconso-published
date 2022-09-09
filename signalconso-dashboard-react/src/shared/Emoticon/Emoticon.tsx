import * as React from 'react'

import {Box, BoxProps} from '@mui/material'

export interface EmoticonProps extends BoxProps {
  children: React.ReactNode
}

export const Emoticon = ({children, sx, ...props}: EmoticonProps) => {
  return (
    <Box sx={sx} role="img" {...props}>
      {children}
    </Box>
  )
}
