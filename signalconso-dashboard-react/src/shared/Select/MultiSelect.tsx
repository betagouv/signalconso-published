import React, {CSSProperties, ReactElement, useMemo} from 'react'
import {Checkbox, FormControl, InputLabel, MenuItem, Select, SelectProps} from '@mui/material'
import {ScMenuItemProps} from '../MenuItem/ScMenuItem'
import {useI18n} from '../../core/i18n'
import {stopPropagation} from '../../core/helper'

interface ScMultiSelectProps<T extends E[], E extends string> extends Omit<SelectProps<T>, 'multiple' | 'onChange'> {
  label?: string
  children?: React.ReactNode
  className?: string
  style?: CSSProperties
  small?: boolean
  withSelectAll?: boolean
  onChange?: (_: T) => void
}

const _ScMultiSelect = <T extends E[], E extends string>(
  {
    id: argId,
    label,
    children,
    className,
    small,
    fullWidth,
    style,
    withSelectAll,
    onChange,
    ...selectProps
  }: ScMultiSelectProps<T, E>,
  ref: any,
) => {
  const id: string = useMemo(() => argId ?? 'sc-multi-select-' + Math.floor(Math.random() * 10000), [argId])
  const {m} = useI18n()

  const allValues = useMemo(() => {
    const values: E[] = []
    React.Children.forEach(children as ReactElement<ScMenuItemProps<E>>[], _ => {
      if (_.props.value) values.push(_.props.value)
    })
    return values
  }, [children])

  const someValuesSelected = !!allValues.find(_ => selectProps.value?.includes(_))

  const allChecked = allValues.length === selectProps.value?.length

  const toggleAll = () => {
    if (selectProps.value?.length === 0) {
      onChange?.(allValues as T)
    } else {
      onChange?.([] as unknown as T)
    }
  }

  return (
    <FormControl fullWidth={fullWidth} size="small" margin="dense" variant="outlined" className={className} style={style}>
      <InputLabel htmlFor={id} id={id + '-label'}>
        {label}
      </InputLabel>
      <Select
        {...selectProps}
        onChange={e => onChange?.(e.target.value as T)}
        inputRef={ref}
        multiple={true}
        labelId={id + '-label'}
        id={id}
      >
        {withSelectAll && (
          <MenuItem
            divider
            sx={{fontWeight: t => t.typography.fontWeightBold}}
            onClickCapture={e => stopPropagation(toggleAll)(e)}
          >
            <Checkbox
              checked={allChecked}
              indeterminate={!allChecked && someValuesSelected}
              size="small"
              style={{paddingLeft: 0, paddingTop: 0, paddingBottom: 0}}
            />
            <span>{m.selectAll}</span>
          </MenuItem>
        )}
        {React.Children.map(children as ReactElement<ScMenuItemProps<E>>[], c =>
          React.cloneElement(c, {
            checked: selectProps.value?.includes(c.props.value!),
          }),
        )}
      </Select>
    </FormControl>
  )
}

/**
 * Workaround because forwardRef break the generic type of ScSelect.
 */
export const ScMultiSelect = React.forwardRef(_ScMultiSelect) as <T extends E[], E extends string>(
  props: ScMultiSelectProps<T, E> & {ref?: React.ForwardedRef<any>},
) => ReturnType<typeof _ScMultiSelect>
