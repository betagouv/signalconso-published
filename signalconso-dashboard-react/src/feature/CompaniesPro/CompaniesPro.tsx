import {Page, PageTitle} from '../../shared/Layout'
import {useI18n} from '../../core/i18n'
import React, {useEffect, useMemo, useState} from 'react'
import {Panel, PanelBody} from '../../shared/Panel'
import {useCompaniesContext} from '../../core/context/CompaniesContext'
import {Datatable} from '../../shared/Datatable/Datatable'
import {Box, Icon, Switch, Tooltip, useTheme} from '@mui/material'
import {styleUtils, sxUtils} from '../../core/theme'
import {Fender, IconBtn} from '../../alexlibs/mui-extension'
import {ScButton} from '../../shared/Button/Button'
import {AddressComponent} from '../../shared/Address/Address'
import {siteMap} from '../../core/siteMap'
import {NavLink} from 'react-router-dom'
import {useUsersContext} from '../../core/context/UsersContext'
import {useBlockedReportNotificationContext} from '../../core/context/BlockedReportNotificationProviderContext'
import {useToast} from '../../core/toast'
import {Txt} from '../../alexlibs/mui-extension'
import {ConfirmDisableNotificationDialog} from './ConfirmDisableNotificationDialog'
import {groupBy} from '../../core/lodashNamedExport'
import {PanelFoot} from '../../shared/Panel/PanelFoot'
import {AccessLevel, Id} from '../../core/model'
import {ScOption} from 'core/helper/ScOption'

