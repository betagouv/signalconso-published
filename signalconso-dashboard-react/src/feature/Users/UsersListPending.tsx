import React, {useEffect} from 'react'
import {Panel} from '../../shared/Panel'
import {Datatable} from '../../shared/Datatable/Datatable'
import {useUsersContext} from '../../core/context/UsersContext'
import {useI18n} from '../../core/i18n'
import {Txt} from '../../alexlibs/mui-extension'

import {useToast} from '../../core/toast'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {IconBtn} from '../../alexlibs/mui-extension'
import {Icon, Tooltip} from '@mui/material'
import {useLogin} from '../../core/context/LoginContext'
import {sxUtils} from '../../core/theme'
import {ScOption} from 'core/helper/ScOption'

export const UsersListPending = () => {
  const _users = useUsersContext().dgccrfPending
  const _invite = useUsersContext().invite
  const {m, formatDate} = useI18n()
  const {connectedUser} = useLogin()
  const {toastError, toastSuccess} = useToast()
  const copyActivationLink = (token: string) => {
    let activationLink = window.location.host + '/#/dgccrf/rejoindre/?token=' + token
    navigator.clipboard.writeText(activationLink).then(_ => toastSuccess(m.addressCopied))
  }

  useEffect(() => {
    _users.fetch()
  }, [])

  useEffect(() => {
    ScOption.from(_users.error).map(toastError)
  }, [_users.error])

  return (
    <Panel>
      <Datatable
        id="userslistpending"
        loading={_users.loading}
        total={_users.entity?.length}
        data={_users.entity}
        columns={[
          {
            id: 'email',
            head: m.email,
            render: _ => <Txt bold>{_.email}</Txt>,
          },
          {
            id: 'tokenCreation',
            head: m.invitationDate,
            render: _ => formatDate(_.tokenCreation),
          },
          {
            id: 'actions',
            sx: _ => sxUtils.tdActions,
            head: '',
            render: _ => (
              <>
                {connectedUser.isAdmin &&
                  ScOption.from(_.email)
                    .map(email => (
                      <ScDialog
                        title={m.resendCompanyAccessToken(_.email)}
                        onConfirm={(event, close) =>
                          _invite
                            .fetch({}, email)
                            .then(_ => close())
                            .then(_ => toastSuccess(m.userInvitationSent))
                        }
                        maxWidth="xs"
                      >
                        <Tooltip title={m.resendInvite}>
                          <IconBtn>
                            <Icon>send</Icon>
                          </IconBtn>
                        </Tooltip>
                      </ScDialog>
                    ))
                    .getOrElse(<></>)}
                {connectedUser.isAdmin &&
                  ScOption.from(_.token)
                    .map(token => (
                      <Tooltip title={m.copyInviteLink}>
                        <IconBtn onClick={_ => copyActivationLink(token)}>
                          <Icon>content_copy</Icon>
                        </IconBtn>
                      </Tooltip>
                    ))
                    .getOrElse(<></>)}
              </>
            ),
          },
        ]}
      />
    </Panel>
  )
}
