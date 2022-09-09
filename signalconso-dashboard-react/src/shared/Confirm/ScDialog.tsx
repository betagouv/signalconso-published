import {Modal, ModalProps} from '../../alexlibs/mui-extension'
import {useI18n} from '../../core/i18n'

export const ScDialog = ({confirmLabel, cancelLabel, ...props}: ModalProps) => {
  const {m} = useI18n()
  return <Modal confirmLabel={confirmLabel ?? m.confirm} cancelLabel={cancelLabel ?? m.close} {...props} />
}
