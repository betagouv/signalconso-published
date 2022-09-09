import {Dialog, DialogActions, DialogContent, DialogTitle} from '@mui/material'
import {ScButton} from '../../shared/Button/Button'
import {useI18n} from '../../core/i18n'
import {Txt} from '../../alexlibs/mui-extension'

interface Props {
  open: boolean
  onConfirm: () => void
  onClose: () => void
}

export const ConfirmDisableNotificationDialog = ({open, onClose, onConfirm}: Props) => {
  const {m} = useI18n()
  return (
    <Dialog maxWidth="xs" open={open}>
      <DialogTitle>{m.notificationDisableWarning}</DialogTitle>
      <DialogContent>
        <Txt color="hint">{m.notificationDisableWarningDesc}</Txt>
      </DialogContent>
      <DialogActions>
        <ScButton autoFocus onClick={onClose} color="primary">
          {m.cancel}
        </ScButton>
        <ScButton onClick={onConfirm} color="primary">
          {m.disable}
        </ScButton>
      </DialogActions>
    </Dialog>
  )
}
