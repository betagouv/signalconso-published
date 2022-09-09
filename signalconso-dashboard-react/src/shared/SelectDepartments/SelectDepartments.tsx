import * as React from 'react'
import {forwardRef, useEffect} from 'react'
import {Icon, InputAdornment, TextField, TextFieldProps} from '@mui/material'
import {useSetState, UseSetState} from '../../alexlibs/react-hooks-lib'
import {SelectDepartmentsMenu} from './SelectDepartmentsMenu'

export interface SelectDepartmentsProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  placeholder?: string
  selectAllLabel?: string
  value?: string[]
  readonly?: boolean
  onChange: (_: string[]) => void
  fullWidth?: boolean
  disabled?: boolean
  label?: string
}

export const SelectDepartments = forwardRef(
  ({value, readonly, onChange, selectAllLabel, label, disabled, ...props}: SelectDepartmentsProps, ref: any) => {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)
    let $input: HTMLElement | undefined = undefined
    const indexValues: UseSetState<string> = useSetState<string>()

    useEffect(() => {
      indexValues.reset(value)
    }, [value])

    const open = (event: any) => {
      if (!disabled) setAnchorEl(event.currentTarget)
    }

    const close = () => {
      setAnchorEl(null)
    }

    return (
      <>
        <TextField
          {...props}
          ref={ref}
          variant="outlined"
          margin="dense"
          size="small"
          onClick={open}
          value={indexValues.toArray().join(', ') ?? ''}
          disabled={readonly}
          label={label}
          inputRef={(n: any) => ($input = n ?? undefined)}
          InputProps={{
            disabled,
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
        <SelectDepartmentsMenu
          selectAllLabel={selectAllLabel}
          anchorEl={anchorEl}
          open={!!anchorEl}
          onClose={close}
          onChange={onChange}
          initialValues={value}
        />
      </>
    )
  },
)
