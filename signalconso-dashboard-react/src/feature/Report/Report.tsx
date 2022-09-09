import React, {useEffect, useMemo, useState} from 'react'
import {Page} from '../../shared/Layout'
import {useParams} from 'react-router'
import {useI18n} from '../../core/i18n'
import {Panel} from '../../shared/Panel'
import {useReportContext} from '../../core/context/ReportContext'
import {Box, Grid, Tab, Tabs, Tooltip} from '@mui/material'
import {useToast} from '../../core/toast'
import {ReportEvents} from './Event/ReportEvents'
import {ReportResponseComponent} from './ReportResponse'
import {ReportHeader} from './ReportHeader'
import {Btn} from '../../alexlibs/mui-extension'
import {ReportPostAction} from './ReportPostAction'
import {useLogin} from '../../core/context/LoginContext'
import {ReportConsumer} from './ReportConsumer/ReportConsumer'
import {ReportCompany} from './ReportCompany/ReportCompany'
import {ReportDescription} from './ReportDescription'
import {useEventContext} from '../../core/context/EventContext'
import {useEffectFn} from '../../alexlibs/react-hooks-lib'
import {map} from '../../alexlibs/ts-utils'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {EventActionValues, EventType, ReportEvent} from '../../core/client/event/Event'
import {FileOrigin} from '../../core/client/file/UploadedFile'
import {Report} from '../../core/client/report/Report'
import {Id} from '../../core/model'

const CONSO: EventType = 'CONSO'

export const creationReportEvent = (report: Report): ReportEvent =>
  Object.freeze({
    data: {
      id: 'dummy',
      details: {} as any,
      reportId: report.id,
      eventType: CONSO,
      creationDate: report.creationDate,
      action: EventActionValues.Creation,
    },
  })

export const ReportComponent = () => {
  const {id} = useParams<{id: Id}>()
  const {m} = useI18n()
  const _report = useReportContext()
  const _event = useEventContext()
  const {connectedUser} = useLogin()
  const {toastError} = useToast()
  const [activeTab, setActiveTab] = useState(0)
  const response = useMemo(
    () => _event.reportEvents.entity?.find(_ => _.data.action === EventActionValues.ReportProResponse),
    [_event.reportEvents],
  )

  useEffect(() => {
    _report.get.clearCache()
    _report.get.fetch({}, id).then(({report}) => {
      if (report.companySiret) _event.companyEvents.fetch({}, report.companySiret)
    })
    _event.reportEvents.fetch({}, id)
  }, [])

  useEffectFn(_report.get.error, toastError)
  useEffectFn(_report.remove.error, toastError)
  useEffectFn(_report.updateCompany.error, toastError)
  useEffectFn(_event.companyEvents.error, toastError)
  useEffectFn(_event.reportEvents.error, toastError)

  const downloadReport = (reportId: Id) => _report.download.fetch({}, [reportId])

  return (
    <Page loading={_report.get.loading}>
      {map(_report.get.entity?.report, report => (
        <>
          <ReportHeader elevated report={report} hideSiret>
            <Box sx={{whiteSpace: 'nowrap'}}>
              {connectedUser.isDGCCRF && (
                <ReportPostAction
                  actionType={EventActionValues.Control}
                  label={m.markDgccrfControlDone}
                  report={report}
                  onAdd={() => _event.reportEvents.fetch({force: true, clean: false}, id)}
                >
                  <Tooltip title={m.markDgccrfControlDone}>
                    <Btn color="primary" icon="add_comment">
                      {m.dgccrfControlDone}
                    </Btn>
                  </Tooltip>
                </ReportPostAction>
              )}

              <ReportPostAction
                actionType={EventActionValues.Comment}
                label={m.addDgccrfComment}
                report={report}
                onAdd={() => _event.reportEvents.fetch({force: true, clean: false}, id)}
              >
                <Tooltip title={m.addDgccrfComment}>
                  <Btn color="primary" icon="add_comment">
                    {m.comment}
                  </Btn>
                </Tooltip>
              </ReportPostAction>

              <Btn color="primary" icon="download" loading={_report.download.loading} onClick={() => downloadReport(report.id)}>
                {m.download}
              </Btn>

              {connectedUser.isAdmin && (
                <ScDialog
                  title={m.removeAsk}
                  content={m.removeReportDesc(report.companySiret ?? '')}
                  onConfirm={(event, close) =>
                    _report.remove
                      .fetch({}, report.id)
                      .then(() => window.history.back())
                      .finally(close)
                  }
                >
                  <Btn loading={_report.remove.loading} sx={{color: t => t.palette.error.main}} icon="delete">
                    {m.delete}
                  </Btn>
                </ScDialog>
              )}
            </Box>
          </ReportHeader>

          <Grid container spacing={2} alignItems="stretch">
            <Grid item xs={12} sm={6}>
              <ReportConsumer report={report} canEdit={connectedUser.isAdmin} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <ReportCompany report={report} canEdit={connectedUser.isAdmin} />
            </Grid>
          </Grid>

          <ReportDescription report={report} files={_report.get.entity?.files} />

          <Panel loading={_event.reportEvents.loading}>
            <>
              <Tabs
                sx={{
                  borderBottom: t => '1px solid ' + t.palette.divider,
                }}
                value={activeTab}
                onChange={(event: React.ChangeEvent<{}>, newValue: number) => setActiveTab(newValue)}
                indicatorColor="primary"
                textColor="primary"
              >
                <Tab label={m.proResponse} />
                <Tab label={m.reportHistory} />
                <Tab label={m.companyHistory} />
              </Tabs>
              <ReportTabPanel value={activeTab} index={0}>
                <ReportResponseComponent
                  canEditFile
                  reportId={report.id}
                  response={response}
                  files={_report.get.entity?.files.filter(_ => _.origin === FileOrigin.Professional)}
                />
              </ReportTabPanel>
              <ReportTabPanel value={activeTab} index={1}>
                <ReportEvents
                  events={
                    _event.reportEvents.loading ? undefined : [creationReportEvent(report), ...(_event.reportEvents.entity ?? [])]
                  }
                />
              </ReportTabPanel>
              <ReportTabPanel value={activeTab} index={2}>
                <ReportEvents events={_event.companyEvents.loading ? undefined : _event.companyEvents.entity ?? []} />
              </ReportTabPanel>
            </>
          </Panel>
        </>
      ))}
    </Page>
  )
}

interface ReportTabPanelProps {
  children?: React.ReactNode
  index: any
  value: any
}

const ReportTabPanel = (props: ReportTabPanelProps) => {
  const {children, value, index, ...other} = props

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`scrollable-auto-tabpanel-${index}`}
      aria-labelledby={`scrollable-auto-tab-${index}`}
      {...other}
    >
      {value === index && children}
    </div>
  )
}
