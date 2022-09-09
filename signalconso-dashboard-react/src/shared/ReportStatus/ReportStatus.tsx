import {Label, LabelProps} from '../Label/Label'
import {useI18n} from '../../core/i18n'
import {useLogin} from '../../core/context/LoginContext'
import {Report, ReportStatus, ReportStatusPro} from '../../core/client/report/Report'

interface ReportStatusLabelProps extends Omit<LabelProps, 'children'> {
  status: ReportStatus
}

interface ReportStatusProLabelProps extends Omit<LabelProps, 'children'> {
  status: ReportStatusPro
}

export const reportStatusColor = {
  [ReportStatus.NA]: '#a1a1a1',
  [ReportStatus.LanceurAlerte]: '#a1a1a1',
  [ReportStatus.TraitementEnCours]: '#f57c00',
  [ReportStatus.Transmis]: '#f57c00',
  [ReportStatus.NonConsulte]: '#03a9f4',
  [ReportStatus.MalAttribue]: '#03a9f4',
  [ReportStatus.PromesseAction]: '#4caf50',
  [ReportStatus.Infonde]: '#4caf50',
  [ReportStatus.ConsulteIgnore]: '#d32f2f',
}

export const reportStatusProColor = {
  [ReportStatusPro.ARepondre]: '#f57c00',
  [ReportStatusPro.NonConsulte]: '#d32f2f',
  [ReportStatusPro.Cloture]: '#03a9f4',
}

export const ReportStatusLabel = ({status, ...props}: ReportStatusLabelProps) => {
  const {connectedUser} = useLogin()
  return connectedUser.isPro ? (
    <ReportStatusProLabel status={Report.getStatusProByStatus(status)} {...props} />
  ) : (
    <ReportStatusAdminLabel status={status} {...props} />
  )
}

export const ReportStatusAdminLabel = ({status, style, ...props}: ReportStatusLabelProps) => {
  const {m} = useI18n()
  return (
    <Label {...props} style={{color: 'white', background: reportStatusColor[status], ...style}}>
      {m.reportStatusShort[status]}
    </Label>
  )
}

export const ReportStatusProLabel = ({status, style, ...props}: ReportStatusProLabelProps) => {
  const {m} = useI18n()
  return (
    <Label {...props} style={{color: 'white', background: reportStatusProColor[status], ...style}}>
      {m.reportStatusShortPro[status]}
    </Label>
  )
}
