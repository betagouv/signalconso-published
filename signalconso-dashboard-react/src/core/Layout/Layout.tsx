import * as React from 'react'
import {ReactElement, ReactNode} from 'react'
import {LayoutProvider, useLayoutContext} from './LayoutContext'
import {Box} from '@mui/material'
import {layoutConfig} from './index'
import {defaultSpacing} from '../theme'

export const sidebarWith = 220

export interface LayoutProps {
  sidebar?: ReactElement<any>
  header?: ReactElement<any>
  title?: string
  children?: ReactNode
  mobileBreakpoint?: number
}

export const Layout = ({sidebar, header, title, mobileBreakpoint, children}: LayoutProps) => {
  return (
    <LayoutProvider title={title} mobileBreakpoint={mobileBreakpoint} showSidebarButton={!!sidebar}>
      <LayoutUsingContext sidebar={sidebar} header={header}>
        {children}
      </LayoutUsingContext>
    </LayoutProvider>
  )
}

const LayoutUsingContext = ({sidebar, header, children}: Pick<LayoutProps, 'sidebar' | 'header' | 'children'>) => {
  const {sidebarOpen, sidebarPinned, isMobileWidth} = useLayoutContext()
  return (
    <>
      {header}
      {sidebar}
      <Box
        component="main"
        sx={{
          transition: t => t.transitions.create('all'),
          paddingLeft:
            (sidebar && sidebarOpen && sidebarPinned && !isMobileWidth ? layoutConfig.sidebarWith + defaultSpacing : 0) + 'px',
          overflow: 'hidden',
          position: 'relative',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {children}
      </Box>
    </>
  )
}
