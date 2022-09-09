import {mapDatesToQueryString} from './helper/useQueryString'
import {toQueryString} from './helper'
import {ReportSearch} from './client/report/ReportSearch'
import {Id} from './model'

export const siteMap = {
  logged: {
    admin: `/admin`,
    reportedWebsites: `/moderation-url-entreprises`,
    reportedWebsites_unknown: `/moderation-url-entreprises/sites-internet/non-identifies`,
    websitesInvestigation: `/moderation-url-entreprises/enquete`,
    reportedPhone: `/suivi-des-telephones`,
    reports: (_?: Partial<ReportSearch>) => `/suivi-des-signalements` + (_ ? toQueryString(mapDatesToQueryString(_)) : ``),
    subscriptions: `/abonnements`,
    report: (id: Id = `:id`) => `/suivi-des-signalements/report/${id}`,
    exports: `/mes-telechargements`,
    companies_toActivate: `/entreprises/a-activer`,
    companies_registered: `/entreprises/les-plus-signalees`,
    companies: `/entreprises`,
    companiesPro: `/mes-entreprises`,
    companyAccesses: (siret: string = `:siret`) => `/entreprise/acces/${siret}`,
    users: `/users`,
    company: (id: Id = `:id`) => `/bilan-entreprise/${id}`,
    users_dgccrf_pending: `/users/pending`,
    users_consumer_validation: `/users/consumers`,
    users_dgccrf_all: `/users/all`,
    settings: `/parametres`,
    modeEmploiDGCCRF: `/mode-emploi-dgccrf`,
    companiesDbSync: `/companies-db-sync`,
    stats: `/stats`,
    proStats: `/stats/pro-stats`,
    dgccrfStats: `/stats/dgccrf-stats`,
    reportStats: `/stats/report-stats`,
  },
  loggedout: {
    activatePro: (siret: string = `:siret`) => `/entreprise/rejoindre/${siret}`,
    activateDgccrf: `/dgccrf/rejoindre`,
    consumerReview: (reportId: Id = `:reportId`) => `/suivi-des-signalements/${reportId}/avis`,
    register: `/activation`,
    login: `/connexion`,
    emailValidation: `/connexion/validation-email`,
    resetPassword: (token: Id = `:token`) => `/connexion/nouveau-mot-de-passe/${token}`,
  },
}
