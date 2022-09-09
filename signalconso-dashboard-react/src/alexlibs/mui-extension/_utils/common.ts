import {SxProps} from '@mui/system'
import {Theme} from '@mui/material'

export const makeSx = <T>(_: {[key in keyof T]: SxProps<Theme>}) => _
