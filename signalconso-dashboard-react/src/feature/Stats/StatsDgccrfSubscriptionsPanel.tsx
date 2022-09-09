import {useTheme} from '@mui/material'
import {SimplifiedAsyncLineChart} from 'shared/Chart/LineChartWrappers'
import {Txt} from '../../alexlibs/mui-extension'
import {useLogin} from '../../core/context/LoginContext'
import {useI18n} from '../../core/i18n'
import {Panel, PanelBody, PanelHead} from '../../shared/Panel'

export const StatsDgccrfSubscriptionsPanel = () => {
  const {apiSdk: api} = useLogin()
  const {m} = useI18n()
  const theme = useTheme()
  const ticks = 12
  return (
    <Panel>
      <PanelHead>{m.dgccrfActions}</PanelHead>
      <PanelBody>
        <Txt color="hint" gutterBottom block dangerouslySetInnerHTML={{__html: m.dgccrfActionsDesc}} />
        <SimplifiedAsyncLineChart
          curves={[
            {
              label: m.dgccrfSubscriptionsCurve,
              loadData: () => api.secured.stats.getDgccrfSubscriptionsCurve({ticks}),
            },
            {
              label: m.dgccrfControlsCurve,
              loadData: () => api.secured.stats.getDgccrfControlsCurve({ticks}),
            },
          ]}
        />
      </PanelBody>
    </Panel>
  )
}
