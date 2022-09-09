import * as React from 'react'
import {createContext, Dispatch, ReactNode, SetStateAction, useContext, useEffect, useState} from 'react'
import {usePersistentState} from '../../alexlibs/react-persistent-state'

const LayoutContext = createContext<UseLayoutContextProps>({} as UseLayoutContextProps)

export interface LayoutProviderProps {
  children: ReactNode
  mobileBreakpoint?: number
  title?: string
  showSidebarButton?: boolean
}

export interface UseLayoutContextProps {
  sidebarOpen: boolean
  setSidebarOpen: Dispatch<SetStateAction<boolean>>
  sidebarPinned: boolean
  setSidebarPinned: Dispatch<SetStateAction<boolean>>
  title?: string
  isMobileWidth: boolean
  showSidebarButton?: boolean
}

export const LayoutProvider = ({title, showSidebarButton, mobileBreakpoint = 760, children}: LayoutProviderProps) => {
  const [pageWidth, setPageWidth] = useState(getWidth())
  const [sidebarOpen, setSidebarOpen] = usePersistentState(true, 'sidebarOpen')
  const [sidebarPinned, setSidebarPinned] = usePersistentState(true, 'sidebarPinned')

  useEffect(() => {
    window.addEventListener('resize', () => setPageWidth(getWidth()))
  }, [])

  return (
    <LayoutContext.Provider
      value={{
        sidebarOpen,
        setSidebarOpen,
        sidebarPinned,
        setSidebarPinned,
        title,
        isMobileWidth: pageWidth < mobileBreakpoint,
        showSidebarButton,
      }}
    >
      {children}
    </LayoutContext.Provider>
  )
}

function getWidth(): number {
  return window.outerWidth
}

export const useLayoutContext = (): UseLayoutContextProps => {
  return useContext<UseLayoutContextProps>(LayoutContext)
}
