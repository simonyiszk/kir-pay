import axios, { AxiosResponse } from 'axios'
import { ErrorResultType, ValidatedApiCall } from '@/lib/api/model.ts'
import { getHashedColor } from '@/lib/utils.ts'

// Session-based auth: every request carries the SESSION cookie
axios.defaults.withCredentials = true

export const AppQueryKeys = {
  Accounts: 'Accounts',
  AdminAccounts: 'AdminAccounts',
  Analytics: 'Analytics',
  App: 'App',
  ConsumptionLeaderboard: 'ConsumptionLeaderboard',
  ItemLeaderboard: 'ItemLeaderboard',
  Events: 'Events',
  Items: 'Items',
  OrderLines: 'OrderLines',
  OrderWithOrderLines: 'OrderWithOrderLines',
  Orders: 'Orders',
  Principals: 'Principals',
  RevenueHeatmap: 'RevenueHeatmap',
  Sessions: 'Sessions',
  Transactions: 'Transactions',
  Vouchers: 'Vouchers'
} as const
export type AppQueryKeys = (typeof AppQueryKeys)[keyof typeof AppQueryKeys]

export const getApiRoot = () => `${window.config.BACKEND_URL}/v1/api`

const defaultColorMapper = <T extends { id?: number }>(data: T) => ({
  ...data,
  color: getHashedColor(data.id?.toString() ?? '')
})

export const addColorToResponse = <T extends object>(data: unknown, mapper: (data: T) => T = defaultColorMapper): T => mapper(data as T)

export const addColorToListResponse = <T>(data: unknown, selector?: (data: T) => string): T[] =>
  (data as T[]).map((entry) => ({
    ...entry,
    color: getHashedColor(selector ? selector(entry) : (entry as { id?: number }).id?.toString() || '')
  }))

const RETRYABLE_STATUS_CODES = new Set([409, 429, 500, 502, 503, 504])
const RETRYABLE_ERROR_CODES = new Set(['ERR_NETWORK', 'ETIMEDOUT', 'ECONNABORTED'])

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))

const httpStatusToErrorResultType = (status: number): ErrorResultType => {
  switch (status) {
    case 400:
      return 'BadRequest'
    case 401:
      return 'Unauthorized'
    case 403:
      return 'Forbidden'
    case 404:
      return 'NotFound'
    case 422:
      return 'UnprocessableEntity'
    default:
      return 'OtherError'
  }
}

const parseErrorBody = (data: unknown): { message?: string } | undefined => {
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch {
      return data ? { message: data } : undefined
    }
  }
  return (data as { message?: string } | null) ?? undefined
}

const MAX_RETRIES = 2
const BASE_DELAY_MS = 1000

const calculateBackoffDelay = (attempt: number): number => {
  const delay = Math.min(BASE_DELAY_MS * Math.pow(2, attempt - 1), 3000)
  return delay * (0.8 + Math.random() * 0.4)
}

const apiCall = async <T>({
  axiosCall,
  mapResponse,
  parseJson = true
}: {
  axiosCall: () => Promise<AxiosResponse>
  mapResponse?: (data: unknown) => T
  parseJson?: boolean
}): Promise<ValidatedApiCall<T>> => {
  const maxAttempts = MAX_RETRIES + 1

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const response = await axiosCall()
      if (response.status >= 200 && response.status < 300) {
        let data: T
        if (mapResponse) {
          data = mapResponse(response.data)
        } else {
          data = (parseJson ? (response.data ?? {}) : response.data) as T
        }
        return { result: 'Ok', data } satisfies ValidatedApiCall<T>
      }

      if (RETRYABLE_STATUS_CODES.has(response.status) && attempt < maxAttempts) {
        await sleep(calculateBackoffDelay(attempt))
        continue
      }

      const errorJson = parseErrorBody(response.data)

      return {
        error: response.status >= 500 ? 'Szerver hiba!' : errorJson?.message,
        result: httpStatusToErrorResultType(response.status)
      } satisfies ValidatedApiCall<T>
    } catch (err) {
      if (axios.isAxiosError(err)) {
        if (err.code && RETRYABLE_ERROR_CODES.has(err.code) && attempt < maxAttempts) {
          await sleep(calculateBackoffDelay(attempt))
          continue
        }
        return {
          result: 'OtherError',
          error: err.code === 'ERR_NETWORK' ? 'Hálózati hiba!' : String(err.message)
        } satisfies ValidatedApiCall<T>
      }
      return {
        result: 'OtherError',
        error: String(err)
      } satisfies ValidatedApiCall<T>
    }
  }

  throw new Error('Unreachable')
}

type HttpRequestParams<T, R> = {
  url: URL
  data?: T
  mapResponse?: (res: unknown) => R
  parseJson?: boolean
}

const httpRequest = <T, R>(
  method: 'get' | 'post' | 'put' | 'delete',
  { url, data, mapResponse, parseJson = true }: HttpRequestParams<T, R>
): Promise<ValidatedApiCall<R>> => {
  const headers: Record<string, string> = {}
  if (data !== undefined && !parseJson) {
    headers['Content-Type'] = 'text/plain'
  }

  return apiCall({
    axiosCall: () =>
      axios.request({
        method,
        url: url.toString(),
        data: parseJson ? data : data !== null && data !== undefined ? (typeof data === 'string' ? data : JSON.stringify(data)) : undefined,
        headers,
        validateStatus: () => true,
        ...(!parseJson ? { responseType: 'text' } : {})
      }),
    mapResponse,
    parseJson
  })
}

export const httpGet = <R>(params: HttpRequestParams<undefined, R>): Promise<ValidatedApiCall<R>> => httpRequest('get', params)
export const httpPost = <T, R>(params: HttpRequestParams<T, R>): Promise<ValidatedApiCall<R>> => httpRequest('post', params)
export const httpPut = <T, R>(params: HttpRequestParams<T, R>): Promise<ValidatedApiCall<R>> => httpRequest('put', params)
export const httpDelete = <R>(params: HttpRequestParams<undefined, R>): Promise<ValidatedApiCall<R>> => httpRequest('delete', params)

export const login = (username: string, password: string) =>
  httpPost<{ username: string; password: string }, undefined>({
    url: new URL(`${getApiRoot()}/login`),
    data: { username, password }
  })

export const logout = () =>
  httpPost<undefined, undefined>({
    url: new URL(`${getApiRoot()}/logout`),
    data: undefined,
    parseJson: false
  })
