import {alpha, Button, ButtonGroup, Icon, IconButton} from '@mui/material'
import {useLogin} from 'core/context/LoginContext'
import {useI18n} from 'core/i18n'
import {siteMap} from 'core/siteMap'
import {useEffect, useState} from 'react'
import {NavLink} from 'react-router-dom'
import {CurveDefinition, LineChartOrPlaceholder} from 'shared/Chart/LineChartWrappers'
import {Panel, PanelBody, PanelHead} from 'shared/Panel'
import {CompanyWithReportsCount} from '../../core/client/company/Company'
import {Period} from '../../core/client/stats/Stats'
import {Id, ReportStatus} from '../../core/model'

const periods: Period[] = ['Day', 'Month']

const ticks = 7

export const CompanyChartPanel = ({companyId, company}: {company: CompanyWithReportsCount; companyId: Id}) => {
  const {apiSdk} = useLogin()
  const {m, formatLargeNumber} = useI18n()
  const [reportsCurvePeriod, setReportsCurvePeriod] = useState<Period>('Month')
  const companyIds = [companyId]
  const [curves, setCurves] = useState<CurveDefinition[] | undefined>()

  useEffect(() => {
    async function inner() {
      setCurves(undefined)
      const [reports, responses] = await Promise.all([
        apiSdk.secured.stats.getReportCountCurve({
          companyIds,
          ticks,
          tickDuration: reportsCurvePeriod,
        }),
        apiSdk.secured.stats.getReportCountCurve({
          companyIds,
          status: [ReportStatus.PromesseAction, ReportStatus.Infonde, ReportStatus.MalAttribue],
          ticks,
          tickDuration: reportsCurvePeriod,
        }),
      ])
      setCurves([
        {
          label: m.reportsCount,
          data: reports,
        },
        {
          label: m.responsesCount,
          data: responses,
        },
      ])
    }
    inner()
  }, [reportsCurvePeriod])

  return (
    <Panel>
      <PanelHead
        action={
          <ButtonGroup color="primary">
            {periods.map(p => (
              <Button
                key={p}
                sx={p === reportsCurvePeriod ? {background: t => alpha(t.palette.primary.main, 0.14)} : {}}
                onClick={() => setReportsCurvePeriod(p)}
              >
                {p === 'Day' ? m.day : m.month}
              </Button>
            ))}
          </ButtonGroup>
        }
      >
        <NavLink to={siteMap.logged.reports({companyIds})}>
          {company.count && formatLargeNumber(company.count)}
          &nbsp;
          {m.reports.toLocaleLowerCase()}
          <IconButton size={'small'} sx={{color: t => t.palette.text.secondary}}>
            <Icon>open_in_new</Icon>
          </IconButton>
        </NavLink>
      </PanelHead>
      <PanelBody>
        <LineChartOrPlaceholder hideLabelToggle={true} {...{curves}} />
      </PanelBody>
    </Panel>
  )
}
