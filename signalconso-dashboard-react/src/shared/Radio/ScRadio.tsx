import React, {forwardRef} from 'react'
import {FormControlLabel, Radio} from '@mui/material'

export interface ScProps {
  // label?: string
}

/** @deprecated
 * Was designed to be a wrapper working with react-hook-form
 * Should maybe removed since it causes Material-ui warning about uncontrolled component
 * Prefere use <Controller> from react-hook-form
 */
export const ScRadio = forwardRef(({label, ...props}: any, ref) => {
  return <FormControlLabel label={label} value={props.value} inputRef={ref} {...props} control={<Radio />} />
})
