import * as React from 'react'
import {useEffect} from 'react'
import {useI18n} from '../../../core/i18n'
import {Divider, Icon, List, ListItem, ListItemIcon, ListItemText} from '@mui/material'
import {Panel, PanelBody, PanelHead} from '../../../shared/Panel'
import {AddressComponent} from '../../../shared/Address/Address'
import {useFetcher} from '../../../alexlibs/react-hooks-lib'
import {Company} from '../../../core/client/company/Company'

interface Props {
  company: Company
}

export const CompanyInfo = ({company}: Props) => {
  const {m, formatDate} = useI18n()

  const _activityCodes = useFetcher(() => import('../../../core/activityCodes').then(_ => _.activityCodes))

  useEffect(() => {
    _activityCodes.fetch()
  }, [company])

  const activities = _activityCodes.entity

  return (
    <Panel loading={_activityCodes.loading}>
      <PanelHead>{m.informations}</PanelHead>
      <PanelBody>
        <List>
          <ListItem>
            <ListItemIcon>
              <Icon>location_on</Icon>
            </ListItemIcon>
            <ListItemText primary={m.address} secondary={<AddressComponent address={company.address} />} />
          </ListItem>
          <Divider variant="inset" component="li" />
          <ListItem>
            <ListItemIcon>
              <Icon>event</Icon>
            </ListItemIcon>
            <ListItemText primary={m.creationDate} secondary={formatDate(company.creationDate)} />
          </ListItem>
          <Divider variant="inset" component="li" />
          <ListItem>
            <ListItemIcon>
              <Icon>label</Icon>
            </ListItemIcon>
            {activities && (
              <ListItemText primary={m.activityCode} secondary={company.activityCode ? activities[company.activityCode] : ''} />
            )}
          </ListItem>
        </List>
      </PanelBody>
    </Panel>
  )
}
