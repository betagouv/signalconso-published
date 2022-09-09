import {config} from 'conf/config'
import {SxProps, Theme} from '@mui/material'
import format from 'date-fns/format'
import {ScOption} from './ScOption'

export type Index<T> = {[key: string]: T}

export type Shape<T extends object> = {[key in keyof T]: any}

export const dateToApiDate = (date?: Date): string | undefined => (date ? format(date, 'yyyy-MM-dd') : undefined)

export const dateToApiTime = (date?: Date): string | undefined => (date ? date.toISOString() : undefined)

export const getHostFromUrl = (url?: string) => {
  return url?.replace('http://', '').replace('https://', '').replace('www.', '').split(/[/?#]/)[0]
}

export const isNotDefined = (value: any): value is undefined | null | '' => {
  return [undefined, null, ''].includes(value)
}

export const isDefined = <T>(value: T | undefined | null | ''): value is T => !isNotDefined(value)

export const emptyStringToUndefined = (value: string): string | undefined => (value.length === 0 ? undefined : value)

export const toNumberOrDefault = (value: any, defaultValue: number): number =>
  isNaN(value) || value == '' || value == null ? defaultValue : value

export const cleanObject = <T extends {[key: string]: any}>(obj: T): Partial<T> => {
  const clone = {...obj}
  for (let k in clone) {
    const val = clone[k]
    if (isNotDefined(val) || (Array.isArray(val) && val.filter(isDefined).length === 0)) {
      delete clone[k]
    }
  }
  return clone
}

export const roundValue = (_: number): number => Math.round(_)

export const toQueryString = (obj: any): string => {
  if (!obj) return ''
  return (
    '?' +
    Object.keys(obj)
      .filter(k => obj[k] !== undefined)
      .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(obj[k])}`)
      .join('&')
  )
}

export const directDownloadBlob =
  (fileName: string) =>
  (blob: Blob): void => {
    const url = window.URL.createObjectURL(new Blob([blob], {type: 'application/pdf'}))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
  }

export const isJsonValid = (json: string): boolean => {
  try {
    JSON.parse(json)
    return true
  } catch (e) {
    return false
  }
}

export const textOverflowMiddleCropping = (text: string, limit: number) => {
  return text.length > limit ? `${text.slice(0, limit / 2)}...${text.slice(text.length - limit / 2, text.length)}` : text
}

export const fromQueryString = <T = object>(qs: string): {[key in keyof T]: string | number} => {
  const decoded = decodeURI(qs.replace(/^\?/, '')).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g, '":"')
  const json: Index<string> = JSON.parse(`{${decoded}}`)
  return Object.entries(json).reduce(
    (acc, [key, value]) => ({...acc, [key]: Number(value) ?? value}),
    {} as {[key in keyof T]: string | number},
  )
}

export const stopPropagation =
  <
    E extends {
      preventDefault?: () => void
      stopPropagation?: () => void
    },
  >(
    action: (event: E) => any,
  ) =>
  (event: E) => {
    event.stopPropagation?.()
    event.preventDefault?.()
    action(event)
  }

export const capitalize = (str?: string, othersInLowerCase = true): string | undefined =>
  ScOption.from(str)
    .map(_ => _.charAt(0).toUpperCase() + (othersInLowerCase ? _.slice(1).toLowerCase() : _.slice(1)))
    .getOrElse(undefined)

interface FnSwitch {
  <T extends string | number | symbol, R = any>(value: T, cases: {[key in T]: ((_: T) => R) | R}): R
  <T extends string | number | symbol, R = any>(
    value: T,
    cases: Partial<{[key in T]: ((_: T) => R) | R}>,
    defaultCase: (_: T) => R,
  ): R
}

export const fnSwitch: FnSwitch = (value, cases, defaultCase?) => {
  const res = cases[value]
  if (!res && !defaultCase) {
    throw new Error(
      `[fnSwitch] ${String(value)} does not match any of theses cases ${Object.keys(cases).join(
        ', ',
      )} defaultCase parameter is not provided.`,
    )
  }
  return (typeof res === 'function' ? res(value) : res) ?? (defaultCase as any)!(value)
}

export const siretToSiren = (siret: string) => siret.slice(0, 9)

export const stringToBoolean = (str?: string): boolean | undefined => {
  if (str) {
    if (str === 'true') return true
    else if (str === 'false') return false
  }
}

export const getAbsoluteLocalUrl = (path: string) => {
  return (config.useHashRouter ? '/#' : '') + path
}

export const openInNew = (path: string) => {
  window.open(getAbsoluteLocalUrl(path), '_blank')
}

export const sxIf = (condition: boolean | undefined, sx: SxProps<Theme>): SxProps<Theme> => {
  return condition ? sx : {}
}

export const countryToFlag = (isoCode: string) => {
  return typeof String.fromCodePoint !== 'undefined'
    ? isoCode.toUpperCase().replace(/./g, char => String.fromCodePoint(char.charCodeAt(0) + 127397))
    : isoCode
}

export const null2Undefined = <A>(value: A | null): A | undefined => (value === null ? undefined : value)

export const undefined2Null = <A>(value: A | undefined): A | null => (value === undefined ? null : value)
