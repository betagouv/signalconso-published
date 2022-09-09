import {useI18n} from '../../core/i18n'
import {ScInput} from '../../shared/Input/ScInput'
import {useForm} from 'react-hook-form'
import {regexp} from '../../core/helper/regexp'
import {LoginPanel} from './LoginPanel'
import {ScButton} from '../../shared/Button/Button'
import {Box} from '@mui/material'
import {ActionProps} from './LoginPage'
import {Alert} from '../../alexlibs/mui-extension'
import {Txt} from '../../alexlibs/mui-extension'
import React from 'react'
import {useToast} from '../../core/toast'
import {useHistory} from 'react-router'
import {siteMap} from '../../core/siteMap'
import {ScInputPassword} from '../../shared/InputPassword/InputPassword'
import {AccessEventActions, ActionResultNames, EventCategories, Matomo} from '../../core/plugins/Matomo'
import {ApiError} from '../../core/client/ApiClient'

interface Form {
  siret: string
  code: string
  email: string
  apiError: string
}

interface Props {
  register: ActionProps<(siret: string, code: string, email: string) => Promise<any>>
}

export const ActivateAccountForm = ({register: registerAction}: Props) => {
  const {m} = useI18n()
  const {toastSuccess} = useToast()
  const history = useHistory()
  const {
    register,
    handleSubmit,
    setError,
    clearErrors,
    formState: {errors},
  } = useForm<Form>({mode: 'onChange'})

  const activateAccount = (form: Form) => {
    clearErrors('apiError')
    registerAction
      .action(form.siret, form.code, form.email)
      .then(() => {
        toastSuccess(m.companyRegisteredEmailSent)
        setTimeout(() => history.push(siteMap.loggedout.login), 400)
        Matomo.trackEvent(EventCategories.account, AccessEventActions.activateCompanyCode, ActionResultNames.success)
      })
      .catch((err: ApiError) => {
        setError('apiError', {
          type: err.details.id,
          message: err.message,
        })
        Matomo.trackEvent(EventCategories.companyAccess, AccessEventActions.activateCompanyCode, ActionResultNames.fail)
      })
  }

  return (
    <LoginPanel title={m.youReceivedNewLetter}>
      <form onSubmit={handleSubmit(activateAccount)}>
        {errors.apiError && (
          <Alert type="error" sx={{mb: 2}}>
            <Txt size="big" block bold>
              {m.registerCompanyError}
            </Txt>
            <Txt>{errors.apiError.message}</Txt>
          </Alert>
        )}
        <ScInput
          sx={{mb: 1}}
          fullWidth
          error={!!errors.siret}
          helperText={errors.siret?.message ?? m.siretOfYourCompanyDesc}
          label={m.siretOfYourCompany}
          {...register('siret', {
            required: {value: true, message: m.required},
            pattern: {value: regexp.siret, message: m.siretOfYourCompanyInvalid},
          })}
        />
        <ScInputPassword
          sx={{mb: 1}}
          fullWidth
          error={!!errors.code}
          helperText={errors.code?.message ?? m.activationCodeDesc}
          label={m.activationCode}
          {...register('code', {
            required: {value: true, message: m.required},
            pattern: {value: regexp.activationCode, message: m.activationCodeInvalid},
          })}
        />
        <ScInput
          sx={{mb: 1}}
          fullWidth
          error={!!errors.email}
          helperText={errors.email?.message ?? m.emailDesc}
          label={m.email}
          {...register('email', {
            required: {value: true, message: m.required},
            pattern: {value: regexp.email, message: m.invalidEmail},
          })}
        />
        <Box
          sx={{
            mt: 2,
            display: 'flex',
            alignItems: 'center',
          }}
        >
          <ScButton
            loading={registerAction.loading}
            onClick={_ => clearErrors('apiError')}
            type="submit"
            color="primary"
            variant="contained"
          >
            {m.activateMyAccount}
          </ScButton>
        </Box>
      </form>
    </LoginPanel>
  )
}
