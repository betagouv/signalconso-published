import {Box, BoxProps, ListItemIcon, ListItemText, MenuItem, Tooltip} from '@mui/material'
import React from 'react'
import {ScMenu} from '../../shared/Menu/Menu'
import antidropIcon from './img/antidrop.png'
import whoisIcon from './img/whois.png'
import internetArchiveIcon from './img/internetArchive.svg'
import scamdocIcon from './img/scamdoc.png'
import {useI18n} from '../../core/i18n'
import {WebsiteWithCompany} from '../../core/client/website/Website'

interface WebsiteActionsProps extends BoxProps {
  website: WebsiteWithCompany
}

const websiteAnalyzeTool = [
  {
    label: 'Antidrop',
    img: antidropIcon,
    url: (website: string) => `https://antidrop.fr`,
  },
  {
    label: 'Whois',
    img: whoisIcon,
    url: (website: string) => `https://who.is/whois/${website}`,
  },
  {
    label: 'The Internet Archive',
    img: internetArchiveIcon,
    url: (website: string) => `https://web.archive.org/web/*/${website}`,
  },
  {
    label: 'ScamDoc',
    img: scamdocIcon,
    url: (website: string) => `https://www.scamdoc.com`,
  },
]

export const WebsiteTools = ({website}: WebsiteActionsProps) => {
  const {m} = useI18n()
  return (
    <Tooltip title={m.identicationTools}>
      <ScMenu icon="travel_explore">
        {websiteAnalyzeTool.map(tool => (
          <MenuItem key={tool.label} onClick={() => window.open(tool.url(website.host), '_blank')}>
            <ListItemIcon>
              <Box
                component="img"
                src={tool.img}
                alt={tool.label}
                sx={{
                  height: 18,
                }}
              />
            </ListItemIcon>
            <ListItemText>{tool.label}</ListItemText>
          </MenuItem>
        ))}
      </ScMenu>
    </Tooltip>
  )
}
