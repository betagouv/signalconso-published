import {Panel, PanelBody} from '../../shared/Panel'
import {Box} from '@mui/material'
import {Txt} from '../../alexlibs/mui-extension'
import {ReportFiles} from './File/ReportFiles'
import React, {ReactNode} from 'react'
import {useReportContext} from '../../core/context/ReportContext'
import {useLogin} from '../../core/context/LoginContext'
import {useI18n} from '../../core/i18n'
import {Divider} from '../../shared/Divider/Divider'
import {EventActionValues} from '../../core/client/event/Event'
import {FileOrigin, UploadedFile} from '../../core/client/file/UploadedFile'
import {Report} from '../../core/client/report/Report'

interface Props {
  files?: UploadedFile[]
  report: Report
  children?: ReactNode
}

export const ReportDescription = ({report, files, children}: Props) => {
  const _report = useReportContext()
  const {connectedUser} = useLogin()
  const {m} = useI18n()

  return (
    <Panel>
      <PanelBody>
        {report.details.map((detail, i) => (
          <Box key={i} sx={{mb: 1}}>
            <Box
              sx={{fontWeight: t => t.typography.fontWeightBold}}
              dangerouslySetInnerHTML={{__html: detail.label.replace(/:$/, '')}}
            />
            <Box sx={{color: t => t.palette.text.secondary}} dangerouslySetInnerHTML={{__html: detail.value}} />
          </Box>
        ))}
        <Divider margin />
        <Txt bold block gutterBottom>
          {m.attachedFiles}
        </Txt>
        <ReportFiles
          hideAddBtn={!connectedUser.isAdmin}
          files={files?.filter(_ => _.origin === FileOrigin.Consumer)}
          reportId={report.id}
          fileOrigin={FileOrigin.Consumer}
          onNewFile={file => {
            _report.postAction
              .fetch({}, report.id, {
                details: '',
                fileIds: [file.id],
                actionType: EventActionValues.ConsumerAttachments,
              })
              .then(() => _report.get.fetch({force: true, clean: false}, report.id))
          }}
        />
        {children && (
          <>
            <Divider margin />
            {children}
          </>
        )}
      </PanelBody>
    </Panel>
  )
}
