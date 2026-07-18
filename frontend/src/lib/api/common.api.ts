import { ErrorResultType, ValidatedApiCall } from '@/lib/api/model.ts'
import { getHashedColor } from '@/lib/utils.ts'

export const AppQueryKeys = {
  App: 'App',
  Accounts: 'Accounts',
  Items: 'Items',
  Principals: 'Principals',
  Orders: 'Orders',
  OrderLines: 'OrderLines',
  OrderWithOrderLines: 'OrderWithOrderLines',
  Vouchers: 'Vouchers',
  Events: 'Events',
  Transactions: 'Transactions',
  Analytics: 'Analytics'
} as const
export type AppQueryKeys = (typeof AppQueryKeys)[keyof typeof AppQueryKeys]

export const createToken = (username: string, password: string) => `Basic ${btoa(`${username}:${password}`)}`

export const getApiRoot = () => `${window.config.BACKEND_URL}/v1/api`

const defaultColorMapper = <T extends { id?: number }>(data: T) => ({
  ...data,
  color: getHashedColor(data.id?.toString() ?? '')
})

export const addColorToResponse = <T extends object>(res: Response, mapper: (data: T) => T = defaultColorMapper): Promise<T> =>
  res.json().then(mapper)

export const addColorToListResponse = <
  T extends {
    id?: number
  }
>(
  res: Response,
  selector?: (data: T) => string
): Promise<T[]> =>
  res.json().then((data: T[]) =>
    data.map((entry) => ({
      ...entry,
      color: getHashedColor(selector ? selector(entry) : entry.id?.toString() || '')
    }))
  )

const httpStatusToErrorResultType = (status: number): ErrorResultType => {
  switch (status) {
    case 404:
      return 'NotFound'
    case 400:
      return 'BadRequest'
    case 401:
      return 'Unauthorized'
    case 403:
      return 'Forbidden'
    default:
      return 'OtherError'
  }
}

const apiCall = <T>({
  apiCall,
  mapResponse,
  parseJson = true
}: {
  apiCall: Promise<Response>
  mapResponse?: (res: Response) => Promise<T>
  parseJson: boolean
}): Promise<ValidatedApiCall<T>> =>
  apiCall
    .then(async (res) => {
      if (res.ok) {
        let data: T
        if (mapResponse) {
          data = await mapResponse(res)
        } else {
          data = parseJson ? await res.json().catch(() => ({})) : await res.text()
        }
        return { result: 'Ok', data } satisfies ValidatedApiCall<T>
      }

      const errorJson = await res.json().catch(() => undefined)

      return {
        error: errorJson?.message,
        result: httpStatusToErrorResultType(res.status)
      } satisfies ValidatedApiCall<T>
    })
    .catch(
      () =>
        ({
          result: 'OtherError'
        }) satisfies ValidatedApiCall<T>
    )

export const httpPost = <T, R>({
  url,
  data,
  token,
  mapResponse,
  parseJson = true
}: {
  url: URL
  data: T
  token: string
  mapResponse?: (res: Response) => Promise<R>
  parseJson?: boolean
}): Promise<ValidatedApiCall<R>> =>
  apiCall({
    apiCall: fetch(url, {
      method: 'POST',
      mode: 'cors',
      headers: { Authorization: token, 'Content-Type': parseJson ? 'application/json' : 'text/plain' },
      body: parseJson ? JSON.stringify(data) : data !== null && data !== undefined ? data + '' : undefined
    }),
    mapResponse,
    parseJson
  })

export const httpGet = <R>({
  url,
  token,
  mapResponse,
  parseJson = true
}: {
  url: URL
  token: string
  mapResponse?: (res: Response) => Promise<R>
  parseJson?: boolean
}): Promise<ValidatedApiCall<R>> =>
  apiCall({
    apiCall: fetch(url, {
      method: 'GET',
      mode: 'cors',
      headers: { Authorization: token }
    }),
    mapResponse,
    parseJson
  })

export const httpDelete = <R>({
  url,
  token,
  mapResponse,
  parseJson = true
}: {
  url: URL
  token: string
  mapResponse?: (res: Response) => Promise<R>
  parseJson?: boolean
}): Promise<ValidatedApiCall<R>> =>
  apiCall({
    apiCall: fetch(url, {
      method: 'DELETE',
      mode: 'cors',
      headers: { Authorization: token }
    }),
    mapResponse,
    parseJson
  })
