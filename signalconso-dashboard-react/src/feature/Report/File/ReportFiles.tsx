import {Box} from '@mui/material'
import React, {useEffect, useMemo, useRef, useState} from 'react'
import {ReportFileAdd} from './ReportFileAdd'
import {ReportFile} from './ReportFile'
import {useI18n} from '../../../core/i18n'
import {Txt} from '../../../alexlibs/mui-extension'
import {FileOrigin, UploadedFile} from '../../../core/client/file/UploadedFile'
import {Id} from '../../../core/model'

export interface ReportFilesProps {
  files?: UploadedFile[]
  onNewFile?: (f: UploadedFile) => void
  onRemoveFile?: (f: UploadedFile) => void
  reportId: Id
  fileOrigin: FileOrigin
  hideAddBtn?: boolean
}

export const ReportFiles = ({
  reportId,
  fileOrigin,
  files,
  hideAddBtn,
  onRemoveFile = () => void 0,
  onNewFile = () => void 0,
}: ReportFilesProps) => {
  const [innerFiles, setInnerFiles] = useState<UploadedFile[]>()
  const {m} = useI18n()
  const attachmentRef = useRef<any>()

  useEffect(() => {
    setInnerFiles(files)
  }, [files])

  useEffect(() => {
    if (attachmentRef.current && window.location.href.includes('anchor=attachment')) {
      attachmentRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
        inline: 'center',
      })
    }
  }, [attachmentRef])

  const newFile = (f: UploadedFile) => {
    onNewFile(f)
    setInnerFiles(prev => [f, ...(prev ?? [])])
  }

  const removeFile = (f: UploadedFile) => {
    onRemoveFile(f)
    setInnerFiles(prev => prev?.filter(_ => _.id !== f.id))
  }

  return (
    <>
      <Box
        ref={attachmentRef}
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          m: -1,
        }}
      >
        {innerFiles
          ?.filter(_ => _.origin === fileOrigin)
          .map(_ => (
            <ReportFile key={_.id} file={_} onRemove={hideAddBtn ? undefined : removeFile} />
          ))}
        {!hideAddBtn && <ReportFileAdd reportId={reportId} fileOrigin={fileOrigin} onUploaded={newFile} />}
      </Box>
      {hideAddBtn && innerFiles?.length === 0 && (
        <Txt block color="hint" sx={{my: 1}}>
          {m.noAttachment}
        </Txt>
      )}
    </>
  )
}
