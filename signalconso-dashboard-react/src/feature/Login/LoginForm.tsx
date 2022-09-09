import * as React from 'react'
import {useI18n} from '../../core/i18n'
import {regexp} from '../../core/helper/regexp'
import {LoginPanel} from './LoginPanel'
import {ActionProps} from './LoginPage'
import {ScInputPassword} from '../../shared/InputPassword/InputPassword'
import {ScInput} from '../../shared/Input/ScInput'
import {useForm} from 'react-hook-form'
import {ScButton} from '../../shared/Button/Button'
import {ForgottenPasswordDialog} from './ForgottenPasswordDialog'
import {AuthenticationEventActions, EventCategories, Matomo} from '../../core/plugins/Matomo'
import {Alert} from '../../alexlibs/mui-extension'
import {Txt} from '../../alexlibs/mui-extension'
import {ApiError} from '../../core/client/ApiClient'
import {SignalConsoPublicSdk} from '../../core/client/SignalConsoPublicSdk'
import {ScOption} from 'core/helper/ScOption'

interface Props {
  login: ActionProps<SignalConsoPublicSdk['authenticate']['login']>
  forgottenPassword?: ActionProps<SignalConsoPublicSdk['authenticate']['forgotPassword']>
}

interface Form {
  email: string
  password: string
  apiError: string
}

export const LoginForm = ({login, forgottenPassword}: Props) => {
  const {m} = useI18n()

  const {
    register,
    handleSubmit,
    watch,
    setError,
    clearErrors,
    formState: {errors},
  } = useForm<Form>({mode: 'onSubmit'})

  const onLogin = async (form: Form) => {
    login
      .action(form.email, form.password)
      .then(auth => {
        Matomo.trackEvent(EventCategories.auth, AuthenticationEventActions.success, auth.user.id)
        Matomo.trackEvent(EventCategories.auth, AuthenticationEventActions.role, auth.user.role)
      })
      .catch((err: ApiError) => {
        setError('apiError', {
          type: err.details.id,
          message: err.message,
        })
        Matomo.trackEvent(EventCategories.auth, AuthenticationEventActions.fail)
      })
  }

  return (
    <LoginPanel title={m.login}>
      <form style={{display: 'flex', flexDirection: 'column'}} onSubmit={handleSubmit(onLogin)} action="#">
        {errors.apiError && (
          <Alert type="error" sx={{mb: 2}}>
            <Txt size="big" block bold>
              {m.somethingWentWrong}
            </Txt>
            <Txt>{errors.apiError.message}</Txt>
          </Alert>
        )}
        <ScInput
          autoComplete="username"
          autoFocus
          type="email"
          error={!!errors.email}
          helperText={errors.email?.message ?? ' '}
          label={m.email}
          sx={{mb: 1}}
          {...register('email', {
            required: {value: true, message: m.required},
            pattern: {value: regexp.email, message: m.invalidEmail},
          })}
        />
        <ScInputPassword
          autoComplete="current-password"
          label={m.password}
          error={!!errors.password}
          helperText={errors.password?.message ?? ' '}
          sx={{mb: 1}}
          {...register('password', {
            required: {value: true, message: m.atLeast8Characters},
          })}
        />
        <div style={{display: 'flex', alignItems: 'center'}}>
          <ScButton
            loading={login.loading}
            type="submit"
            onClick={_ => clearErrors('apiError')}
            variant="contained"
            color="primary"
            sx={{mr: 1}}
          >
            {m.login}
          </ScButton>
          {ScOption.from(forgottenPassword)
            .map(_ => (
              <ForgottenPasswordDialog value={watch('email')} loading={_.loading} error={_.error} onSubmit={_.action}>
                <ScButton color="primary">{m.forgottenPassword}</ScButton>
              </ForgottenPasswordDialog>
            ))
            .toUndefined()}
        </div>
      </form>
    </LoginPanel>
  )
}
