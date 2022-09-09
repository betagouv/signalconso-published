import * as React from 'react'
import {Page} from 'shared/Layout'
import {StatsDgccrfAccountPanel} from './StatsDgccrfAccountPanel'
import {StatsDgccrfSubscriptionsPanel} from './StatsDgccrfSubscriptionsPanel'

export const DgccrfStats = () => {
  return (
    <Page>
      <StatsDgccrfAccountPanel />
      <StatsDgccrfSubscriptionsPanel />
    </Page>
  )
}
