import {Page, PageTitle} from '../../shared/Layout'
import React from 'react'
import {useI18n} from '../../core/i18n'
import {Panel} from '../../shared/Panel'
import {SettingRow} from './SettingRow'
import {NavLink} from 'react-router-dom'
import {siteMap} from '../../core/siteMap'
import {ScButton} from '../../shared/Button/Button'
import {EditPasswordDialog} from './EditPasswordDialog'
import {useLogin} from '../../core/context/LoginContext'
import {EditProfileDialog} from './EditProfileDialog'

export const Settings = () => {
  const {m} = useI18n()
  const {connectedUser} = useLogin()

  return (
    <Page size="s">
      <PageTitle>{m.menu_settings}</PageTitle>
      <Panel>
        <SettingRow
          icon="person"
          title={m.name}
          description={
            <>
              {connectedUser.firstName} {connectedUser.lastName}
            </>
          }
        >
          <EditProfileDialog>
            <ScButton icon="edit" color="primary">
              {m.edit}
            </ScButton>
          </EditProfileDialog>
        </SettingRow>
        <SettingRow icon="vpn_key" title={m.password} description={m.editPasswordDesc}>
          <EditPasswordDialog>
            <ScButton icon="edit" color="primary">
              {m.edit}
            </ScButton>
          </EditPasswordDialog>
        </SettingRow>
        {connectedUser.isPro && (
          <SettingRow icon="notifications" title={m.notifications} description={m.notificationSettings}>
            <NavLink to={siteMap.logged.companiesPro}>
              <ScButton icon="edit" color="primary">
                {m.edit}
              </ScButton>
            </NavLink>
          </SettingRow>
        )}
      </Panel>
    </Page>
  )
}
