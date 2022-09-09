import {Box, Icon, Tooltip} from '@mui/material'
import {extensionToType, FileType, reportFileConfig} from './reportFileConfig'
import {useLogin} from '../../../core/context/LoginContext'
import {config} from '../../../conf/config'
import React, {useEffect} from 'react'
import {useFetcher} from '../../../alexlibs/react-hooks-lib'
import {IconBtn} from '../../../alexlibs/mui-extension'
import {useToast} from '../../../core/toast'
import {useI18n} from '../../../core/i18n'
import {ScDialog} from '../../../shared/Confirm/ScDialog'
import {combineSx, defaultSpacing} from 'core/theme'
import {makeSx} from '../../../alexlibs/mui-extension'
import {UploadedFile} from '../../../core/client/file/UploadedFile'
import {ScOption} from 'core/helper/ScOption'

export interface ReportFileProps {
  file: UploadedFile
  dense?: boolean
  onRemove?: (file: UploadedFile) => void
}

const removeBtnSize = 30
const cardMargin = 1

const css = makeSx({
  root: {
    display: 'block',
    position: 'relative',
    padding: t => t.spacing(cardMargin),
    '&:hover > $removeBtn': {
      display: 'flex !important',
    },
  },
  image: {
    display: 'inline-flex',
    border: t => '1px solid ' + t.palette.divider,
    borderRadius: reportFileConfig.cardBorderRadius,
    height: reportFileConfig.cardSize,
    width: reportFileConfig.cardSize,
    color: t => t.palette.text.disabled,
    overflow: 'hidden',
    position: 'relative',
    transition: t => t.transitions.create('all'),
    '&:hover': {
      boxShadow: t => t.shadows[4],
    },
    '& > div': {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundSize: 'cover',
      height: '100%',
      width: '100%',
    },
  },
  removeBtn: {
    display: 'none !important',
    position: 'absolute',
    top: (removeBtnSize - defaultSpacing * cardMargin) / -2,
    right: (removeBtnSize - defaultSpacing * cardMargin) / -2,
    width: removeBtnSize,
    height: removeBtnSize,
    borderRadius: removeBtnSize,
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: t => t.shadows[4],
    background: t => t.palette.background.paper + ' !important',
  },
  imgImage: {
    color: '#00b50f',
  },
  imgPdf: {
    color: '#db4537',
  },
  imgDoc: {
    color: '#4185f3',
  },
  backgroundImage: {
    position: 'absolute',
    top: 0,
    right: 0,
    left: 0,
    bottom: 0,
    backgroundSize: 'cover',
  },
})

export const ReportFile = ({file, dense, onRemove}: ReportFileProps) => {
  const fileType = extensionToType(file.filename)
  const {apiSdk} = useLogin()
  const _remove = useFetcher(apiSdk.secured.document.remove)
  const {toastError} = useToast()
  const {m} = useI18n()

  const fileUrl = ScOption.from(apiSdk.public.document.getLink(file))
    .map(_ => (config.isDev ? _.replace(config.apiBaseUrl, 'https://signal-api.conso.gouv.fr') : _))
    .toUndefined()

  const remove = async () => {
    await _remove.fetch({}, file)
    onRemove?.(file)
  }

  useEffect(() => {
    ScOption.from(_remove.error).map(toastError)
  }, [_remove.error])

  return (
    <Tooltip title={file.filename}>
      <Box component="a" target="_blank" href={fileUrl} sx={css.root}>
        <Box sx={css.image}>
          {(() => {
            switch (fileType) {
              case FileType.Image: {
                return (
                  <div>
                    <Box sx={combineSx(css.backgroundImage, {backgroundImage: `url(${fileUrl})`})} />
                    <Icon sx={css.imgImage}>image</Icon>
                  </div>
                )
              }
              case FileType.PDF: {
                return (
                  <div>
                    <Icon sx={css.imgPdf}>picture_as_pdf</Icon>
                  </div>
                )
              }
              case FileType.Doc: {
                return (
                  <div>
                    <Icon sx={css.imgDoc}>article</Icon>
                  </div>
                )
              }
              default: {
                return (
                  <div>
                    <Icon>insert_drive_file</Icon>
                  </div>
                )
              }
            }
          })()}
        </Box>
        {onRemove && (
          <ScDialog
            title={m.removeAsk}
            content={<span dangerouslySetInnerHTML={{__html: m.thisWillBeRemoved(file.filename)}} />}
            maxWidth="xs"
            onClick={e => {
              e.stopPropagation()
              e.preventDefault()
            }}
            onConfirm={(event, close) => {
              remove()
              close()
            }}
            confirmLabel={m.delete}
          >
            <IconBtn loading={_remove.loading} size="small" sx={css.removeBtn}>
              <Icon>clear</Icon>
            </IconBtn>
          </ScDialog>
        )}
      </Box>
    </Tooltip>
  )
}
