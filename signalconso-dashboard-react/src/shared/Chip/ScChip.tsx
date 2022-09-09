import React, {forwardRef} from 'react'
import {Chip as MatChip, ChipProps as MatChipProps} from '@mui/material'
import {combineSx, sxUtils} from '../../core/theme'

interface ChipProps extends MatChipProps {}

export const ScChip = forwardRef(({variant = 'outlined', sx = {}, ...props}: ChipProps, ref: any) => {
  return <MatChip ref={ref} variant={variant} {...props} sx={combineSx(sxUtils.fontNormal, sx)} />
})
