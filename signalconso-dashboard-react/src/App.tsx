import React, {useEffect} from 'react'
import {config} from './conf/config'
import {CircularProgress, CssBaseline, StyledEngineProvider, ThemeProvider} from '@mui/material'
import {BrowserRouter, HashRouter, Redirect, Route, Switch} from 'react-router-dom'
import {I18nProvider} from './core/i18n'
import {ReportProvider} from './core/context/ReportContext'
import {Reports} from './feature/Reports/Reports'
import {ReportComponent} from './feature/Report/Report'
import {ToastProvider} from './alexlibs/mui-extension'
import {muiTheme} from './core/theme'
import {ReportedWebsites} from './feature/ReportedWebsites/ReportedWebsites'
import {ReportedPhones} from './feature/ReportedPhones/ReportedPhones'
import {siteMap} from './core/siteMap'
import {Companies} from './feature/Companies/Companies'
import {Users} from './feature/Users/Users'
import {ConstantProvider} from './core/context/ConstantContext'
import {ReportedPhonesProvider} from './core/context/ReportedPhonesContext'
import {AsyncFileProvider} from './core/context/AsyncFileContext'
import {CompaniesProvider} from './core/context/CompaniesContext'
import {ReportsProvider} from './core/context/ReportsContext'
import {Provide} from './shared/Provide/Provide'
import {UsersProvider} from './core/context/UsersContext'
import {Settings} from './feature/Settings/Settings'
import {Subscriptions} from './feature/Subscriptions/Subscriptions'
import {SubscriptionsProvider} from './core/context/SubscriptionsContext'
import {LoginPage} from './feature/Login/LoginPage'
import {Layout, layoutConfig} from './core/Layout'
import {Login} from './shared/Login/Login'
import {LoginProvider, useLogin} from './core/context/LoginContext'
import {useFetcher} from './alexlibs/react-hooks-lib'
import {ReportsPro} from './feature/ReportsPro/ReportsPro'
import {ReportedWebsitesProvider} from './core/context/ReportedWebsitesContext'
import {CompanyAccesses} from './feature/CompanyAccesses/CompanyAccesses'
import {useHistory} from 'react-router'
import {UnregistredWebsitesProvider} from './core/context/UnregistredWebsitesContext'
import {CompaniesPro} from './feature/CompaniesPro/CompaniesPro'
import {ReportPro} from './feature/Report/ReportPro'
import {AccessesProvider} from './core/context/AccessesContext'
import {EmailValidation} from './feature/EmailValidation/EmailValidation'
import {CenteredContent} from './shared/CenteredContent/CenteredContent'
import {ResetPassword} from './feature/ResetPassword/ResetPassword'
import {UserActivation} from './feature/Users/UserActivation'
import {BlockedReportNotificationProvider} from './core/context/BlockedReportNotificationProviderContext'
import {ActivateNewCompany} from './feature/ActivateNewCompany/ActivateNewCompany'
import {ModeEmploiDGCCRF} from './feature/ModeEmploiDGCCRF/ModeEmploiDGCCRF'
import {ConsumerReview} from './feature/ConsumerReview/ConsumerReview'
import {CompanyComponent} from './feature/Company/Company'
import {CompaniesDbSyncProvider} from './core/context/CompaniesDbSyncContext'
import {EventProvider} from './core/context/EventContext'
import {CompaniesDbSync} from './feature/CompaniesDbSync/CompaniesDbSync'
import {Matomo} from './core/plugins/Matomo'
import {apiPublicSdk, makeSecuredSdk, SignalConsoApiSdk} from './core/ApiSdkInstance'
import {Stats} from './feature/Stats/Stats'
import {Admin} from './feature/Admin/Admin'
import {ConsumerEmailValidationProvider} from './core/context/EmailValidationContext'
import {WebsiteInvestigationProvider} from './core/context/WebsiteInvestigationContext'
import {ScSidebar} from './core/ScSidebar/ScSidebar'
import {ScHeader} from './core/ScHeader/ScHeader'
import {ApiError} from './core/client/ApiClient'

const Router: typeof HashRouter = config.useHashRouter ? HashRouter : BrowserRouter

export const App = () => {
  return (
    <Provide
      providers={[
        _ => <ThemeProvider theme={muiTheme()} children={_} />,
        _ => <StyledEngineProvider children={_} />,
        _ => <CssBaseline children={_} />,
        _ => <Router children={_} />,
        _ => <I18nProvider children={_} />,
        _ => <ToastProvider horizontal="right" children={_} />,
      ]}
    >
      <AppLogin />
    </Provide>
  )
}

