import {UploadedFile} from './UploadedFile'
import {ApiClientApi} from '../ApiClient'

export class SecuredFileClient {
  constructor(private client: ApiClientApi) {}

  readonly remove = (file: UploadedFile) => {
    return this.client.delete(`/reports/files/${file.id}/${file.filename}`)
  }
}
