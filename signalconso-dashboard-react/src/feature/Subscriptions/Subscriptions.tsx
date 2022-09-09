import {Page, PageTitle} from '../../shared/Layout'
import React, {useEffect} from 'react'
import {useI18n} from '../../core/i18n'
import {useSubscriptionsContext} from '../../core/context/SubscriptionsContext'
import {SubscriptionCard} from './SubscriptionCard'
import {Alert, Animate} from '../../alexlibs/mui-extension'
import {Box, Icon, LinearProgress} from '@mui/material'
import {Ripple} from '../../shared/Ripple/Ripple'
import {styleUtils} from '../../core/theme'

export const Subscriptions = () => {
  const {m} = useI18n()
  const _subscriptions = useSubscriptionsContext()

  useEffect(() => {
    _subscriptions.fetch()
  }, [])

  return (
    <Page size="s">
      <PageTitle>{m.menu_subscriptions}</PageTitle>

      <Alert id="subscriptions-info" type="info" deletable sx={{mb: 2}}>
        <div dangerouslySetInnerHTML={{__html: m.subscriptionsAlertInfo}} />
      </Alert>

      {_subscriptions.fetching && <LinearProgress />}
      <Animate>
        <Ripple>
          <Box
            sx={{
              my: 3,
              overflow: 'hidden',
              cursor: 'pointer',
              fontWeight: t => t.typography.fontWeightBold,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              p: 2,
              border: t => `1px dashed ${t.palette.divider}`,
              color: t => t.palette.primary.main,
              fontSize: t => styleUtils(t).fontSize.title,
              borderRadius: t => t.shape.borderRadius + 'px',
            }}
            title={m.add}
            onClick={() => !_subscriptions.creating && _subscriptions.create({insertBefore: true})}
          >
            <Icon>add</Icon>
            {m.add}
          </Box>
        </Ripple>
      </Animate>
      {_subscriptions.list?.map(subscription => (
        <SubscriptionCard
          key={subscription.id}
          subscription={subscription}
          removing={_subscriptions.removing(subscription.id)}
          onUpdate={_ => _subscriptions.update(subscription.id, _)}
          onDelete={() => _subscriptions.remove(subscription.id)}
        />
      ))}
    </Page>
  )
}
