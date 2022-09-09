import * as React from 'react'
import {ReactNode, useMemo, useState} from 'react'
import {alpha, Box, Theme, Tooltip} from '@mui/material'
import withStyles from '@mui/styles/withStyles'
import {useTimeout} from '../../alexlibs/react-hooks-lib'
import {mapFor} from '../../alexlibs/ts-utils'
import {Txt} from '../../alexlibs/mui-extension'
import {useI18n} from '../../core/i18n'
import {styleUtils} from '../../core/theme'
import {makeSx} from '../../alexlibs/mui-extension'

export interface HorizontalBarChartData {
  label: ReactNode
  value: number
  color?: string
}

interface Props {
  data?: HorizontalBarChartData[]
  grid?: boolean
  width?: number
}

const sx = makeSx({
  item: {
    display: 'flex',
    my: 0.5,
    mx: 0,
  },
  label: {
    alignSelf: 'flex-end',
    textAlign: 'right',
    p: 0,
    pr: 2,
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
})

export const HorizontalBarChart = ({data, grid, width = 200}: Props) => {
  const {m} = useI18n()
  const maxValue = useMemo(() => data && Math.max(...data.map(_ => _.value)), [data])
  const sumValue = useMemo(() => data && data.reduce((sum, _) => _.value + sum, 0), [data])
  const [appeared, setAppeared] = useState<boolean>(false)
  const gridAxis = 4
  const {formatLargeNumber} = useI18n()
  useTimeout(() => setAppeared(true), 0)

  return (
    <Box
      sx={{
        overflow: 'hidden',
      }}
    >
      {data && maxValue && sumValue ? (
        data.map((item, i) => {
          const percentOfMax = (item.value / maxValue) * 100
          const percentOfAll = (item.value / sumValue) * 100
          return (
            <Box key={i} sx={sx.item}>
              <Box sx={sx.label} style={{width: width, minWidth: width}}>
                {item.label}
              </Box>
              <LightTooltip
                title={
                  <>
                    <Txt size="big" block bold>
                      {item.label}
                    </Txt>
                    <div style={{textAlign: 'right'}}>
                      <Txt size="title" color="primary" block>
                        {formatLargeNumber(item.value)}
                      </Txt>
                      <Txt size="big" color="hint" block>
                        {Math.ceil(percentOfAll)}%
                      </Txt>
                    </div>
                  </>
                }
              >
                <Box
                  sx={{
                    py: 0.25,
                    px: 0,
                    flex: 1,
                    transition: t => t.transitions.create('background'),
                    '&:hover': {
                      background: t => alpha(t.palette.primary.main, 0.1),
                    },
                  }}
                >
                  <Box
                    sx={{
                      fontSize: t => styleUtils(t).fontSize.small,
                      fontWeight: t => t.typography.fontWeightBold,
                      transition: t => t.transitions.create('width', {duration: 1000}),
                      width: 0,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'flex-end',
                      minHeight: 24,
                      borderBottom: t => `4px solid ${t.palette.primary.main}`,
                      color: t => t.palette.primary.main,
                    }}
                    style={{width: appeared ? `calc(${percentOfMax * 0.9}%)` : 0, color: item.color, borderColor: item.color}}
                  >
                    {percentOfMax > 5 && <div>{formatLargeNumber(item.value)}</div>}
                  </Box>
                </Box>
              </LightTooltip>
            </Box>
          )
        })
      ) : (
        <Box sx={sx.label}> {m.noDataAtm} </Box>
      )}
      {grid && data && data.length > 0 && (
        <Box sx={sx.item}>
          <Box sx={sx.label} style={{width: width, minWidth: width}} />
          <div style={{position: 'relative', width: '100%'}}>
            {mapFor(gridAxis + 1, i => (
              <Box
                key={i}
                sx={{
                  width: '1px',
                  height: 1000,
                  background: t => t.palette.divider,
                  position: 'absolute',
                  bottom: 0,
                  right: 0,
                  left: `calc(${i * (100 / gridAxis)}% - 1px)`,
                }}
              />
            ))}
          </div>
        </Box>
      )}
    </Box>
  )
}

const LightTooltip = withStyles((theme: Theme) => ({
  tooltip: {
    backgroundColor: theme.palette.common.white,
    color: 'rgba(0, 0, 0, 0.87)',
    boxShadow: theme.shadows[1],
    fontSize: 11,
  },
}))(Tooltip)
