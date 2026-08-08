import { addColorToListResponse, addColorToResponse, getApiRoot, httpDelete, httpGet, httpPost, httpPut } from '@/lib/api/common.api.ts'
import {
  Account,
  AccountCreateDto,
  AnalyticsDto,
  BatchVoucherCreateDto,
  ConsumptionLeaderboardEntry,
  Event,
  Item,
  ItemCreateDto,
  ItemLeaderboardEntry,
  OrderWithOrderLine,
  Principal,
  PrincipalCreateDto,
  PrincipalDto,
  SessionInfo,
  Transaction,
  Voucher,
  VoucherCountDto,
  VoucherDeltaDto,
  VoucherImportResult
} from '@/lib/api/model.ts'

const getUrl = (endpoint: string, params?: object) => {
  const url = new URL(`${getApiRoot()}/admin/${endpoint}`)
  Object.entries(params || {})
    .filter((param) => param[1] !== undefined)
    .forEach((param) => url.searchParams.append(param[0], param[1]))
  return url
}

export const getAnalytics = () => httpGet<AnalyticsDto>({ url: getUrl('analytics') })

export const findAllPrincipals = () => httpGet<Principal[]>({ url: getUrl('principals') })

export const createPrincipal = (data: PrincipalCreateDto) => httpPost<PrincipalCreateDto, Principal>({ url: getUrl('principals'), data })

export const updatePrincipal = (principalId: number, data: PrincipalDto) =>
  httpPost<PrincipalDto, Principal>({ url: getUrl(`principals/${principalId}`), data })

export const deletePrincipal = (principalId: number) =>
  httpDelete<undefined>({ url: getUrl(`principals/${principalId}`), parseJson: false })

export const enablePrincipal = (principalId: number) =>
  httpPost<undefined, Principal>({ url: getUrl(`principals/${principalId}/enable`), data: undefined })

export const disablePrincipal = (principalId: number) =>
  httpPost<undefined, Principal>({ url: getUrl(`principals/${principalId}/disable`), data: undefined })

export const exportPrincipalTemplate = () => httpGet<string>({ url: getUrl('template/principals'), parseJson: false })

export const exportPrincipals = () => httpGet<string>({ url: getUrl('export/principals'), parseJson: false })

export const importPrincipals = (csv: string, idempotencyKey: string) =>
  httpPost<string, undefined>({ url: getUrl('import/principals', { idempotencyKey }), data: csv, parseJson: false })

export const findAllItems = () =>
  httpGet<Item[]>({
    url: getUrl('items'),
    mapResponse: addColorToListResponse
  })

export const findItemById = (itemId: number) =>
  httpGet<Item>({
    url: getUrl(`items/${itemId}`),
    mapResponse: addColorToResponse
  })

export const createItem = (data: ItemCreateDto) =>
  httpPost<ItemCreateDto, Item>({ url: getUrl('items'), data, mapResponse: addColorToResponse })

export const updateItem = (itemId: number, data: Item) =>
  httpPut<Item, Item>({ url: getUrl(`items/${itemId}`), data, mapResponse: addColorToResponse })

export const deleteItem = (itemId: number) => httpDelete<undefined>({ url: getUrl(`items/${itemId}`), parseJson: false })

export const enableItem = (itemId: number) =>
  httpPost<undefined, Item>({
    url: getUrl(`items/${itemId}/enable`),
    data: undefined,
    mapResponse: addColorToResponse
  })

export const disableItem = (itemId: number) =>
  httpPost<undefined, Item>({
    url: getUrl(`items/${itemId}/disable`),
    data: undefined,
    mapResponse: addColorToResponse
  })

export const exportItemTemplate = () => httpGet<string>({ url: getUrl('template/items'), parseJson: false })

export const exportItems = () => httpGet<string>({ url: getUrl('export/items'), parseJson: false })

export const importItems = (csv: string, idempotencyKey: string) =>
  httpPost<string, undefined>({ url: getUrl('import/items', { idempotencyKey }), data: csv, parseJson: false })

export const createAccount = (data: AccountCreateDto) =>
  httpPost<AccountCreateDto, Account>({ url: getUrl('accounts'), data, mapResponse: addColorToResponse })

export const updateAccount = (accountId: number, data: Account) =>
  httpPost<Account, Account>({
    url: getUrl(`accounts/${accountId}`),
    data,
    mapResponse: addColorToResponse
  })

export const deleteAccount = (accountId: number) => httpDelete<undefined>({ url: getUrl(`accounts/${accountId}`), parseJson: false })