const AppLogin = () => {
  const history = useHistory()
  const forgottenPassword = useFetcher<SignalConsoApiSdk['public']['authenticate']['forgotPassword'], ApiError>(
    apiPublicSdk.authenticate.forgotPassword,
  )

  return (
    <Login
      onRegister={apiPublicSdk.authenticate.sendActivationLink}
      onLogin={apiPublicSdk.authenticate.login}
      onLogout={() => history.push('/')}
      getTokenFromResponse={_ => _.token}
    >
      {({authResponse, login, logout, register, isCheckingToken, setToken}) => (
        <Layout
          header={<ScHeader />}
          sidebar={authResponse?.user && <ScSidebar connectedUser={authResponse.user} logout={logout} />}
        >
          <Switch>
            <Route path={siteMap.loggedout.emailValidation}>
              <EmailValidation onSaveToken={setToken} onValidateEmail={apiPublicSdk.authenticate.validateEmail} />
            </Route>
            <Route path={siteMap.loggedout.resetPassword()}>
              <ResetPassword onResetPassword={apiPublicSdk.authenticate.resetPassword} />
            </Route>
            <Route path={siteMap.loggedout.activatePro()}>
              <UserActivation
                onActivateUser={apiPublicSdk.user.activateAccount}
                onFetchTokenInfo={apiPublicSdk.user.fetchTokenInfo}
              />
            </Route>
            <Route path={siteMap.loggedout.activateDgccrf}>
              <UserActivation
                onActivateUser={apiPublicSdk.user.activateAccount}
                onFetchTokenInfo={apiPublicSdk.user.fetchTokenInfo}
              />
            </Route>
            <Route path={siteMap.loggedout.consumerReview()}>
              <ConsumerReview onSubmit={apiPublicSdk.report.postReviewOnReportResponse} />
            </Route>
            <Route path="/">
              {authResponse ? (
                <LoginProvider
                  connectedUser={authResponse.user}
                  token={authResponse.token}
                  onLogout={logout}
                  apiSdk={makeSecuredSdk(authResponse.token)}
                >
                  <AppLogged />
                </LoginProvider>
              ) : isCheckingToken ? (
                <CenteredContent offset={layoutConfig.headerHeight}>
                  <CircularProgress />
                </CenteredContent>
              ) : (
                <LoginPage
                  login={login}
                  register={register}
                  forgottenPassword={{
                    action: (email: string) => forgottenPassword.fetch({}, email),
                    loading: forgottenPassword.loading,
                    error: forgottenPassword.error,
                  }}
                />
              )}
            </Route>
          </Switch>
        </Layout>
      )}
    </Login>
  )
}

const AppLogged = () => {
  const {apiSdk, connectedUser, logout} = useLogin()
  const history = useHistory()
  useEffect(() => history.listen(_ => Matomo.trackPage(`/${connectedUser.role.toLocaleLowerCase()}${_.pathname}`)), [history])

  return (
    <Provide
      providers={[
        _ => <ReportsProvider api={apiSdk} children={_} />,
        _ => <ReportProvider api={apiSdk} children={_} />,
        _ => <ConstantProvider api={apiSdk} children={_} />,
        _ => <ReportedPhonesProvider api={apiSdk} children={_} />,
        _ => <AsyncFileProvider api={apiSdk} children={_} />,
        _ => <CompaniesProvider api={apiSdk} children={_} />,
        _ => <UsersProvider api={apiSdk} children={_} />,
        _ => <ConsumerEmailValidationProvider api={apiSdk} children={_} />,
        _ => <ReportedWebsitesProvider api={apiSdk} children={_} />,
        _ => <WebsiteInvestigationProvider api={apiSdk} children={_} />,
        _ => <UnregistredWebsitesProvider api={apiSdk} children={_} />,
        _ => <SubscriptionsProvider api={apiSdk} children={_} />,
        _ => <AccessesProvider api={apiSdk} children={_} />,
        _ => <BlockedReportNotificationProvider api={apiSdk} children={_} />,
        _ => <CompaniesDbSyncProvider api={apiSdk} children={_} />,
        _ => <EventProvider api={apiSdk} children={_} />,
      ]}
    >
      <Switch>
        <Route path={siteMap.logged.admin} component={Admin} />
        <Route path={siteMap.logged.reportedWebsites} component={ReportedWebsites} />
        <Route path={siteMap.logged.reportedPhone} component={ReportedPhones} />
        <Route path={siteMap.logged.report()} component={connectedUser.isPro ? ReportPro : ReportComponent} />
        <Route path={siteMap.logged.reports()} component={connectedUser.isPro ? ReportsPro : Reports} />
        <Route path={siteMap.logged.users} component={Users} />
        <Route path={siteMap.logged.companies} component={Companies} />
        <Route path={siteMap.logged.companyAccesses()} component={CompanyAccesses} />
        <Route path={siteMap.logged.company()} component={CompanyComponent} />
        <Route path={siteMap.logged.subscriptions} component={Subscriptions} />
        <Route path={siteMap.logged.companiesPro} component={CompaniesPro} />
        <Route path={siteMap.logged.settings} component={Settings} />
        <Route path={siteMap.logged.modeEmploiDGCCRF} component={ModeEmploiDGCCRF} />
        <Route path={siteMap.logged.companiesDbSync} component={CompaniesDbSync} />
        <Route path={siteMap.logged.stats} component={Stats} />
        <Route path={siteMap.loggedout.register} component={ActivateNewCompany} />
        <Redirect from="/" to={siteMap.logged.reports()} />
      </Switch>
    </Provide>
  )
}
