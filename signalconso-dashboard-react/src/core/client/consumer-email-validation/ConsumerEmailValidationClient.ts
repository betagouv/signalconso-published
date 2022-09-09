import {ConsumerEmailResult, ConsumerEmailValidation, ConsumerEmailValidationSearch} from './ConsumerEmailValidation'
import {Paginate} from '../../model'
import {cleanObject} from '../../helper'
import {ApiClientApi} from '../ApiClient'

export class ConsumerEmailValidationClient {
  constructor(private client: ApiClientApi) {}

  readonly validate = (email: string) => {
    return this.client.post<ConsumerEmailResult>('/email-validation/validate', {body: {email}})
  }

  readonly search = (search: ConsumerEmailValidationSearch): Promise<Paginate<ConsumerEmailValidation>> => {
    return this.client
      .get<Paginate<{[key in keyof ConsumerEmailValidation]: any}>>('/email-validation/search', {qs: cleanObject(search)})
      .then(res => ({
        ...res,
        entities: res.entities.map(_ => ({
          ..._,
          lastValidationDate: _.lastValidationDate ? new Date(_.lastValidationDate) : undefined,
          lastAttempt: _.lastAttempt ? new Date(_.lastAttempt) : undefined,
          creationDate: new Date(_.creationDate),
        })),
      }))
  }
}
