import * as React from 'react'
import {ReactNode} from 'react'
import {Page as MxPage} from '../../../alexlibs/mui-extension'
import {LinearProgress} from '@mui/material'
import {PageProps as MxPageProps} from '../../../alexlibs/mui-extension'

export const pageWidth = {
  xl: 1400,
  l: 1100,
  m: 932,
  s: 680,
}

export interface PageProps extends MxPageProps {
  large?: boolean
  size?: 'xl' | 'l' | 's' | 'm'
  children: ReactNode
  loading?: boolean
}

export const Page = ({loading, size, sx, ...props}: PageProps) => {
  return (
    <>
      {loading && (
        <div style={{position: 'relative'}}>
          <LinearProgress
            sx={{
              position: 'absolute',
              top: 0,
              right: 0,
              left: 0,
            }}
          />
        </div>
      )}
      <MxPage
        {...props}
        sx={{
          p: 2,
          pt: 3,
          ...sx,
        }}
        width={pageWidth[size ?? 'm']}
        {...props}
      />
    </>
  )
}
