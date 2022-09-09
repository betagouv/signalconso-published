import {Box, Icon, Tooltip} from '@mui/material'
import {Txt} from '../../alexlibs/mui-extension'
import {useI18n} from '../../core/i18n'
import {styleUtils} from '../../core/theme'

interface Props {
  consumerReferenceNumber?: string
}

export const ReportReferenceNumber = ({consumerReferenceNumber}: Props) => {
  const {m} = useI18n()
  return consumerReferenceNumber ? (
    <Box>
      <Tooltip arrow title={m.reportConsumerReferenceNumberDesc}>
        <Txt sx={{cursor: 'pointer'}}>
          <Txt sx={{color: t => t.palette.text.secondary}}>
            {m.reportConsumerReferenceNumber}
            <Icon fontSize="small" sx={{mb: -0.5, ml: 0.5}}>
              help_outline
            </Icon>{' '}
            :{' '}
          </Txt>
        </Txt>
      </Tooltip>
      <Txt sx={{fontSize: t => styleUtils(t).fontSize.big, overflowWrap: 'anywhere'}}>{consumerReferenceNumber}</Txt>
    </Box>
  ) : null
}
