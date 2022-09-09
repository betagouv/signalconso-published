import React, {ReactNode} from 'react'
import {Box} from '@mui/material'

interface ScChipContainer {
  children: ReactNode
}

export const ScChipContainer = ({children}: ScChipContainer) => {
  return (
    <Box
      sx={{
        m: -1 / 2,
        flexWrap: 'wrap',
        display: 'flex',
        '& > *': {
          m: 1 / 2,
        },
      }}
    >
      {children}
    </Box>
  )
}
