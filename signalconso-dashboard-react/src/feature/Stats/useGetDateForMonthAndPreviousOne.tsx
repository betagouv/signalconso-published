import {useMemo} from 'react'
import {endOfMonth, startOfMonth, subMonths, subYears} from 'date-fns'

export interface UseGetDateForMonthAndPreviousOneProps {
  current: {start: Date; end: Date}
  lastMonth: {start: Date; end: Date}
}

export const useGetDateForMonthAndPreviousOne = (selectedMonth: number): UseGetDateForMonthAndPreviousOneProps => {
  const currentMonth = useMemo(() => new Date().getMonth(), [])
  return useMemo(() => {
    const selectedDate = new Date(new Date().setMonth(selectedMonth))
    const selectedDateHandlingYear = selectedMonth > currentMonth + 1 ? subYears(selectedDate, 1) : selectedDate
    return {
      current: {
        start: startOfMonth(selectedDateHandlingYear),
        end: endOfMonth(selectedDateHandlingYear),
      },
      lastMonth: {
        start: startOfMonth(subMonths(selectedDateHandlingYear, 1)),
        end: endOfMonth(subMonths(selectedDateHandlingYear, 1)),
      },
    }
  }, [selectedMonth])
}
