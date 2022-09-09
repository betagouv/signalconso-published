import {Box, Icon, Menu, MenuItem} from '@mui/material'
import logoGouvMobile from './gouv-mobile.svg'
import logoDgccrf from './logo-dgccrf.png'
import {config} from '../../conf/config'
import logoSignalConso from './signalconso.svg'
import {Btn, IconBtn} from '../../alexlibs/mui-extension'
import React from 'react'
import {useI18n} from '../i18n'
import {useLayoutContext} from '../Layout/LayoutContext'
import {styleUtils} from '../theme'
import {Header} from '../Layout/Header/Header'

const HeaderItem = ({children, href}: {children: any; href: string}) => {
  return (
    <Btn
      color="primary"
      href={href}
      sx={{
        textTransform: 'initial',
        fontSize: t => styleUtils(t).fontSize.normal,
        py: 0,
        px: 2,
      }}
    >
      {children}
    </Btn>
  )
}

export const ScHeader = () => {
  const {isMobileWidth} = useLayoutContext()
  const {m} = useI18n()
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => {
    setAnchorEl(null)
  }
  return (
    <Header>
      <Box
        component="img"
        src={logoGouvMobile}
        alt={m.altLogoGouv}
        sx={{
          height: 40,
          mr: 2,
        }}
      />
      {!isMobileWidth && (
        <Box
          component="img"
          src={logoDgccrf}
          alt={m.altLogoDGCCRF}
          sx={{
            height: 40,
            mr: 2,
          }}
        />
      )}
      <a href={config.appBaseUrl}>
        <Box
          component="img"
          src={logoSignalConso}
          alt={m.altLogoSignalConso}
          sx={{
            height: 40,
            mr: 1,
          }}
        />
      </a>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          marginLeft: 'auto',
        }}
      >
        {isMobileWidth ? (
          <>
            <IconBtn aria-haspopup="true" onClick={handleClick}>
              <Icon>menu</Icon>
            </IconBtn>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleClose}>
              <a href={config.appBaseUrl}>
                <MenuItem>{m.home}</MenuItem>
              </a>
              <a href={config.appBaseUrl + '/comment-ca-marche'}>
                <MenuItem>{m.howItWorks}</MenuItem>
              </a>
              <a href={config.appBaseUrl + '/centre-aide'}>
                <MenuItem>{m.helpCenter}</MenuItem>
              </a>
            </Menu>
          </>
        ) : (
          <nav>
            <HeaderItem href={config.appBaseUrl}>{m.home}</HeaderItem>
            <HeaderItem href={config.appBaseUrl + '/comment-ca-marche'}>{m.howItWorks}</HeaderItem>
            <HeaderItem href={config.appBaseUrl + '/centre-aide'}>{m.helpCenter}</HeaderItem>
          </nav>
        )}
      </div>
    </Header>
  )
}
