import {ScButton} from '../../shared/Button/Button'
import {Alert} from '../../alexlibs/mui-extension'
import React from 'react'
import {useI18n} from '../../core/i18n'
import {Controller, useForm} from 'react-hook-form'
import {ScInput} from '../../shared/Input/ScInput'
import {regexp} from '../../core/helper/regexp'
import {Enum} from '../../alexlibs/ts-utils'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {ScRadioGroup} from '../../shared/RadioGroup/RadioGroup'
import {ScRadioGroupItem} from '../../shared/RadioGroup/RadioGroupItem'
import {Txt} from '../../alexlibs/mui-extension'
import {CompanyAccessLevel} from '../../core/client/company-access/CompanyAccess'

interface Props {
  errorMessage?: string
  loading: boolean
  onCreate: (email: string, level: CompanyAccessLevel) => Promise<any>
}

interface Form {
  email: string
  level: CompanyAccessLevel
}

export const CompanyAccessCreateBtn = ({errorMessage, loading, onCreate}: Props) => {
  const {m} = useI18n()
  const {
    register,
    handleSubmit,
    control,
    formState: {errors, isValid},
    reset,
  } = useForm<Form>({mode: 'onChange'})
  return (
    <ScDialog
      maxWidth="xs"
      title={m.invitNewUser}
      confirmLabel={m.create}
      loading={loading}
      confirmDisabled={!isValid}
      onConfirm={async (event, close) => {
        await handleSubmit(({email, level}) => onCreate(email, level))()
        close()
        reset()
      }}
      content={
        <>
          {errorMessage && (
            <Alert dense type="error" deletable gutterBottom>
              {m.anErrorOccurred}
            </Alert>
          )}
          <ScInput
            error={!!errors.email}
            helperText={errors.email?.message ?? ' '}
            fullWidth
            label={m.email}
            {...register('email', {
              pattern: {value: regexp.email, message: m.invalidEmail},
              required: {value: true, message: m.required},
            })}
          />

          <Txt block gutterBottom size="big">
            {m.authorization}
          </Txt>
          <Controller
            name="level"
            rules={{required: {value: true, message: m.required}}}
            control={control}
            render={({field}) => (
              <ScRadioGroup error={!!errors.level} {...field}>
                {Enum.keys(CompanyAccessLevel).map(level => (
                  <ScRadioGroupItem
                    title={CompanyAccessLevel[level]}
                    description={m.companyAccessLevelDescription[CompanyAccessLevel[level]]}
                    value={level}
                    key={level}
                  />
                ))}
              </ScRadioGroup>
            )}
          />
        </>
      }
    >
      <ScButton loading={loading} icon="add" color="primary" variant="contained">
        {m.invite}
      </ScButton>
    </ScDialog>
  )
}
