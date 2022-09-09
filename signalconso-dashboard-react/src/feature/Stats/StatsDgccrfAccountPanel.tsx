import {Panel, PanelBody, PanelHead} from '../../shared/Panel'
import {useLogin} from '../../core/context/LoginContext'
import {useI18n} from '../../core/i18n'
import {Txt} from '../../alexlibs/mui-extension'
import {useTheme} from '@mui/material'
import {SimplifiedAsyncLineChart} from 'shared/Chart/LineChartWrappers'

export const StatsDgccrfAccountPanel = () => {
  const {apiSdk: api} = useLogin()
  const {m} = useI18n()
  const ticks = 12
  return (
    <Panel>
      <PanelHead>{m.dgccrfUser}</PanelHead>
      <PanelBody>
        <Txt color="hint" gutterBottom block dangerouslySetInnerHTML={{__html: m.dgccrfUserDesc}} />
        <SimplifiedAsyncLineChart
          curves={[
            {
              label: m.dgccrfCountActiveAccount,
              loadData: () => api.secured.stats.getActiveDgccrfAccountCurve({ticks}),
            },
            {
              label: m.dgccrfCountAccount,
              loadData: () => api.secured.stats.getDgccrfAccountCurve({ticks}),
            },
          ]}
        />
      </PanelBody>
    </Panel>
  )
}
