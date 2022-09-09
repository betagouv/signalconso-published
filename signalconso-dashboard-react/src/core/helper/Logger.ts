export class ApiSdkLogger {
  static readonly error = (...data: any[]) => console.error('[SignalConsoApiSdk]', ...data)
}
