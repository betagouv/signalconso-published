import {ScInput} from '../../shared/Input/ScInput'
import React, {ReactElement} from 'react'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {useI18n} from '../../core/i18n'
import {Controller, useForm} from 'react-hook-form'
import {Checkbox, FormControlLabel} from '@mui/material'
import {Alert} from '../../alexlibs/mui-extension'
import {Address} from '../../core/model'

interface EditAddressDialogProps {
  children: ReactElement<any>
  address: Address
  onChange: (_: Form) => Promise<any>
  onChangeError?: string
}

interface Form {
  number: string
  street: string
  addressSupplement?: string
  postalCode: string
  city: string
  activationDocumentRequired?: boolean
}

export const EditAddressDialog = ({address, children, onChange, onChangeError}: EditAddressDialogProps) => {
  const {m} = useI18n()

  const {
    register,
    handleSubmit,
    getValues,
    control,
    formState: {errors, isValid},
  } = useForm<Form>({mode: 'onChange'})

  return (
    <ScDialog
      maxWidth="xs"
      confirmDisabled={!isValid}
      title={m.editAddress}
      onConfirm={(event, close) => {
        onChange(getValues())
        close()
      }}
      content={close => (
        <>
          {onChangeError && <Alert type="error">{onChangeError}</Alert>}
          <div style={{display: 'flex'}}>
            <ScInput
              defaultValue={address.number}
              error={!!errors.number}
              helperText={errors.number?.message ?? ' '}
              sx={{mr: 1, maxWidth: 80}}
              placeholder={m.numberShort}
              {...register('number', {
                required: {value: true, message: m.required},
              })}
            />
            <ScInput
              defaultValue={address.street}
              error={!!errors.street}
              helperText={errors.street?.message ?? ' '}
              fullWidth
              placeholder={m.street}
              {...register('street', {
                required: {value: true, message: m.required},
              })}
            />
          </div>
          <ScInput
            defaultValue={address.addressSupplement}
            error={!!errors.addressSupplement}
            helperText={errors.addressSupplement?.message ?? ' '}
            fullWidth
            placeholder={m.addressSupplement}
            {...register('addressSupplement')}
          />
          <div style={{display: 'flex'}}>
            <ScInput
              defaultValue={address.postalCode}
              error={!!errors.postalCode}
              helperText={errors.postalCode?.message ?? ' '}
              sx={{mr: 1, maxWidth: 100}}
              fullWidth
              placeholder={m.postalCode}
              {...register('postalCode', {
                required: {value: true, message: m.required},
              })}
            />
            <ScInput
              defaultValue={address.city}
              error={!!errors.city}
              helperText={errors.city?.message ?? ' '}
              fullWidth
              placeholder={m.city}
              {...register('city', {
                required: {value: true, message: m.required},
              })}
            />
          </div>
          <Controller
            name="activationDocumentRequired"
            control={control}
            render={({field}) => (
              <FormControlLabel control={<Checkbox {...field} checked={field.value} />} label={m.activationDocumentRequired} />
            )}
          />
        </>
      )}
      confirmLabel={m.edit}
    >
      {children}
    </ScDialog>
  )
}
