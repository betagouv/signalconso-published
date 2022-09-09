import {Page, PageTitle} from '../../shared/Layout'
import {useI18n} from '../../core/i18n'
import React from 'react'
import {Redirect, Route, Switch, useRouteMatch} from 'react-router-dom'
import {siteMap} from '../../core/siteMap'
import {PageTab, PageTabs} from '../../shared/Layout/Page/PageTabs'
import {CompaniesToActivate} from './CompaniesToActivate'
import {CompaniesRegistered} from './CompaniesRegistered'
import {useLogin} from '../../core/context/LoginContext'

export const Companies = () => {
  const {m} = useI18n()
  const {path} = useRouteMatch()
  const {connectedUser} = useLogin()

  return (
    <Page>
      <PageTitle>{m.company}</PageTitle>
      {connectedUser.isAdmin && (
        <PageTabs>
          <PageTab to={siteMap.logged.companies_registered} label={m.companiesActivated} />
          <PageTab to={siteMap.logged.companies_toActivate} label={m.companiesToActivate} />
        </PageTabs>
      )}
      <Switch>
        <Redirect exact from={path} to={siteMap.logged.companies_registered} />
        <Route path={siteMap.logged.companies_registered} component={CompaniesRegistered} />
        <Route path={siteMap.logged.companies_toActivate} component={CompaniesToActivate} />
      </Switch>
    </Page>
  )
}
