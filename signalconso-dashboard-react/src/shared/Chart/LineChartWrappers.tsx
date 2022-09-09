import {Skeleton} from '@mui/material'
import {useI18n} from 'core/i18n'
import {ScLineChart} from './ScLineChart'
import {useEffect, useState} from 'react'
import {CountByDate} from '../../core/client/stats/Stats'

export const toPercentage = (numerator: CountByDate[], denominator: CountByDate[]): CountByDate[] => {
  return numerator.map<CountByDate>((k, i) => ({
    date: k.date,
    count: denominator[i] && denominator[i].count > 0 ? Math.min(Math.round((k.count / denominator[i].count) * 100), 100) : 0,
  }))
}

export interface CurveDefinition {
  label: string
  data: CountByDate[]
}

interface LineChartOrPlaceholderProps {
  curves?: CurveDefinition[]
  hideLabelToggle?: boolean
}

// Displays the data, or a loading placeholder if the data is undefined
// For complex needs, when you need to fully control the loading of the data in the parent component
export const LineChartOrPlaceholder = ({curves, ...rest}: LineChartOrPlaceholderProps) => {
  const height = 300
  return curves ? (
    <ScLineChart
      {...{height}}
      disableAnimation={true}
      curves={curves.map(({label, data}, idx) => {
        return {
          key: idx.toString(),
          label,
          curve: data,
        }
      })}
      {...rest}
    />
  ) : (
    <Skeleton variant="rectangular" height={height} width="100%" sx={{borderRadius: '8px'}} />
  )
}

type AsyncLineChartProps = {
  loadCurves: () => Promise<CurveDefinition[]>
  hideLabelToggle?: boolean
}

// You provide one loading function that returns all the data
// Useful when you need to compute some curve based on the data from some other curve
export const AsyncLineChart = ({loadCurves, ...rest}: AsyncLineChartProps) => {
  const [curves, setCurves] = useState<CurveDefinition[] | undefined>(undefined)
  useEffect(() => {
    loadCurves().then(setCurves)
  }, [])
  return <LineChartOrPlaceholder curves={curves} {...rest} />
}

interface SimplifiedAsyncLineChartProps {
  curves: {
    label: string
    loadData: () => Promise<CountByDate[]>
  }[]
  hideLabelToggle?: boolean
}

// You provide a loading function for each line
// Easiest to use, but sometimes not always flexible enough
export const SimplifiedAsyncLineChart = ({curves, ...rest}: SimplifiedAsyncLineChartProps) => {
  const loadCurves = () =>
    Promise.all(
      curves.map(({loadData, ...rest}) => {
        return loadData().then(data => ({data, ...rest}))
      }),
    )
  return <AsyncLineChart {...{loadCurves}} {...rest} />
}
