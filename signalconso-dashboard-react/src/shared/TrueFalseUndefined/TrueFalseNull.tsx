import {BoxProps, Theme, ToggleButton, ToggleButtonGroup} from '@mui/material'
import React, {forwardRef, ReactNode, useMemo} from 'react'
import {useI18n} from '../../core/i18n'
import {SxProps} from '@mui/system'

export interface TrueFalseNullProps extends Omit<BoxProps, 'onChange'> {
  gutter?: boolean
  value: boolean | null
  onChange: (_: boolean | null) => void
  label?: {
    true?: ReactNode
    false?: ReactNode
    undefined?: ReactNode
  }
}

const buttonStyle: SxProps<Theme> = {
  textTransform: 'none',
  // paddingTop: .5,
  // paddingBottom: .5,
  paddingRight: 1.5,
  paddingLeft: 1.5,
  whiteSpace: 'nowrap',
}

export const TrueFalseNull = forwardRef(({gutter, value, onChange, label, sx, ...props}: TrueFalseNullProps, ref: any) => {
  const {m} = useI18n()
  const parsedValue = useMemo(() => {
    if ([true, 'true'].includes(value as any)) return 'true'
    if ([false, 'false'].includes(value as any)) return 'false'
    return ''
  }, [value])

  return (
    <ToggleButtonGroup
      {...props}
      exclusive
      sx={{
        ...(gutter
          ? {
              mt: 1,
              mb: 0.5,
              display: 'block',
            }
          : {}),
        ...sx,
      }}
      size="small"
      color="primary"
      ref={ref}
      style={{flexDirection: 'row'}}
      value={parsedValue}
      onChange={(e, value: string | null) => {
        const valueAsBoolean: boolean | null = value === 'true' ? true : value === 'false' ? false : null
        onChange(valueAsBoolean)
      }}
    >
      <ToggleButton sx={buttonStyle} value="true">
        {label?.true ?? m.yes}
      </ToggleButton>
      <ToggleButton sx={buttonStyle} value="false">
        {label?.false ?? m.no}
      </ToggleButton>
      <ToggleButton sx={buttonStyle} value="">
        {label?.undefined ?? m.indifferent}
      </ToggleButton>
    </ToggleButtonGroup>
  )
})
