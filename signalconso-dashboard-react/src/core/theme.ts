import {red} from '@mui/material/colors'
import {alpha, createTheme, darken, SxProps, Theme} from '@mui/material'
import {ThemeOptions} from '@mui/material/styles/createTheme'
import {makeSx} from '../alexlibs/mui-extension'
import {lighten} from '@mui/system/colorManipulator'

export const combineSx = (...sxs: (SxProps<Theme> | undefined | false)[]): SxProps<Theme> => {
  return sxs.reduce((res, sx) => (sx !== undefined && sx !== false ? {...res, ...sx} : res), {} as any)
}

export const sxUtils = makeSx({
  fontBig: {
    fontSize: t => t.typography.fontSize * 1.15,
  },
  fontNormal: {
    fontSize: t => t.typography.fontSize,
  },
  fontSmall: {
    fontSize: t => t.typography.fontSize * 0.85,
  },
  fontTitle: {
    fontSize: t => t.typography.fontSize * 1.3,
  },
  fontBigTitle: {
    fontSize: t => t.typography.fontSize * 1.6,
  },
  tdActions: {
    textAlign: 'right',
  },
  truncate: {
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
  },
  inlineIcon: {
    display: 'inline !important',
    fontSize: 'inherit',
    lineHeight: 1,
    verticalAlign: 'text-top',
  },
  divider: {
    mt: 2,
    mb: 2,
  },
})

export const styleUtils = (t: Theme) => ({
  gridSpacing: 3 as any,
  fontSize: {
    big: t.typography.fontSize * 1.15,
    normal: t.typography.fontSize,
    small: t.typography.fontSize * 0.85,
    title: t.typography.fontSize * 1.3,
    bigTitle: t.typography.fontSize * 1.6,
  },
  spacing: (...args: number[]) => {
    const [top = 0, right = 0, bottom = 0, left = 0] = args ?? [1, 1, 2, 1]
    return `${t.spacing(top)} ${t.spacing(right)} ${t.spacing(bottom)} ${t.spacing(left)}`
  },
  color: {
    success: '#00b79f',
    error: '#cf0040',
    warning: '#FFB900',
    info: '#0288d1',
  },
  truncate: {
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
  } as any,
})

export const defaultSpacing = 8

export const muiTheme = (dark?: boolean): Theme => {
  const defaultRadius = 6
  const fontFamily = '"Open Sans", sans-serif'
  const fontSize = 15
  const colorPrimary = {
    main: '#2b7c9f',
    light: '#6fd3ff',
    dark: '#1c536b',
    // main: '#407e99',
    // light: '#6697ad',
    // dark: '#2c586b',
  }
  const colorSecondary = {
    main: '#1e2b50',
    light: lighten('#1e2b50', 0.3),
    dark: darken('#1e2b50', 0.3),
  }
  const baseTheme = createTheme({
    spacing: defaultSpacing,
    palette: {
      primary: colorPrimary,
      secondary: colorSecondary,
      error: red,
      mode: dark ? 'dark' : 'light',
    },
    shape: {
      borderRadius: defaultRadius,
    },
    typography: {
      fontSize,
      fontFamily,
      fontWeightBold: 500,
    },
  })
  const theme: ThemeOptions = {
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          '*': {
            boxSizing: 'border-box',
          },
          '.material-icons': {
            // display: 'inherit',
          },
          '.recharts-surface': {
            overflow: 'visible',
          },
          html: {
            fontSize: baseTheme.typography.fontSize,
            color: baseTheme.palette.text.primary,
          },
          body: {
            fontSize: '1rem',
            lineHeight: '1.5rem',
            background: baseTheme.palette.background.paper,
            margin: 0,
            color: baseTheme.palette.text.primary,
            boxSizing: 'border-box',
          },
          ul: {
            marginTop: '.5em',
          },
          h1: baseTheme.typography.h4,
          h2: {
            ...baseTheme.typography.h6,
            mb: 2,
            mt: 3,
          },
          p: {
            ...baseTheme.typography.body1,
            textAlign: 'justify',
          },
          a: {
            color: 'inherit',
            textDecoration: 'none',
          },
          ':focus': {
            outline: 0,
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 20,
          },
          outlinedPrimary: {
            borderColor: baseTheme.palette.divider,
          },
        },
      },
      // MuiCard: {
      //   styleOverrides: {
      //     root: {
      //       borderRadius: defaultRadius,
      //     },
      //   },
      // },
      MuiTabs: {
        styleOverrides: {
          root: {
            minHeight: 0,
          },
        },
      },
      MuiTab: {
        styleOverrides: {
          root: {
            textTransform: 'none',
            fontWeight: 600,
            minHeight: 40,
            minWidth: '80px !important',
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          outlined: {
            borderColor: baseTheme.palette.divider,
          },
        },
      },
      MuiMenuItem: {
        styleOverrides: {
          root: {
            fontSize: '1rem',
            minHeight: 42,
            [baseTheme.breakpoints.up('xs')]: {
              minHeight: 42,
            },
          },
        },
      },
      MuiDialogTitle: {
        styleOverrides: {
          root: {
            paddingBottom: 8,
          },
        },
      },
      MuiFormHelperText: {
        styleOverrides: {
          sizeSmall: {
            marginBottom: -4,
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: {
            paddingTop: 0,
            paddingBottom: 0,
            minHeight: 50,
            height: 50,
            paddingRight: 8,
            paddingLeft: 8,
          },
          head: {
            lineHeight: 1.2,
          },
          sizeSmall: {
            height: 40,
            minHeight: 40,
          },
        },
      },
      MuiTooltip: {
        styleOverrides: {
          tooltip: {
            fontSize: baseTheme.typography.fontSize,
            fontWeight: 'normal',
          },
        },
      },
      MuiIcon: {
        styleOverrides: {
          root: {
            width: 'auto',
          },
        },
      },
      MuiIconButton: {
        styleOverrides: {
          root: {
            spacing: 6,
          },
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            '&:hover $notchedOutline': {
              borderColor: alpha(colorPrimary.main, 0.7),
            },
          },
          notchedOutline: {
            transition: 'border-color 140ms cubic-bezier(0.4, 0, 0.2, 1) 0ms',
            background: 'rgba(0,0,0,.02)',
            borderColor: 'rgba(0, 0, 0, 0.12)',
          },
        },
      },
    },
  }
  return createTheme({
    ...baseTheme,
    ...theme,
    ...(dark
      ? {
          MuiOutlinedInput: {
            styleOverrides: {
              notchedOutline: {
                borderColor: '#d9dce0',
              },
            },
          },
        }
      : ({} as any)),
  })
}
