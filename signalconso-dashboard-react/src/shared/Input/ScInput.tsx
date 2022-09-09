import {FilledInputProps, TextField, TextFieldProps} from '@mui/material'
import React from 'react'

export type ScInputProps = Omit<TextFieldProps, 'variant' | 'margin'> & {
  small?: boolean
  InputProps?: Partial<FilledInputProps>
}

export const ScInput = React.forwardRef(({small, ...props}: ScInputProps, ref) => {
  return <TextField {...props} size="small" variant="outlined" margin="dense" inputRef={ref} />
})
