import React from 'react'
import {Page} from '../../shared/Layout'
import {LoginPanel} from '../Login/LoginPanel'
import {useI18n} from '../../core/i18n'
import {ScInput} from '../../shared/Input/ScInput'
import {regexp} from '../../core/helper/regexp'
import {ScButton} from '../../shared/Button/Button'
import {Box} from '@mui/material'
import {useLogin} from '../../core/context/LoginContext'
import {useForm} from 'react-hook-form'
import {useAccessesContext} from '../../core/context/AccessesContext'
import {useToast} from '../../core/toast'
import {Txt} from '../../alexlibs/mui-extension'
import {useHistory} from 'react-router'
import {siteMap} from '../../core/siteMap'
import {Alert} from '../../alexlibs/mui-extension'
import {ScInputPassword} from '../../shared/InputPassword/InputPassword'
import {AccessEventActions, ActionResultNames, EventCategories, Matomo} from '../../core/plugins/Matomo'
import {HelpContactInfo} from '../../shared/HelpContactInfo/HelpContactInfo'

interface Form {
  siret: string
  code: string
}

export const ActivateNewCompany = () => {
  const {m} = useI18n()
  const {connectedUser} = useLogin()
  const _acceptToken = useAccessesContext().acceptToken
  const {toastSuccess} = useToast()
  const history = useHistory()

  const {
    register,
    handleSubmit,
    formState: {errors},
  } = useForm<Form>({mode: 'onSubmit'})

  const acceptToken = (form: Form) => {
    _acceptToken
      .call(form.siret, form.code)
      .then(() => {
        toastSuccess(m.companyRegistered)
        history.push(siteMap.logged.companiesPro)
        Matomo.trackEvent(EventCategories.companyAccess, AccessEventActions.addCompanyToAccount, ActionResultNames.success)
      })
      .catch(() => {
        Matomo.trackEvent(EventCategories.companyAccess, AccessEventActions.addCompanyToAccount, ActionResultNames.fail)
      })
  }

  return (
    <Page size="s">
      <LoginPanel title={m.youReceivedNewLetter}>
        {_acceptToken.error && (
          <Alert type="error" sx={{mb: 2}}>
            <Txt size="big" block bold>
              {m.registerCompanyError}
            </Txt>
            <Txt>{m.registerCompanyErrorDesc}</Txt>
          </Alert>
        )}
        <form onSubmit={handleSubmit(acceptToken)}>
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
          <ScInput sx={{mb: 1}} disabled fullWidth label={m.email} value={connectedUser.email} />
          <Box
            sx={{
              mt: 2,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            <ScButton loading={_acceptToken.loading} type="submit" color="primary" variant="contained">
              {m.addCompany}
            </ScButton>
          </Box>
        </form>
      </LoginPanel>
      <HelpContactInfo />
    </Page>
  )
}
