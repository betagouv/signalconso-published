import {Page, PageTitle} from '../../shared/Layout'
import {useI18n} from '../../core/i18n'
import React from 'react'
import {PageTab, PageTabs} from '../../shared/Layout/Page/PageTabs'
import {siteMap} from '../../core/siteMap'
import {Redirect, Route, Switch, useRouteMatch} from 'react-router-dom'
import {UsersList} from './UsersList'
import {UsersListPending} from './UsersListPending'
import {ScButton} from '../../shared/Button/Button'
import {Alert} from '../../alexlibs/mui-extension'
import {ScInput} from '../../shared/Input/ScInput'
import {useForm} from 'react-hook-form'
import {Txt} from '../../alexlibs/mui-extension'
import {useUsersContext} from '../../core/context/UsersContext'
import {regexp} from '../../core/helper/regexp'
import {useToast} from '../../core/toast'

import {ScDialog} from '../../shared/Confirm/ScDialog'
import {ConsumerListPending} from './ConsumerListPending'
import {ScOption} from 'core/helper/ScOption'

export const Users = () => {
  const {m} = useI18n()
  const {path, url} = useRouteMatch()
  const {
    register,
    handleSubmit,
    formState: {errors, isValid},
  } = useForm<{email: string}>({mode: 'onChange'})
  const _invite = useUsersContext().invite
  const {toastSuccess} = useToast()

  return (
    <Page>
      <PageTitle
        action={
          <ScDialog
            maxWidth="xs"
            onConfirm={(event, close) => {
              handleSubmit(({email}) => {
                _invite
                  .fetch({}, email)
                  .then(() => toastSuccess(m.userInvitationSent))
                  .then(close)
              })()
            }}
            confirmLabel={m.invite}
            loading={_invite.loading}
            confirmDisabled={!isValid}
            title={m.users_invite_dialog_title}
            content={
              <>
                {ScOption.from(_invite.error?.details?.id)
                  .map(errId => (
                    <Alert dense type="error" deletable gutterBottom>
                      {m.apiErrorsCode[errId as keyof typeof m.apiErrorsCode]}
                    </Alert>
                  ))
                  .toUndefined()}
                <Txt color="hint" block gutterBottom>
                  {m.users_invite_dialog_desc}
                </Txt>
                <ScInput
                  autoFocus
                  fullWidth
                  label={m.email}
                  error={!!errors.email}
                  helperText={errors.email?.message}
                  {...register('email', {
                    required: m.required,
                    pattern: {
                      value: regexp.emailDGCCRF,
                      message: m.emailDGCCRFValidation,
                    },
                  })}
                />
              </>
            }
          >
            <ScButton icon="person_add" variant="contained" color="primary">
              {m.invite}
            </ScButton>
          </ScDialog>
        }
      >
        {m.menu_users}
      </PageTitle>
      <PageTabs>
        <PageTab to={siteMap.logged.users_dgccrf_all} label={m.dgccrfUsers} />
        <PageTab to={siteMap.logged.users_dgccrf_pending} label={m.dgccrfUsersPending} />
        <PageTab to={siteMap.logged.users_consumer_validation} label={m.consumersPending} />
      </PageTabs>
      <Switch>
        <Redirect exact from={path} to={siteMap.logged.users_dgccrf_all} />
        <Route path={siteMap.logged.users_dgccrf_all} component={UsersList} />
        <Route path={siteMap.logged.users_dgccrf_pending} component={UsersListPending} />
        <Route path={siteMap.logged.users_consumer_validation} component={ConsumerListPending} />
      </Switch>
    </Page>
  )
}
