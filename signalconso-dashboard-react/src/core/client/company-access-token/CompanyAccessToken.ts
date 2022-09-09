import {CompanyAccessLevel} from '../company-access/CompanyAccess'
import {Id} from '../../model'

export interface CompanyAccessToken {
  id: Id
  level: CompanyAccessLevel
  emailedTo?: string
  expirationDate: Date
  token?: string
}
