import {alpha, Icon, IconButton, IconProps, Menu, MenuItem, SxProps, Theme} from '@mui/material'
import {Enum} from '../../alexlibs/ts-utils'
import {useI18n} from '../../core/i18n'
import {Txt} from '../../alexlibs/mui-extension'
import {useEffect, useMemo, useState} from 'react'
import {ReportTag} from '../../core/client/report/Report'

export type SelectTagsMenuValue = 'included' | 'excluded' | undefined

export type SelectTagsMenuValues = Partial<{[key in ReportTag]: SelectTagsMenuValue}>

interface ScSelectTagsMenuProps {
  onClose: () => void
  onChange: (_: SelectTagsMenuValues) => void
  open: boolean
  value?: SelectTagsMenuValues
  anchorEl: HTMLElement | null
}

const TagButton = ({
  children,
  color,
  sx,
  status = 'default',
  ...props
}: {
  status?: 'active' | 'inactive' | 'default'
  children: string
  color: (t: Theme) => string
} & Pick<IconProps, 'sx' | 'onClick'>) => {
  const parsedColor = status === 'active' ? color : (t: Theme) => t.palette.text.disabled
  return (
    <IconButton
      {...props}
      size="small"
      sx={{
        color: parsedColor,
        border: t => `1px solid ${alpha(parsedColor(t), 0.3)}`,
        ...(status === 'active' && {
          boxShadow: t => `inset 0 0 0 1px ${parsedColor(t)}`,
          borderColor: t => parsedColor(t),
        }),
        ...(status !== 'active' && {
          opacity: 0.5,
        }),
        ...sx,
      }}
    >
      <Icon fontSize="small">{children}</Icon>
    </IconButton>
  )
}

export const SelectTagsMenu = ({onClose, onChange, open, value, anchorEl}: ScSelectTagsMenuProps) => {
  const {m} = useI18n()

  const tags = useMemo(() => {
    const reponseConsoTag = ReportTag.ReponseConso
    const tagsWithoutReponseConso = (Enum.keys(ReportTag) as ReportTag[]).filter(_ => _ !== reponseConsoTag)
    return [reponseConsoTag, ...tagsWithoutReponseConso]
  }, [])
  const [innerValue, setInnerValue] = useState<SelectTagsMenuValues | undefined>()

  useEffect(() => {
    setInnerValue(value)
  }, [value])

  const handleChange = (tag: ReportTag, v: SelectTagsMenuValue) => {
    const newValue = {...innerValue, [tag]: value?.[tag] === v ? undefined : v}
    setInnerValue(newValue)
    onChange(newValue)
  }

  const switchTagValue = <T,>(tag: ReportTag, {included, excluded, notdefined}: {included: T; excluded: T; notdefined: T}): T => {
    if (innerValue?.[tag] === 'included') return included
    if (innerValue?.[tag] === 'excluded') return excluded
    return notdefined
  }

  return (
    <Menu anchorEl={anchorEl} open={open} onClose={onClose}>
      {tags.map(tag => (
        <MenuItem key={tag}>
          <TagButton
            status={switchTagValue(tag, {
              excluded: 'inactive',
              included: 'active',
              notdefined: 'default',
            })}
            color={t => t.palette.success.light}
            sx={{mr: 1}}
            onClick={() => handleChange(tag, 'included')}
          >
            add
          </TagButton>
          <TagButton
            status={switchTagValue(tag, {
              excluded: 'active',
              included: 'inactive',
              notdefined: 'default',
            })}
            color={t => t.palette.error.main}
            sx={{mr: 1}}
            onClick={() => handleChange(tag, 'excluded')}
          >
            remove
          </TagButton>
          <Txt
            size="big"
            sx={{
              flex: 1,
              ...switchTagValue<SxProps<Theme>>(tag, {
                excluded: {
                  color: t => t.palette.text.disabled,
                  textDecoration: 'line-through',
                },
                included: {
                  fontWeight: t => t.typography.fontWeightBold,
                },
                notdefined: {
                  color: t => t.palette.text.secondary,
                },
              }),
            }}
          >
            {m.reportTagDesc[tag]}
          </Txt>
        </MenuItem>
      ))}
    </Menu>
  )
}