export const enableAccount = (accountId: number) =>
  httpPost<undefined, Account>({
    url: getUrl(`accounts/${accountId}/enable`),
    data: undefined,
    mapResponse: addColorToResponse
  })

export const disableAccount = (accountId: number) =>
  httpPost<undefined, Account>({
    url: getUrl(`accounts/${accountId}/disable`),
    data: undefined,
    mapResponse: addColorToResponse
  })

export const exportAccountTemplate = () => httpGet<string>({ url: getUrl('template/accounts'), parseJson: false })

export const exportAccounts = () => httpGet<string>({ url: getUrl('export/accounts'), parseJson: false })

export const importAccounts = (csv: string, idempotencyKey: string) =>
  httpPost<string, undefined>({ url: getUrl('import/accounts', { idempotencyKey }), data: csv, parseJson: false })

export const findAllVouchers = () => httpGet<Voucher[]>({ url: getUrl('vouchers') })

export const createVoucher = (data: Voucher) => httpPost<Voucher, Voucher>({ url: getUrl('vouchers'), data })

export const createBatchVoucher = (data: BatchVoucherCreateDto) =>
  httpPost<BatchVoucherCreateDto, Voucher>({ url: getUrl(`items/${data.itemId}/voucher`), data })

export const updateVoucher = (voucherId: number, data: VoucherCountDto) =>
  httpPost<VoucherCountDto, Voucher>({ url: getUrl(`vouchers/${voucherId}/count`), data })

export const incrementVoucherCount = (voucherId: number, data: VoucherDeltaDto) =>
  httpPost<VoucherDeltaDto, Voucher>({ url: getUrl(`vouchers/${voucherId}/increment`), data })

export const deleteVoucher = (voucherId: number) => httpDelete<undefined>({ url: getUrl(`vouchers/${voucherId}`), parseJson: false })

export const exportVoucherTemplate = () => httpGet<string>({ url: getUrl('template/vouchers'), parseJson: false })

export const exportVouchers = () => httpGet<string>({ url: getUrl('export/vouchers'), parseJson: false })

export const importVouchers = (csv: string, idempotencyKey: string) =>
  httpPost<string, VoucherImportResult>({
    url: getUrl('import/vouchers', { idempotencyKey }),
    data: csv,
    parseJson: false,
    mapResponse: (data) => JSON.parse(data as string) as VoucherImportResult
  })

export const exportOrders = () => httpGet<string>({ url: getUrl('export/orders'), parseJson: false })

export const exportOrderLines = () => httpGet<string>({ url: getUrl('export/order_lines'), parseJson: false })

export const findAllOrdersWithOrderLines = (page?: number, size?: number) =>
  httpGet<OrderWithOrderLine[]>({ url: getUrl('orders-with-order-lines', { page, size }) })

export const exportOrdersWithOrderLines = () => httpGet<string>({ url: getUrl('export/orders-with-order-lines'), parseJson: false })

export const findAllEvents = (page?: number, size?: number) =>
  httpGet<Event[]>({
    url: getUrl('events', { page, size }),
    mapResponse: (data) => addColorToListResponse(data, (event: Event) => event.event)
  })

export const getConsumptionLeaderboard = (limit: number) =>
  httpGet<ConsumptionLeaderboardEntry[]>({
    url: getUrl('consumption-leaderboard', { limit }),
    mapResponse: (data) => addColorToListResponse(data, (entry: ConsumptionLeaderboardEntry) => entry.accountId?.toString() || '')
  })

export const getItemLeaderboard = (limit: number) =>
  httpGet<ItemLeaderboardEntry[]>({
    url: getUrl('item-leaderboard', { limit }),
    mapResponse: (data) => addColorToListResponse(data, (entry: ItemLeaderboardEntry) => entry.itemId?.toString() || '')
  })

export const exportEvents = () => httpGet<string>({ url: getUrl('export/events'), parseJson: false })

export const findAllTransactions = (page?: number, size?: number) => httpGet<Transaction[]>({ url: getUrl('transactions', { page, size }) })

export const exportTransactions = () => httpGet<string>({ url: getUrl('export/transactions'), parseJson: false })

export const findAllSessions = (page?: number, size?: number) => httpGet<SessionInfo[]>({ url: getUrl('sessions', { page, size }) })

export const deleteSession = (sessionId: string) => httpDelete<undefined>({ url: getUrl(`sessions/${sessionId}`), parseJson: false })
