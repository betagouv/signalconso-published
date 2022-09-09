import {cleanReportFilter, reportFilter2QueryString} from 'core/client/report/ReportsClient'
import {subDays} from 'date-fns'
import {Duration, duration} from '../../../alexlibs/ts-utils'
import {cleanObject, roundValue, toNumberOrDefault} from '../../helper'
import {
  CountByDate,
  CurveStatsParams,
  Id,
  Report,
  ReportResponseStatsParams,
  ReportSearch,
  ReportStatus,
  ReportStatusProDistribution,
} from '../../model'
import {ApiClientApi} from '../ApiClient'
import {ReportResponseReviews, ReportStatusDistribution, ReportTagsDistribution, SimpleStat} from './Stats'

// All of this could be greatly simplified, if we just fully compute the stats server-side
// Like it's done with the 'PublicStat' on the website

export class StatsClient {
  constructor(private client: ApiClientApi) {}

  readonly percentage = new StatsPercentageClient(this)

  readonly percentageCurve = new StatsCurveClient(this)

  readonly getReportCount = (filters?: ReportSearch) => {
    const qs = filters && cleanObject(reportFilter2QueryString(cleanReportFilter(filters)))
    return this.client.get<SimpleStat>(`stats/reports/count`, {qs})
  }
  readonly getReportCountCurve = (search?: ReportSearch & CurveStatsParams) => {
    return this.client
      .get<CountByDate[]>(`stats/reports/curve`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getTags = (companyId: Id) => {
    return this.client.get<ReportTagsDistribution>(`/stats/reports/tags`, {qs: {companyId}})
  }

  readonly getStatus = (companyId: Id) => {
    return this.client.get<ReportStatusDistribution>(`/stats/reports/status`, {qs: {companyId}})
  }

  readonly getProStatus = (companyId: Id): Promise<ReportStatusProDistribution> => {
    return this.client.get<ReportStatusDistribution>(`/stats/reports/status`, {qs: {companyId}}).then(
      _ =>
        <ReportStatusProDistribution>{
          ARepondre: toNumberOrDefault(_.Transmis, 0),
          NonConsulte: toNumberOrDefault(_.TraitementEnCours, 0),
          Cloture:
            toNumberOrDefault(_.PromesseAction, 0) +
            toNumberOrDefault(_.Infonde, 0) +
            toNumberOrDefault(_.NonConsulte, 0) +
            toNumberOrDefault(_.ConsulteIgnore, 0) +
            toNumberOrDefault(_.MalAttribue, 0),
        },
    )
  }

  readonly getResponseReviews = (companyId: Id) => {
    return this.client.get<ReportResponseReviews>(`/stats/reports/reviews`, {qs: {companyId}})
  }

  readonly getReportedInactiveProAccountRate = (search?: CurveStatsParams) => {
    return this.client
      .get<CountByDate[]>(`/stats/pro-account-rate`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getProReportToTransmitStat = () => {
    return this.client
      .get<CountByDate[]>(`/stats/reports/pro-totransmit`)
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getProReportTransmittedStat = (search?: CurveStatsParams) => {
    return this.client
      .get<CountByDate[]>(`/stats/reports/pro-transmitted`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getProReportResponseStat = (search?: ReportResponseStatsParams) => {
    return this.client
      .get<CountByDate[]>(`/stats/reports/pro-response`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getActiveDgccrfAccountCurve = (search?: CurveStatsParams) => {
    return this.client
      .get<CountByDate[]>(`/stats/dgccrf-active-account`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getDgccrfAccountCurve = (search?: CurveStatsParams) => {
    return this.client
      .get<CountByDate[]>(`/stats/dgccrf-account`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getDgccrfControlsCurve = (search?: CurveStatsParams) => {
    return this.client
      .get<CountByDate[]>(`/stats/dgccrf-controls`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getDgccrfSubscriptionsCurve = (search?: CurveStatsParams) => {
    return this.client
      .get<CountByDate[]>(`/stats/dgccrf-subscriptions`, {qs: search})
      .then(res => res.map(_ => ({..._, date: new Date(_.date)})))
  }

  readonly getReadDelay = (companyId: Id): Promise<Duration | undefined> => {
    return this.client
      .get<{value: number | undefined}>(`/stats/reports/delay/read`, {qs: {companyId}})
      .then(_ => (_.value ? duration(_.value, 'hour') : undefined))
  }

  readonly getResponseDelay = (companyId: Id): Promise<Duration | undefined> => {
    return this.client
      .get<{value: number | undefined}>(`/stats/reports/delay/responsed`, {qs: {companyId}})
      .then(_ => (_.value ? duration(_.value, 'hour') : undefined))
  }
}

class StatsPercentageClient {
  constructor(private client: StatsClient) {}

  private readonly delayBeforeCountingToWaitForProResponseInDays = 30

  private readonly statsAdminStartDate = new Date('2019-01-01')

  private readonly getPercentByStatus = async ({
    companyId,
    status,
    baseStatus,
    start,
    end,
  }: {
    companyId?: Id
    status: ReportStatus[]
    baseStatus?: ReportStatus[]
    start?: Date
    end?: Date
  }): Promise<SimpleStat> => {
    const [count, baseCount] = await Promise.all([
      this.client.getReportCount({start, end, status, ...(companyId ? {companyIds: [companyId]} : {})}),
      this.client.getReportCount({start, end, status: baseStatus, ...(companyId ? {companyIds: [companyId]} : {})}),
    ])
    return {value: roundValue((+count.value / +baseCount.value) * 100)}
  }
  readonly getReportForwardedToPro = (companyId?: Id): Promise<SimpleStat> => {
    return this.getPercentByStatus({
      companyId,
      status: Report.transmittedStatus,
      start: this.statsAdminStartDate,
      end: subDays(new Date(), this.delayBeforeCountingToWaitForProResponseInDays),
    })
  }

  readonly getReportReadByPro = (companyId?: Id) => {
    return this.getPercentByStatus({
      companyId,
      status: Report.readStatus,
      baseStatus: Report.transmittedStatus,
      start: this.statsAdminStartDate,
      end: subDays(new Date(), this.delayBeforeCountingToWaitForProResponseInDays),
    })
  }

  readonly getReportWithResponse = (companyId?: Id) => {
    return this.getPercentByStatus({
      companyId,
      status: Report.respondedStatus,
      baseStatus: Report.readStatus,
      start: this.statsAdminStartDate,
      end: subDays(new Date(), this.delayBeforeCountingToWaitForProResponseInDays),
    })
  }

  readonly getReportWithWebsite = async (companyId?: Id): Promise<SimpleStat> => {
    const [count, baseCount] = await Promise.all([
      this.client.getReportCount({
        hasWebsite: true,
        start: this.statsAdminStartDate,
        end: subDays(new Date(), this.delayBeforeCountingToWaitForProResponseInDays),
        ...(companyId ? {companyIds: [companyId]} : {}),
      }),
      this.client.getReportCount({
        start: this.statsAdminStartDate,
        end: subDays(new Date(), this.delayBeforeCountingToWaitForProResponseInDays),
        ...(companyId ? {companyIds: [companyId]} : {}),
      }),
    ])
    return {value: roundValue((+count.value / +baseCount.value) * 100)}
  }
}

class StatsCurveClient {
  constructor(private client: StatsClient) {}

  private readonly getReportPercentageCurve = async ({
    companyId,
    ticks,
    tickDuration,
    status,
    baseStatus,
  }: CurveStatsParams & {companyId?: Id; status: ReportStatus[]; baseStatus?: ReportStatus[]}): Promise<CountByDate[]> => {
    const params = {
      status,
      ticks,
      tickDuration,
      ...(companyId ? {companyIds: [companyId]} : {}),
    }
    const baseParams = {
      status: baseStatus,
      ticks,
      tickDuration,
      ...(companyId ? {companyIds: [companyId]} : {}),
    }
    const [curve, baseCurve] = await Promise.all([
      this.client.getReportCountCurve(params),
      this.client.getReportCountCurve(baseParams),
    ])
    if (curve.length !== baseCurve.length) {
      console.error(params, curve, `doesn't have the same size than `, baseParams, baseCurve)
      return Promise.reject({code: 'front-side'})
    }
    return this.getPercent(curve, baseCurve)
  }

  private getPercent = (curve: CountByDate[], baseCurve: CountByDate[]): Promise<CountByDate[]> => {
    let res: CountByDate[] = []
    for (let i = 0; i < curve.length; i++) {
      if (curve[i].date.getTime() !== baseCurve[i].date.getTime()) {
        console.error(curve[i], `have different date than`, baseCurve[i], ' values: ', curve, baseCurve)
        return Promise.reject({code: 'front-side'})
      }
      res[i] = {
        count: roundValue((curve[i].count / baseCurve[i].count) * 100),
        date: curve[i].date,
      }
    }
    return Promise.resolve(res)
  }

  readonly getReportForwardedPercentage = async (params: CurveStatsParams & {companyId?: Id}): Promise<CountByDate[]> => {
    return this.getReportPercentageCurve({
      ...params,
      status: Report.transmittedStatus,
    })
  }

  readonly getReportRespondedPercentage = (params: CurveStatsParams & {companyId?: Id}) => {
    return this.getReportPercentageCurve({
      ...params,
      status: Report.respondedStatus,
      baseStatus: Report.readStatus,
    })
  }

  readonly getReportReadPercentage = (params: CurveStatsParams & {companyId?: Id}) => {
    return this.getReportPercentageCurve({
      ...params,
      status: Report.readStatus,
      baseStatus: Report.transmittedStatus,
    })
  }
}
