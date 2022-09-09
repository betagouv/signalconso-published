import {Redirect, Route, Switch, useRouteMatch} from 'react-router-dom'
import {Page, PageTitle} from 'shared/Layout'
import {useI18n} from '../../core/i18n'
import {siteMap} from '../../core/siteMap'
import {PageTab, PageTabs} from '../../shared/Layout/Page/PageTabs'
import {DgccrfStats} from './DgccrfStats'
import {ProStats} from './ProStats'
import {ReportStats} from './ReportStats'

export const Stats = () => {
  const {path} = useRouteMatch()
  const {m} = useI18n()
  return (
    <Page>
      <PageTitle>{m.statsLandingPage}</PageTitle>
      <PageTabs>
        <PageTab to={siteMap.logged.reportStats} label={m.statsReports} />
        <PageTab to={siteMap.logged.proStats} label={m.statsPro} />
        <PageTab to={siteMap.logged.dgccrfStats} label={m.statsDgccrf} />
      </PageTabs>
      <Switch>
        <Redirect exact from={path} to={siteMap.logged.reportStats} />
        <Route path={siteMap.logged.reportStats} component={ReportStats} />
        <Route path={siteMap.logged.proStats} component={ProStats} />
        <Route path={siteMap.logged.dgccrfStats} component={DgccrfStats} />
      </Switch>
    </Page>
  )
}
