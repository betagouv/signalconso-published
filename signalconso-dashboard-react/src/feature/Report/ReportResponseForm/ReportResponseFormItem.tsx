import {Txt} from '../../../alexlibs/mui-extension'
import React from 'react'
import {Box} from '@mui/material'

interface ReportAnswerProItemProps {
  title: string
  desc?: string
  children?: any
}

export const ReportResponseFormItem = ({children, title, desc}: ReportAnswerProItemProps) => {
  return (
    <Box sx={{mb: 2}}>
      <Txt block size="big" bold>
        {title}
      </Txt>
      {desc && (
        <Txt color="hint">
          <span dangerouslySetInnerHTML={{__html: desc}} />
        </Txt>
      )}
      <Box sx={{mt: 0.5}}>{children}</Box>
    </Box>
  )
}
