import {CenteredContent} from '../../shared/CenteredContent/CenteredContent'
import {Page} from '../../shared/Layout'
import {useForm} from 'react-hook-form'
import {Panel, PanelBody, PanelHead} from '../../shared/Panel'
import {ScInputPassword} from '../../shared/InputPassword/InputPassword'
import React from 'react'
import {useI18n} from '../../core/i18n'
import {PanelFoot} from '../../shared/Panel/PanelFoot'
import {ScButton} from '../../shared/Button/Button'
import {useHistory, useParams} from 'react-router'
import {useAsync} from '../../alexlibs/react-hooks-lib'
import {HelpContactInfo} from '../../shared/HelpContactInfo/HelpContactInfo'
import {AuthenticationEventActions, EventCategories, Matomo} from '../../core/plugins/Matomo'
import {useToast} from '../../core/toast'
import {siteMap} from '../../core/siteMap'
import {layoutConfig} from '../../core/Layout'
import {Id} from '../../core/model'
import {fnSwitch} from '../../alexlibs/ts-utils'

interface Form {
  newPassword: string
  newPasswordConfirmation: string
}

interface Props {
  onResetPassword: (password: string, token: Id) => Promise<any>
}

export const ResetPassword = ({onResetPassword}: Props) => {
  const {m} = useI18n()
  const {token} = useParams<{token: Id}>()
  const _resetPassword = useAsync(onResetPassword)
  const history = useHistory()
  const {toastError, toastSuccess} = useToast()
  const {
    register,
    handleSubmit,
    getValues,
    reset,
    formState: {errors, isValid},
  } = useForm<Form>()

  const resetPassword = (form: Form) => {
    _resetPassword
      .call(form.newPassword, token)
      .then(() => {
        toastSuccess(m.resetPasswordSuccess)
        setTimeout(() => history.push(siteMap.loggedout.login), 400)
        Matomo.trackEvent(EventCategories.account, AuthenticationEventActions.resetPasswordSuccess)
      })
      .catch(err => {
        const errorMessage = fnSwitch(
          err.code,
          {
            404: m.resetPasswordNotFound,
          },
          () => undefined,
        )
        toastError({message: errorMessage})
        reset()
        Matomo.trackEvent(EventCategories.account, AuthenticationEventActions.resetPasswordFail)
      })
  }

  return (
    <CenteredContent offset={layoutConfig.headerHeight}>
      <Page size="s" sx={{maxWidth: `500px !important`}}>
        <form onSubmit={handleSubmit(resetPassword)}>
          <Panel>
            <PanelHead>{m.newPassword}</PanelHead>
            <PanelBody>
              <ScInputPassword
                error={!!errors.newPassword}
                helperText={errors.newPassword?.message ?? ' '}
                fullWidth
                label={m.newPassword}
                {...register('newPassword', {
                  required: {value: true, message: m.required},
                  minLength: {value: 8, message: m.passwordNotLongEnough},
                  validate: value => value === getValues().newPasswordConfirmation || m.passwordDoesntMatch,
                })}
              />
              <ScInputPassword
                error={!!errors.newPasswordConfirmation}
                helperText={errors.newPasswordConfirmation?.message ?? ' '}
                fullWidth
                label={m.newPasswordConfirmation}
                {...register('newPasswordConfirmation', {
                  required: {value: true, message: m.required},
                  minLength: {value: 8, message: m.passwordNotLongEnough},
                  validate: value => value === getValues().newPassword || m.passwordDoesntMatch,
                })}
              />
            </PanelBody>
            <PanelFoot alignEnd>
              <ScButton variant="contained" color="primary" type="submit">
                {m.validate}
              </ScButton>
            </PanelFoot>
          </Panel>
          <HelpContactInfo />
        </form>
      </Page>
    </CenteredContent>
  )
}
