import React, {ReactElement, useState} from 'react'
import {Alert} from '../../alexlibs/mui-extension'
import {useI18n} from '../../core/i18n'
import {ScInput} from '../../shared/Input/ScInput'
import {useFetcher} from '../../alexlibs/react-hooks-lib'
import {useLogin} from '../../core/context/LoginContext'
import {useToast} from '../../core/toast'
import {ScDialog} from '../../shared/Confirm/ScDialog'
import {EventActionValues} from '../../core/client/event/Event'
import {Report} from '../../core/client/report/Report'

interface Props {
  report: Report
  children: ReactElement<any>
  onAdd: () => void
  required?: boolean
  label: string
  actionType: EventActionValues
}

export const ReportPostAction = ({label, actionType, report, children, onAdd, required}: Props) => {
  const {m} = useI18n()
  const {apiSdk} = useLogin()
  const _addComment = useFetcher(apiSdk.secured.reports.postAction)
  const [comment, setComment] = useState('')
  const {toastSuccess} = useToast()

  const addComment = () => {
    return _addComment.fetch({}, report.id, {actionType, details: comment, fileIds: []})
  }

  return (
    <ScDialog
      title={label}
      loading={_addComment.loading}
      onConfirm={(event, close) =>
        addComment().then(() => {
          setComment('')
          onAdd()
          toastSuccess(m.commentAdded)
          close()
        })
      }
      confirmLabel={m.add}
      confirmDisabled={required && comment === ''}
      content={
        <>
          {_addComment.error && <Alert type="error">{m.anErrorOccurred}</Alert>}
          <ScInput
            required={required}
            value={comment}
            onChange={e => setComment(e.target.value)}
            multiline
            fullWidth
            rows={3}
            maxRows={8}
          />
        </>
      }
    >
      {children}
    </ScDialog>
  )
}
