import {ApiClientApi} from '../ApiClient'
import {AsyncFile, AsyncFileStatus} from './AsyncFile'
import {addHours} from 'date-fns'

export class AsyncFileClient {
  constructor(private client: ApiClientApi) {}

  private static readonly fileGenerationTimeoutHours = 24

  readonly fetch = () => {
    return this.client.get<AsyncFile[]>(`/async-files`).then(result =>
      result.map(_ => {
        const creationDate = new Date(_.creationDate)
        return {
          ..._,
          creationDate,
          status: AsyncFileClient.getStatus(_),
        }
      }),
    )
  }

  private static readonly getStatus = (file: AsyncFile): AsyncFileStatus => {
    if (file.filename) {
      return AsyncFileStatus.Successed
    }
    const creationDate = new Date(file.creationDate)
    return new Date().getTime() > addHours(creationDate, AsyncFileClient.fileGenerationTimeoutHours).getTime()
      ? AsyncFileStatus.Failed
      : AsyncFileStatus.Loading
  }
}
