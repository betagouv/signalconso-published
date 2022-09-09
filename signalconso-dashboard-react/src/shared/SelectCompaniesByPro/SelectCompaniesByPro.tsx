import * as React from 'react'
import {useEffect, useState} from 'react'
import {Icon, InputAdornment, TextField, TextFieldProps} from '@mui/material'
import {SelectCompaniesByProMenu} from './SelectCompaniesByProMenu'
import {useI18n} from '../../core/i18n'

import {CompanyWithAccessLevel} from '../../core/client/company/Company'
import {ScOption} from 'core/helper/ScOption'

export interface SelectDepartmentsProps extends Omit<TextFieldProps, 'onChange'> {
  accessibleCompanies: CompanyWithAccessLevel[]
  placeholder?: string
  label?: string
  values?: string[]
  readonly?: boolean
  onChange: (_: string[]) => void
  fullWidth?: boolean
}

export const SelectCompaniesByPro = ({
  accessibleCompanies,
  placeholder,
  label,
  values,
  readonly,
  onChange,
  fullWidth,
  ...props
}: SelectDepartmentsProps) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)
  let $input: HTMLElement | undefined = undefined
  const {m} = useI18n()
  const [innerValues, setInnerValues] = useState<string[]>()

  useEffect(() => {
    setInnerValues(values ?? [])
  }, [values])

  const open = (event: any) => setAnchorEl(event.currentTarget)

  const close = () => {
    setAnchorEl(null)
  }

  return (
    <>
      <TextField
        {...props}
        fullWidth={fullWidth}
        variant="outlined"
        margin="dense"
        size="small"
        placeholder={placeholder}
        label={label ?? m.siret}
        onClick={open}
        value={ScOption.from(innerValues)
          .filter(_ => _.length > 0)
          .map(_ => `(${_.length}) ${_.join(', ')}`)
          .getOrElse('')}
        disabled={readonly}
        inputRef={(n: any) => ($input = n ?? undefined)}
        InputProps={{
          readOnly: true,
          endAdornment: (
            <InputAdornment position="end">
              <Icon
                sx={{
                  height: 20,
                  color: t => t.palette.text.secondary,
                  verticalAlign: 'top',
                }}
              >
                arrow_drop_down
              </Icon>
            </InputAdornment>
          ),
        }}
      />
      <SelectCompaniesByProMenu
        accessibleCompanies={accessibleCompanies}
        anchorEl={anchorEl}
        open={!!anchorEl}
        onClose={close}
        onChange={_ => {
          onChange(_)
          setInnerValues(_)
        }}
        initialValues={values}
      />
    </>
  )
}
