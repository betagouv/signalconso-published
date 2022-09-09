import {Tab, TabProps, Tabs} from '@mui/material'
import * as React from 'react'
import {ReactElement, useMemo, useState} from 'react'
import {useHistory, useLocation} from 'react-router'

interface Props {
  children: Array<ReactElement<PageTabProps>>
}

export const PageTabs = ({children}: Props) => {
  const {pathname} = useLocation()
  const index = useMemo(() => children.map(_ => _.props.to).indexOf(pathname), [pathname])
  const [value, setValue] = useState(Math.max(0, index))

  const handleChange = (event: any, index: number) => {
    setValue(index)
  }

  return (
    <Tabs
      value={value}
      indicatorColor="primary"
      textColor="primary"
      onChange={handleChange}
      sx={{
        mb: 3,
        borderBottom: t => '1px solid ' + t.palette.divider,
      }}
    >
      {children}
    </Tabs>
  )
}

export interface PageTabProps extends TabProps {
  to: string
  label?: string
  icon?: string | React.ReactElement
  disabled?: boolean
}

export const PageTab = ({to, ...props}: PageTabProps) => {
  const history = useHistory()
  return <Tab {...props} onClick={() => history.push(to)} />
}