export const CompaniesPro = () => {
  const {m} = useI18n()
  const theme = useTheme()
  const _companies = useCompaniesContext()
  const _blockedNotifications = useBlockedReportNotificationContext()
  const _users = useUsersContext()
  const {toastError} = useToast()
  const [state, setState] = useState<Id | Id[] | undefined>()

  useEffect(() => {
    _users.getConnectedUser.fetch({force: false})
    _blockedNotifications.list.fetch()
    _companies.accessibleByPro.fetch({force: false})
  }, [])

  const blockedNotificationIndex = useMemo(() => {
    return ScOption.from(_blockedNotifications.list.entity)
      .map(blockedNotification => groupBy(blockedNotification, _ => _.companyId))
      .getOrElse(undefined)
  }, [_blockedNotifications.list.entity])

  useEffect(() => {
    ScOption.from(_blockedNotifications.create.error)
      .map(toastError)
      .map(() => _blockedNotifications.list.fetch({clean: false}))
    ScOption.from(_blockedNotifications.remove.error)
      .map(toastError)
      .map(() => _blockedNotifications.list.fetch({clean: false}))
  }, [_blockedNotifications.create.error, _blockedNotifications.remove.error])

  const allNotificationsAreBlocked = useMemo(() => {
    if (_companies.accessibleByPro.entity && blockedNotificationIndex) {
      return _companies.accessibleByPro.entity?.every(_ => blockedNotificationIndex[_.id])
    }
    return false
  }, [_companies.accessibleByPro.entity, blockedNotificationIndex])

  return (
    <Page size="s">
      <PageTitle
        action={
          <NavLink to={siteMap.loggedout.register}>
            <ScButton icon="add" color="primary" variant="outlined">
              {m.addACompany}
            </ScButton>
          </NavLink>
        }
      >
        {m.myCompanies}
      </PageTitle>

      {ScOption.from(_companies.accessibleByPro.entity)
        .map(
          companies =>
            companies.length > 5 && (
              <Panel>
                <PanelBody>
                  <Txt block size="big" bold>
                    {m.notifications}
                  </Txt>
                  <Txt block color="hint">
                    {m.notificationAcceptForCompany}
                  </Txt>
                </PanelBody>
                <PanelFoot alignEnd>
                  <ScButton
                    disabled={allNotificationsAreBlocked}
                    color="primary"
                    icon="notifications_off"
                    onClick={() => setState(companies.map(_ => _.id))}
                  >
                    {m.disableAll}
                  </ScButton>
                  <ScButton
                    disabled={_blockedNotifications.list.entity?.length === 0}
                    color="primary"
                    icon="notifications_active"
                    sx={{mr: 1}}
                    onClick={() => _blockedNotifications.remove.call(companies.map(_ => _.id))}
                  >
                    {m.enableAll}
                  </ScButton>
                </PanelFoot>
              </Panel>
            ),
        )
        .getOrElse(undefined)}

      <Panel>
        <Datatable
          data={_companies.accessibleByPro.entity}
          loading={_companies.accessibleByPro.loading || _blockedNotifications.list.loading}
          getRenderRowKey={_ => _.id}
          columns={[
            {
              id: '',
              style: {
                lineHeight: 1.4,
                maxWidth: 200,
              },
              head: m.name,
              render: _ => (
                <Tooltip title={_.name}>
                  <span>
                    <Box
                      component="span"
                      sx={{
                        fontWeight: 'bold',
                        mb: '-1px',
                      }}
                    >
                      {_.name}
                    </Box>
                    <br />
                    <Box
                      component="span"
                      sx={{
                        fontSize: t => styleUtils(t).fontSize.small,
                        color: t => t.palette.text.disabled,
                      }}
                    >
                      {_.siret}
                    </Box>
                  </span>
                </Tooltip>
              ),
            },
            {
              head: m.address,
              id: 'address',
              sx: _ => ({
                maxWidth: 240,
                color: t => t.palette.text.secondary,
                ...styleUtils(theme).truncate,
              }),
              render: _ => (
                <Tooltip title={<AddressComponent address={_.address} />}>
                  <span>
                    <AddressComponent address={_.address} />
                  </span>
                </Tooltip>
              ),
            },
            {
              head: (
                <Box component="span" sx={{whiteSpace: 'nowrap', verticalAlign: 'middle'}}>
                  {m.notification}
                </Box>
              ),
              id: 'status',
              sx: _ => ({
                width: 0,
                textAlign: 'center',
                p: 0,
              }),
              render: _ => (
                <>
                  <Switch
                    disabled={!blockedNotificationIndex}
                    checked={!blockedNotificationIndex?.[_.id]}
                    onChange={e => {
                      e.target.checked ? _blockedNotifications.remove.call([_.id]) : setState(_.id)
                    }}
                  />
                </>
              ),
            },
            {
              head: '',
              id: 'actions',
              sx: _ => sxUtils.tdActions,
              render: _ => (
                <>
                  {_.level === AccessLevel.ADMIN && (
                    <NavLink to={siteMap.logged.companyAccesses(_.siret)}>
                      <Tooltip title={m.handleAccesses}>
                        <IconBtn color="primary">
                          <Icon>vpn_key</Icon>
                        </IconBtn>
                      </Tooltip>
                    </NavLink>
                  )}
                  <NavLink to={siteMap.logged.company(_.id)}>
                    <Tooltip title={m.myStats}>
                      <IconBtn color="primary">
                        <Icon>bar_chart</Icon>
                      </IconBtn>
                    </Tooltip>
                  </NavLink>
                  <NavLink to={siteMap.logged.reports({siretSirenList: [_.siret]})}>
                    <Tooltip title={m.reports}>
                      <IconBtn color="primary">
                        <Icon>chevron_right</Icon>
                      </IconBtn>
                    </Tooltip>
                  </NavLink>
                </>
              ),
            },
          ]}
          renderEmptyState={
            <Fender
              title={m.noCompanyFound}
              icon="store"
              sx={{
                margin: 'auto',
                mt: 1,
                mb: 2,
              }}
            >
              <ScButton variant="contained" color="primary" icon="add" sx={{mt: 1}}>
                {m.registerACompany}
              </ScButton>
            </Fender>
          }
        />
      </Panel>
      <ConfirmDisableNotificationDialog
        open={!!state}
        onClose={() => setState(undefined)}
        onConfirm={() => {
          _blockedNotifications.create.call([state!].flatMap(_ => _))
          setState(undefined)
        }}
      />
    </Page>
  )
}
