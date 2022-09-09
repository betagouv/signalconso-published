import {config} from '../../../conf/config'
import {Icon, Tooltip} from '@mui/material'
import React from 'react'
import {extensionToType, FileType} from './reportFileConfig'
import {ReportFileProps} from './ReportFile'
import {useLogin} from '../../../core/context/LoginContext'
import {makeSx} from '../../../alexlibs/mui-extension'
import {ScOption} from 'core/helper/ScOption'

const css = makeSx({
  imgPdf: {
    color: '#db4537',
  },
  imgDoc: {
    color: '#4185f3',
  },
  imgPicture: {
    color: '#00c385',
  },
})

export const ReportFileSmall = ({file}: ReportFileProps) => {
  const fileType = extensionToType(file.filename)
  const {apiSdk} = useLogin()
  const fileUrl = ScOption.from(apiSdk.public.document.getLink(file))
    .map(_ => (config.isDev ? _.replace(config.apiBaseUrl, 'https://signal-api.conso.gouv.fr') : _))
    .toUndefined()

  return (
    <Tooltip title={file.filename} key={file.id}>
      <a href={fileUrl} target="_blank">
        {(() => {
          switch (fileType) {
            case FileType.Image: {
              return <Icon sx={css.imgPicture}>image</Icon>
            }
            case FileType.PDF: {
              return <Icon sx={css.imgPdf}>picture_as_pdf</Icon>
            }
            case FileType.Doc: {
              return <Icon sx={css.imgDoc}>article</Icon>
            }
            default: {
              return <Icon>insert_drive_file</Icon>
            }
          }
        })()}
      </a>
    </Tooltip>
  )
}
