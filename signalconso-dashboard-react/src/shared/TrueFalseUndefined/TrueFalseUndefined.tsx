import {forwardRef} from 'react'
import {TrueFalseNull, TrueFalseNullProps} from './TrueFalseNull'
import {null2Undefined, undefined2Null} from '../../core/helper'

interface Props extends Omit<TrueFalseNullProps, 'value' | 'onChange'> {
  value: boolean | undefined
  onChange: (_: boolean | undefined) => void
}

// TrueFalseNull uses null as the empty value, as needed by react-form
// TrueFalseUndefined wraps it so we can use undefined instead
export const TrueFalseUndefined = forwardRef(({value, onChange, ...props}: Props, ref: any) => {
  return <TrueFalseNull value={undefined2Null(value)} onChange={_ => onChange(null2Undefined(_))} {...props} ref={ref} />
})
