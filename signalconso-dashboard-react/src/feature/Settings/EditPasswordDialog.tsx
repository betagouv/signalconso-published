import React, {ReactElement} from 'react'
import {useI18n} from '../../core/i18n'
import {Txt} from '../../alexlibs/mui-extension'
import {useForm} from 'react-hook-form'
import {useUsersContext} from '../../core/context/UsersContext'
import {Alert} from '../../alexlibs/mui-extension'
import {useToast} from '../../core/toast'

import {ScDialog} from '../../shared/Confirm/ScDialog'
import {ScInputPassword} from '../../shared/InputPassword/InputPassword'
import {AccountEventActions, EventCategories, Matomo} from '../../core/plugins/Matomo'
import {ScOption} from 'core/helper/ScOption'

interface Form {
  oldPassword: string
  newPassword: string
  newPasswordConfirmation: string
}

interface Props {
  children: ReactElement<any>
}

export const EditPasswordDialog = ({children}: Props) => {
  const {m} = useI18n()
  const _changePassword = useUsersContext().changePassword
  const {
    register,
    handleSubmit,
    getValues,
    formState: {errors, isValid},
  } = useForm<Form>({mode: 'onChange'})
  const {toastSuccess} = useToast()

  return (
    <ScDialog
      title={m.editPassword}
      maxWidth="xs"
      confirmLabel={m.edit}
      confirmDisabled={!isValid}
      loading={_changePassword.loading}
      onConfirm={(event, close) => {
        handleSubmit((form: Form) => {
          _changePassword
            .fetch({}, form.oldPassword, form.newPassword)
            .then(() => {
              toastSuccess(m.passwordEdited)
              close()
              Matomo.trackEvent(EventCategories.account, AccountEventActions.changePasswordSuccess)
            })
            .catch(_ => {
              Matomo.trackEvent(EventCategories.account, AccountEventActions.changePasswordFail)
            })
        })()
      }}
      content={
        <>
          {ScOption.from(_changePassword.error)
            .map(error => (
              <Alert dense type="error" deletable gutterBottom>
                {error.details?.code === 401 ? m.invalidPassword : m.failedToChangePassword}
              </Alert>
            ))
            .toUndefined()}
          <Txt color="hint" block gutterBottom>
            {m.editPasswordDialogDesc}
          </Txt>
          <ScInputPassword
            inputProps={{
              autocomplete: 'false',
            }}
            autoComplete="false"
            error={!!errors.oldPassword}
            helperText={errors.oldPassword?.message ?? ' '}
            fullWidth
            label={m.oldPassword}
            {...register('oldPassword', {
              required: {value: true, message: m.required},
              minLength: {value: 8, message: m.passwordNotLongEnough},
            })}
          />
          <ScInputPassword
            error={!!errors.newPassword}
            helperText={errors.newPassword?.message ?? ' '}
            fullWidth
            label={m.newPassword}
            {...register('newPassword', {
              required: {value: true, message: m.required},
              minLength: {value: 8, message: m.passwordNotLongEnough},
              validate: value => value !== getValues().oldPassword || m.passwordAreIdentical,
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
        </>
      }
    >
      {children}
    </ScDialog>
  )
}
