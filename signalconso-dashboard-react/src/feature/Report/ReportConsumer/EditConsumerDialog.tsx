import React, {ReactElement} from 'react'
import {useI18n} from '../../../core/i18n'
import {ScInput} from '../../../shared/Input/ScInput'
import {Controller, useForm} from 'react-hook-form'
import {regexp} from '../../../core/helper/regexp'
import {Checkbox, FormControlLabel} from '@mui/material'
import {ScDialog} from '../../../shared/Confirm/ScDialog'
import {Report, ReportConsumerUpdate} from '../../../core/client/report/Report'
import {emptyStringToUndefined} from '../../../core/helper/utils'

interface Props {
  report: Report
  children: ReactElement<any>
  onChange: (userForm: ReportConsumerUpdate) => unknown
}

// Inside this component, we replace empty string by undefined
type FormData = Omit<ReportConsumerUpdate, 'consumerReferenceNumber'> & {
  consumerReferenceNumber: string
}

function buildFormData(report: Report): FormData {
  const {firstName, lastName, email, contactAgreement, consumerReferenceNumber} = report
  return {firstName, lastName, email, contactAgreement, consumerReferenceNumber: consumerReferenceNumber ?? ''}
}

function translateFormData({consumerReferenceNumber, ...rest}: FormData): ReportConsumerUpdate {
  return {consumerReferenceNumber: emptyStringToUndefined(consumerReferenceNumber), ...rest}
}

export const EditConsumerDialog = ({report, onChange, children}: Props) => {
  const {m} = useI18n()
  const {
    register,
    getValues,
    control,
    formState: {errors},
  } = useForm<FormData>({mode: 'onChange', defaultValues: buildFormData(report)})
  return (
    <ScDialog
      PaperProps={{sx: {overflow: 'visible'}}}
      title={m.editConsumer}
      maxWidth="xs"
      confirmLabel={m.edit}
      onConfirm={(e, close) => {
        onChange(translateFormData(getValues()))
        close()
      }}
      content={
        <>
          <ScInput
            fullWidth
            autoFocus
            label={m.firstName}
            error={!!errors.firstName}
            helperText={errors.firstName?.message ?? ' '}
            {...register('firstName', {
              required: {value: true, message: m.required},
            })}
          />

          <ScInput
            fullWidth
            label={m.lastName}
            error={!!errors.lastName}
            helperText={errors.lastName?.message ?? ' '}
            {...register('lastName', {
              required: {value: true, message: m.required},
            })}
          />

          <ScInput
            fullWidth
            label={m.email}
            error={!!errors.email}
            helperText={errors.email?.message ?? ' '}
            {...register('email', {
              pattern: {value: regexp.email, message: m.invalidEmail},
              required: {value: true, message: m.required},
            })}
          />

          <ScInput
            fullWidth
            label={m.reportConsumerReferenceNumber}
            error={!!errors.consumerReferenceNumber}
            helperText={errors.consumerReferenceNumber?.message ?? ' '}
            {...register('consumerReferenceNumber')}
          />

          <Controller
            name="contactAgreement"
            control={control}
            render={({field}) => (
              <FormControlLabel
                control={<Checkbox {...field} checked={field.value} disabled={report.contactAgreement} />}
                label={m.contactAgreement}
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
