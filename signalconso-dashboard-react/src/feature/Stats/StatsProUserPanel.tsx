import {useTheme} from '@mui/material'
import {SimplifiedAsyncLineChart} from 'shared/Chart/LineChartWrappers'
import {Txt} from '../../alexlibs/mui-extension'
import {useLogin} from '../../core/context/LoginContext'
import {useI18n} from '../../core/i18n'
import {Panel, PanelBody, PanelHead} from '../../shared/Panel'

export const StatsProUserPanel = () => {
  const {apiSdk: api} = useLogin()
  const {m} = useI18n()
  const theme = useTheme()
  const ticks = 12
  return (
    <Panel>
      <PanelHead>{m.reportsOnFisrtProActivationAccount}</PanelHead>
      <PanelBody>
        <Txt color="hint" gutterBottom block dangerouslySetInnerHTML={{__html: m.reportsProUserDesc}} />
        <SimplifiedAsyncLineChart
          curves={[
            {
              label: m.reportsProTransmittedCount,
              loadData: () => api.secured.stats.getProReportTransmittedStat({ticks}),
            },
            {
              label: m.proFirstAccountActivation,
              loadData: () => api.secured.stats.getReportedInactiveProAccountRate({ticks}),
            },
          ]}
        />
      </PanelBody>
    </Panel>
  )
}
