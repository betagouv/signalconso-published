import {ScSelect} from '../Select/Select'
import {MenuItem} from '@mui/material'
import {useMemo} from 'react'
import {useI18n} from '../../core/i18n'

interface Props {
  value: number
  onChange: (_: number) => void
}

export const SelectMonth = ({value, onChange}: Props) => {
  const currentMonth = useMemo(() => new Date().getMonth(), [])
  const currentYear = useMemo(() => new Date().getFullYear(), [])
  const {m} = useI18n()

  return (
    <ScSelect value={value} onChange={x => onChange(x.target.value as number)} style={{margin: 0}}>
      {Object.entries(m.month_).map(([index, label]) => (
        <MenuItem key={index} value={+index - 1}>
          {label} {+index - 1 > currentMonth + 1 ? currentYear - 1 : currentYear}
        </MenuItem>
      ))}
    </ScSelect>
  )
}
