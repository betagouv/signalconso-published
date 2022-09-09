import React, {ReactElement} from 'react'
import {useI18n} from '../../core/i18n'
import {Controller, useForm} from 'react-hook-form'
import {Alert} from '../../alexlibs/mui-extension'
import {useToast} from '../../core/toast'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {AccountEventActions, EventCategories, Matomo} from '../../core/plugins/Matomo'
import {useAsync} from '../../alexlibs/react-hooks-lib'
import {useLogin} from '../../core/context/LoginContext'
import {map} from '../../alexlibs/ts-utils'
import {ScInput} from '../../shared/Input/ScInput'
import {ApiError} from '../../core/client/ApiClient'

interface Form {
  firstName: string
  lastName: string
}

interface Props {
  children: ReactElement<any>
}

export const EditProfileDialog = ({children}: Props) => {
  const {m} = useI18n()
  const {apiSdk, connectedUser, setConnectedUser} = useLogin()
  const _editUser = useAsync(apiSdk.secured.user.edit)
  const {toastSuccess} = useToast()
  const defaultFormValues: Form = {
    firstName: connectedUser.firstName,
    lastName: connectedUser.lastName,
  }
  const {
    getValues,
    handleSubmit,
    control,
    reset,
    watch,
    formState: {errors, isValid},
  } = useForm<Form>({
    mode: 'onChange',
    defaultValues: defaultFormValues,
  })

  watch('firstName')
  watch('lastName')
  const hasUserChanged = getValues().firstName !== connectedUser.firstName || getValues().lastName !== connectedUser.lastName

  return (
    <ScDialog
      title={m.editName}
      maxWidth="xs"
      onOpen={() => {
        reset(defaultFormValues)
      }}
      confirmLabel={m.edit}
      confirmDisabled={!isValid || !hasUserChanged}
      loading={_editUser.loading}
      onConfirm={(event, close) => {
        handleSubmit((form: Form) => {
          _editUser
            .call(connectedUser.id, form)
            .then(() => {
              toastSuccess(m.saved)
              close()
              Matomo.trackEvent(EventCategories.account, AccountEventActions.changeNameSuccess)
              setConnectedUser(_ => ({..._, ...form}))
            })
            .catch(_ => {
              Matomo.trackEvent(EventCategories.account, AccountEventActions.changeNameFail)
            })
        })()
      }}
      content={
        <>
          {map(_editUser.error, (error: ApiError) => (
            <Alert dense type="error" deletable gutterBottom>
              {error.message ?? m.anErrorOccurred}
            </Alert>
          ))}
          <Controller
            name="firstName"
            control={control}
            render={({field}) => (
              <ScInput
                {...field}
                autoComplete="false"
                error={!!errors.firstName}
                helperText={errors.firstName?.message ?? ' '}
                fullWidth
                label={m.firstName}
              />
            )}
          />
          <Controller
            name="lastName"
            control={control}
            render={({field}) => (
              <ScInput
                {...field}
                autoComplete="false"
                error={!!errors.lastName}
                helperText={errors.lastName?.message ?? ' '}
                fullWidth
                label={m.lastName}
              />
            )}
          />
        </>
      }
    >
      {children}
    </ScDialog>
  )
}
