import React, {ReactElement} from 'react'
import {useI18n} from '../../core/i18n'
import {Autocomplete} from '@mui/material'
import {ScDialog} from '../Confirm/ScDialog'
import {ScInput} from '../Input/ScInput'
import {useEffectFn} from '../../alexlibs/react-hooks-lib'

interface Props<T> {
  value?: T
  children: ReactElement<any>
  inputLabel: string
  title: string
  defaultValue?: T
  getOptionLabel: (_: T) => string
  onChange: (_?: T) => void
  options?: T[]
}

export const AutocompleteDialog = <T extends unknown>({
  children,
  inputLabel,
  value,
  title,
  defaultValue,
  getOptionLabel,
  onChange,
  options,
}: Props<T>) => {
  const {m} = useI18n()
  const [innerValue, setInnerValue] = React.useState<T | undefined>(defaultValue)

  useEffectFn(value, setInnerValue)

  return (
    <ScDialog
      PaperProps={{style: {position: 'static'}}}
      maxWidth="xs"
      title={title}
      content={_ => (
        <>
          <Autocomplete
            multiple={false}
            value={innerValue}
            defaultValue={defaultValue}
            sx={{
              mb: 1.5,
              minWidth: 280,
              width: 300,
            }}
            onChange={(event, newInputValue) => {
              setInnerValue(newInputValue ?? undefined)
            }}
            options={options ?? []}
            getOptionLabel={getOptionLabel}
            renderInput={params => <ScInput autoFocus {...params} label={inputLabel} />}
          />
        </>
      )}
      onConfirm={(_, close) => {
        onChange(innerValue)
        close()
      }}
      confirmLabel={m.edit}
    >
      {children}
    </ScDialog>
  )
}
