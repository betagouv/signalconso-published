import {Box, BoxProps} from '@mui/material'
import {styleUtils} from '../../core/theme'

interface Props extends BoxProps {}

export const PanelTitle = ({sx, ...props}: Props) => {
  return (
    <Box
      component="h3"
      {...props}
      sx={{
        fontWeight: t => t.typography.fontWeightMedium,
        my: 2,
        mx: 0,
        fontSize: t => styleUtils(t).fontSize.title,
        ...sx,
      }}
    />
  )
}
