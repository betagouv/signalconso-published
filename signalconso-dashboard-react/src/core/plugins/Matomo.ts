import {config} from 'conf/config'

declare const _paq: any

export class Matomo {
  constructor() {}

  private static previousTrackedPage?: string

  private static isAlreadyFired = (path: string) => {
    if (path !== Matomo.previousTrackedPage) {
      Matomo.previousTrackedPage = path
      return false
    }
    return true
  }

  static readonly trackEvent = (category: EventCategories, action: AnalyticAction, name?: any, value?: any) => {
    Matomo.push(['trackEvent', category, action, name, value])
  }

  static readonly trackPage = (path: string) => {
    if (!Matomo.isAlreadyFired(path)) {
      Matomo.push(['setCustomUrl', config.appBaseUrl + path])
      Matomo.push(['trackPageView'])
    }
  }

  private static readonly push = (args: any[]) => {
    if (config.isDev) {
      console.info('[Matomo]', args)
    } else {
      try {
        _paq.push(args)
      } catch (e) {
        console.error('[Matomo]', e)
        if (!(e instanceof ReferenceError)) {
          throw e
        }
      }
    }
  }
}

type AnalyticAction =
  | AuthenticationEventActions
  | ReportEventActions
  | DgccrfEventActions
  | CompanySearchEventActions
  | ContractualDisputeActions
  | AccountEventActions
  | AccessEventActions

export enum EventCategories {
  report = 'Signalement',
  companySearch = "Identification de l'établissement",
  auth = 'Authentification',
  account = 'Compte utilisateur',
  companyAccess = "Accès de l'entreprise",
  contractualDispute = 'Litige contractuel',
  dgccrf = 'DGCCRF',
}

export enum DgccrfEventActions {
  reportsSearch = 'Recherche de signalements',
}

export enum ReportEventActions {
  outOfBounds = "Affichage d'un message problème hors périmètre",
  information = "Consultation du détail d'un message d'information",
  secondaryCategories = 'Affichage des autres problèmes',
  validateCategory = "Sélection d'une catégorie",
  validateSubcategory = "Sélection d'une sous catégorie",
  employee = "Consommateur employé de l'entreprise",
  notEmployee = "Consommateur non employé de l'entreprise",
  validateDetails = 'Validation de la description',
  validateCompany = "Validation de l'établissement",
  validateConsumer = 'Validation du consommateur',
  validateConfirmation = "Validation de l'envoi d'un signalement",
  reportSendSuccess = "Envoi d'un signalement",
  reportSendFail = "Echec de l'envoi d'un signalement",
  keywordsDetection = 'Mots-clés détectés',
  informationFromKeywordsDetection = "Consultation du détail d'un message d'information suite à la détection de mots-clés",
  contactualReport = 'Litige contractuel',
}

export enum CompanySearchEventActions {
  search = 'Recherche',
  select = 'Sélection dans la liste de résultats',
  searchByIdentity = 'Recherche par SIRET / SIREN / RCS',
  searchByUrl = 'Recherche par URL',
}

export enum ContractualDisputeActions {
  consult = 'Consultation',
  downloadTemplate = 'Téléchargement lettre type',
}

export enum ContractualDisputeNames {
  step = 'Démarche',
}

export enum AuthenticationEventActions {
  success = 'Authentification réussie',
  role = 'Rôle de la personne authentifiée',
  fail = 'Authentification en échec',
  forgotPasswordSuccess = 'Mot de passe oublié - envoi du mail',
  forgotPasswordFail = 'Mot de passe oublié - erreur technique',
  resetPasswordSuccess = 'Réinitialistation du mot de passe',
  resetPasswordFail = 'Réinitialistation du mot de passe - erreur technique',
}

export enum AccountEventActions {
  changeNameSuccess = 'Changement de nom réussi',
  changeNameFail = 'Changement de nom échec',
  changePasswordSuccess = 'Changement mdp réussi',
  changePasswordFail = 'Changement mdp en échec',
  registerUser = "Inscription d'un utilisateur",
}

export enum AccountEventNames {
  userAlreadyRegistered = 'Compte déjà existant',
}

export enum AccessEventActions {
  addCompanyToAccount = "Ajout d'une entreprise à un compte",
  activateCompanyCode = "Activation d'une entreprise",
}

export enum ActionResultNames {
  success = 'Succès',
  fail = 'Echec',
}
