import { loginToken } from './auth.fixture'

const BACKEND_URL = 'http://localhost:8001'
const ADMIN_USER = 'admin'
const ADMIN_PASS = 'admin'

let adminTokenPromise: Promise<string> | undefined
function adminToken(): Promise<string> {
  adminTokenPromise ??= loginToken(ADMIN_USER, ADMIN_PASS)
  return adminTokenPromise
}

function uniqueSuffix(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`
}

export function randomUUID(): string {
  return crypto.randomUUID()
}

export interface ApiResponse<T = unknown> {
  status: number
  body: T
  ok: boolean
}

async function fetchJson<T = unknown>(
  method: string,
  url: string,
  opts: { headers: Record<string, string>; body?: unknown }
): Promise<ApiResponse<T>> {
  const init: RequestInit = {
    method,
    headers: opts.headers
  }
  if (opts.body !== undefined) {
    init.body = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body)
  }
  const res = await fetch(url, init)
  const contentType = res.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')
  const body = isJson ? await res.json() : await res.text()
  return { status: res.status, body: body as T, ok: res.ok }
}

async function fetchText(
  method: string,
  url: string,
  opts: { headers: Record<string, string>; body?: unknown }
): Promise<ApiResponse<string>> {
  const init: RequestInit = {
    method,
    headers: opts.headers
  }
  if (opts.body !== undefined) {
    init.body = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body)
  }
  const res = await fetch(url, init)
  return { status: res.status, body: await res.text(), ok: res.ok }
}

export interface ApiClient {
  get: <T = unknown>(path: string, params?: Record<string, string | number | undefined>) => Promise<ApiResponse<T>>
  post: <T = unknown>(path: string, body?: unknown) => Promise<ApiResponse<T>>
  put: <T = unknown>(path: string, body?: unknown) => Promise<ApiResponse<T>>
  del: <T = unknown>(path: string) => Promise<ApiResponse<T>>
  getText: (path: string, params?: Record<string, string | number | undefined>) => Promise<ApiResponse<string>>
  postText: <T = unknown>(path: string, csvBody: string, params?: Record<string, string | number | undefined>) => Promise<ApiResponse<T>>
}

export function apiClient(basePath: string, token: string | (() => Promise<string>)): ApiClient {
  const resolveToken = async (): Promise<string> => (typeof token === 'function' ? await token() : token)

  const authHeaders = async (contentType: string): Promise<Record<string, string>> => {
    const headers: Record<string, string> = { 'Content-Type': contentType }
    const sessionToken = await resolveToken()
    if (sessionToken) headers['Cookie'] = `SESSION=${sessionToken}`
    return headers
  }

  const buildUrl = (path: string, params?: Record<string, string | number | undefined>): string => {
    const url = new URL(`${BACKEND_URL}${basePath}${path}`)
    if (params) {
      Object.entries(params)
        .filter(([, v]) => v !== undefined)
        .forEach(([k, v]) => url.searchParams.append(k, String(v)))
    }
    return url.toString()
  }

  return {
    get: async <T>(path: string, params?: Record<string, string | number | undefined>) =>
      fetchJson<T>('GET', buildUrl(path, params), { headers: await authHeaders('application/json') }),

    post: async <T>(path: string, body?: unknown) =>
      fetchJson<T>('POST', buildUrl(path), { headers: await authHeaders('application/json'), body }),

    put: async <T>(path: string, body?: unknown) =>
      fetchJson<T>('PUT', buildUrl(path), { headers: await authHeaders('application/json'), body }),

    del: async <T>(path: string) => fetchJson<T>('DELETE', buildUrl(path), { headers: await authHeaders('application/json') }),

    getText: async (path: string, params?: Record<string, string | number | undefined>) =>
      fetchText('GET', buildUrl(path, params), { headers: await authHeaders('application/json') }),

    postText: async <T>(path: string, csvBody: string, params?: Record<string, string | number | undefined>) =>
      fetchJson<T>('POST', buildUrl(path, params), { headers: await authHeaders('text/plain'), body: csvBody })
  }
}

function adminClient(): ApiClient {
  return apiClient('/v1/api/admin', adminToken)
}

export interface E2EAccount {
  id: number
  name: string
  email?: string | null
  phone?: string | null
  card?: string | null
  balance: number
  active: boolean
  version: number
}

export interface E2EItem {
  id: number
  name: string
  alias?: string | null
  cost: number
  stock: number
  enabled: boolean
  showOnLeaderboard?: boolean
}

export interface E2EPrincipal {
  id: number
  name: string
  role: 'ADMIN' | 'TERMINAL'
  active: boolean
  canUpload: boolean
  canTransfer: boolean
  canSellItems: boolean
  canRedeemVouchers: boolean
  canAssignCards: boolean
  createdAt?: number
  lastUsed?: number
}

export interface E2EVoucher {
  id: number
  accountId: number
  itemId: number
  count: number
}

export async function createAccount(
  overrides: Partial<{
    name: string
    email: string
    phone: string
    balance: number
    active: boolean
    card: string
  }> = {}
): Promise<E2EAccount> {
  const body = {
    name: overrides.name ?? `E2E Test ${uniqueSuffix()}`,
    email: overrides.email ?? null,
    phone: overrides.phone ?? null,
    balance: overrides.balance ?? 1000,
    active: overrides.active ?? true,
    card: overrides.card ?? `CARD-${uniqueSuffix()}`,
    idempotencyKey: randomUUID()
  }
  const { body: account } = await adminClient().post<E2EAccount>('/accounts', body)
  return account
}

function deleteAccount(id: number): Promise<ApiResponse<unknown>> {
  return adminClient().del(`/accounts/${id}`)
}

export async function createItem(
  overrides: Partial<{
    name: string
    alias: string
    cost: number
    stock: number
    enabled: boolean
  }> = {}
): Promise<E2EItem> {
  const body = {
    name: overrides.name ?? `E2E Item ${uniqueSuffix()}`,
    alias: overrides.alias ?? null,
    cost: overrides.cost ?? 500,
    stock: overrides.stock ?? 100,
    enabled: overrides.enabled ?? true,
    idempotencyKey: randomUUID()
  }
  const { body: item } = await adminClient().post<E2EItem>('/items', body)
  return item
}

function deleteItem(id: number): Promise<ApiResponse<unknown>> {
  return adminClient().del(`/items/${id}`)
}

export async function createPrincipal(
  overrides: Partial<{
    name: string
    password: string
    role: 'ADMIN' | 'TERMINAL'
    canUpload: boolean
    canTransfer: boolean
    canSellItems: boolean
    canRedeemVouchers: boolean
    canAssignCards: boolean
    active: boolean
  }> = {}
): Promise<E2EPrincipal> {
  const body = {
    name: overrides.name ?? `e2e-term-${uniqueSuffix()}`,
    password: overrides.password ?? 'e2e-test-pw',
    role: overrides.role ?? 'TERMINAL',
    canUpload: overrides.canUpload ?? true,
    canTransfer: overrides.canTransfer ?? true,
    canSellItems: overrides.canSellItems ?? true,
    canRedeemVouchers: overrides.canRedeemVouchers ?? true,
    canAssignCards: overrides.canAssignCards ?? true,
    active: overrides.active ?? true,
    idempotencyKey: randomUUID()
  }
  const { body: principal } = await adminClient().post<E2EPrincipal>('/principals', body)
  return principal
}

function deletePrincipal(id: number): Promise<ApiResponse<unknown>> {
  return adminClient().del(`/principals/${id}`)
}

export async function createVoucher(overrides: { accountId: number; itemId: number; count?: number }): Promise<E2EVoucher> {
  const body = {
    accountId: overrides.accountId,
    itemId: overrides.itemId,
    count: overrides.count ?? 1
  }
  const { body: voucher } = await adminClient().post<E2EVoucher>('/vouchers', body)
  return voucher
}

function deleteVoucher(id: number): Promise<ApiResponse<unknown>> {
  return adminClient().del(`/vouchers/${id}`)
}

function terminalClient(): ApiClient {
  return apiClient('/v1/api/terminal', adminToken)
}

export async function payByCard(card: string, amount: number, idempotencyKey: string): Promise<ApiResponse<unknown>> {
  return terminalClient().post(`/account-by-card/${card}/pay`, { amount, idempotencyKey })
}

export function createCleanupTracker() {
  const accountIds: number[] = []
  const itemIds: number[] = []
  const principalIds: number[] = []
  const voucherIds: number[] = []

  return {
    trackAccount(p: Promise<{ id: number }>): Promise<{ id: number }> {
      return p.then((a) => {
        accountIds.push(a.id)
        return a
      })
    },
    trackItem(p: Promise<{ id: number }>): Promise<{ id: number }> {
      return p.then((i) => {
        itemIds.push(i.id)
        return i
      })
    },
    trackPrincipal(p: Promise<{ id: number }>): Promise<{ id: number }> {
      return p.then((p) => {
        principalIds.push(p.id)
        return p
      })
    },
    trackVoucher(p: Promise<{ id: number }>): Promise<{ id: number }> {
      return p.then((v) => {
        voucherIds.push(v.id)
        return v
      })
    },
    async run(): Promise<void> {
      await Promise.all([
        ...accountIds.map((id) => deleteAccount(id).catch(() => {})),
        ...itemIds.map((id) => deleteItem(id).catch(() => {})),
        ...principalIds.map((id) => deletePrincipal(id).catch(() => {})),
        ...voucherIds.map((id) => deleteVoucher(id).catch(() => {}))
      ])
    }
  }
}
