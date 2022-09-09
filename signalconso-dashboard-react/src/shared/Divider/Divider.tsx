import {Divider as MuiDivider, DividerProps} from '@mui/material'

export const Divider = ({margin, sx, ...props}: DividerProps & {margin?: boolean}) => {
  return (
    <MuiDivider
      {...props}
      sx={{
        ...(margin && {
          mt: 2,
          mb: 2,
        }),
        ...sx,
      }}
    />
  )
}
