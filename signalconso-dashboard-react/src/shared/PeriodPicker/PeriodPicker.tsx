import {Box, BoxProps} from '@mui/material'
import {addDays, subDays} from 'date-fns'
import {useI18n} from '../../core/i18n'
import {Datepicker} from '../Datepicker/Datepicker'

export interface DatepickerProps extends Omit<BoxProps, 'onChange'> {
  value?: [Date | undefined, Date | undefined]
  onChange: (_: [Date | undefined, Date | undefined]) => void
  label?: [string, string]
  fullWidth?: boolean
}

const datesAreInterverted = (start: Date, end: Date) => start.getTime() > end.getTime()

export const PeriodPicker = ({value = [undefined, undefined], onChange, label, fullWidth, sx, ...props}: DatepickerProps) => {
  const [start, end] = value ?? [undefined, undefined]
  const {m} = useI18n()

  const handleStartChange = (newStart?: Date) => {
    const newEnd = newStart && end && datesAreInterverted(newStart, end) ? addDays(newStart, 1) : end
    onChange([newStart, newEnd])
  }

  const handleEndChange = (newEnd?: Date) => {
    const newStart = start && newEnd && datesAreInterverted(start, newEnd) ? subDays(newEnd, 1) : start
    onChange([newStart, newEnd])
  }

  return (
    <Box
      {...props}
      sx={{
        display: 'flex',
        alignItems: 'center',
        ...(fullWidth && {width: '100%'}),
        ...sx,
      }}
    >
      <Datepicker
        label={label?.[0] ?? m.start}
        fullWidth={fullWidth}
        value={start}
        onChange={handleStartChange}
        sx={{marginRight: '-1px'}}
        InputProps={{
          sx: _ => ({
            borderBottomRightRadius: 0,
            borderTopRightRadius: 0,
          }),
        }}
        timeOfDay="startOfDay"
      />

      <Datepicker
        label={label?.[1] ?? m.end}
        fullWidth={fullWidth}
        value={end}
        onChange={handleEndChange}
        InputProps={{
          sx: _ => ({
            borderBottomLeftRadius: 0,
            borderTopLeftRadius: 0,
          }),
        }}
        timeOfDay="endOfDay"
      />
    </Box>
  )
}
