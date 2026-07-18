import { addColorToListResponse, addColorToResponse, getApiRoot, httpDelete, httpGet, httpPost } from '@/lib/api/common.api.ts'
import {
  Account,
  AnalyticsDto,
  BatchVoucherDto,
  ConsumptionLeaderboardEntry,
  Event,
  Item,
  ItemLeaderboardEntry,
  Order,
  OrderLine,
  OrderWithOrderLine,
  Principal,
  PrincipalDto,
  Transaction,
  Voucher,
  VoucherCountDto
} from '@/lib/api/model.ts'

const getUrl = (endpoint: string, params?: object) => {
  const url = new URL(`${getApiRoot()}/admin/${endpoint}`)
  Object.entries(params || {})
    .filter((param) => param[1] !== undefined)
    .forEach((param) => url.searchParams.append(param[0], param[1]))
  return url
}

export const getAnalytics = (token: string) => httpGet<AnalyticsDto>({ url: getUrl('analytics'), token })

export const findAllPrincipals = (token: string) => httpGet<Principal[]>({ url: getUrl('principals'), token })

export const createPrincipal = (token: string, data: PrincipalDto) =>
  httpPost<PrincipalDto, Principal>({ url: getUrl('principals'), data, token })

export const updatePrincipal = (token: string, principalId: number, data: PrincipalDto) =>
  httpPost<PrincipalDto, Principal>({ url: getUrl(`principals/${principalId}`), data, token })

export const deletePrincipal = (token: string, principalId: number) =>
  httpDelete<undefined>({ url: getUrl(`principals/${principalId}`), token, parseJson: false })

export const enablePrincipal = (token: string, principalId: number) =>
  httpPost<undefined, Principal>({ url: getUrl(`principals/${principalId}/enable`), data: undefined, token })

export const disablePrincipal = (token: string, principalId: number) =>
  httpPost<undefined, Principal>({ url: getUrl(`principals/${principalId}/disable`), data: undefined, token })

export const exportPrincipalTemplate = (token: string) => httpGet<string>({ url: getUrl('template/principals'), token, parseJson: false })

export const exportPrincipals = (token: string) => httpGet<string>({ url: getUrl('export/principals'), token, parseJson: false })

export const importPrincipals = (token: string, csv: string) =>
  httpPost<string, undefined>({ url: getUrl('import/principals'), token, data: csv, parseJson: false })

export const findAllItems = (token: string) =>
  httpGet<Item[]>({
    url: getUrl('items'),
    token,
    mapResponse: addColorToListResponse
  })

export const findItemById = (token: string, itemId: number) =>
  httpGet<Item>({
    url: getUrl(`items/${itemId}`),
    token,
    mapResponse: addColorToResponse
  })

export const createItem = (token: string, data: Item) =>
  httpPost<Item, Item>({ url: getUrl('items'), data, token, mapResponse: addColorToResponse })

export const updateItem = (token: string, itemId: number, data: Item) =>
  httpPost<Item, Item>({ url: getUrl(`items/${itemId}`), data, token, mapResponse: addColorToResponse })

export const deleteItem = (token: string, itemId: number) =>
  httpDelete<undefined>({ url: getUrl(`items/${itemId}`), token, parseJson: false })

export const enableItem = (token: string, itemId: number) =>
  httpPost<undefined, Item>({
    url: getUrl(`items/${itemId}/enable`),
    data: undefined,
    token,
    mapResponse: addColorToResponse
  })

export const disableItem = (token: string, itemId: number) =>
  httpPost<undefined, Item>({
    url: getUrl(`items/${itemId}/disable`),
    data: undefined,
    token,
    mapResponse: addColorToResponse
  })

export const exportItemTemplate = (token: string) => httpGet<string>({ url: getUrl('template/items'), token, parseJson: false })

export const exportItems = (token: string) => httpGet<string>({ url: getUrl('export/items'), token, parseJson: false })

export const importItems = (token: string, csv: string) =>
  httpPost<string, undefined>({ url: getUrl('import/items'), token, data: csv, parseJson: false })

export const createAccount = (token: string, data: Account) =>
  httpPost<Account, Account>({ url: getUrl('accounts'), data, token, mapResponse: addColorToResponse })

export const updateAccount = (token: string, accountId: number, data: Account) =>
  httpPost<Account, Account>({
    url: getUrl(`accounts/${accountId}`),
    data,
    token,
    mapResponse: addColorToResponse
  })

export const deleteAccount = (token: string, accountId: number) =>
  httpDelete<undefined>({ url: getUrl(`accounts/${accountId}`), token, parseJson: false })

export const enableAccount = (token: string, accountId: number) =>
  httpPost<undefined, Account>({
    url: getUrl(`accounts/${accountId}/enable`),
    data: undefined,
    token,
    mapResponse: addColorToResponse
  })

export const disableAccount = (token: string, accountId: number) =>
  httpPost<undefined, Account>({
    url: getUrl(`accounts/${accountId}/disable`),
    data: undefined,
    token,
    mapResponse: addColorToResponse
  })

export const exportAccountTemplate = (token: string) => httpGet<string>({ url: getUrl('template/accounts'), token, parseJson: false })

export const exportAccounts = (token: string) => httpGet<string>({ url: getUrl('export/accounts'), token, parseJson: false })

export const importAccounts = (token: string, csv: string) =>
  httpPost<string, undefined>({ url: getUrl('import/accounts'), token, data: csv, parseJson: false })

export const findAllVouchers = (token: string) => httpGet<Voucher[]>({ url: getUrl('vouchers'), token })

export const createVoucher = (token: string, data: Voucher) => httpPost<Voucher, Voucher>({ url: getUrl('vouchers'), data, token })

export const createBatchVoucher = (token: string, data: BatchVoucherDto) =>
  httpPost<BatchVoucherDto, Voucher>({ url: getUrl(`items/${data.itemId}/voucher`), data, token })

export const updateVoucher = (token: string, voucherId: number, data: VoucherCountDto) =>
  httpPost<VoucherCountDto, Voucher>({ url: getUrl(`vouchers/${voucherId}/count`), data, token })

export const deleteVoucher = (token: string, voucherId: number) =>
  httpDelete<undefined>({ url: getUrl(`vouchers/${voucherId}`), token, parseJson: false })

export const exportVoucherTemplate = (token: string) => httpGet<string>({ url: getUrl('template/vouchers'), token, parseJson: false })

export const exportVouchers = (token: string) => httpGet<string>({ url: getUrl('export/vouchers'), token, parseJson: false })

export const importVouchers = (token: string, csv: string) =>
  httpPost<string, undefined>({ url: getUrl('import/vouchers'), token, data: csv, parseJson: false })

export const findAllOrders = (token: string, page?: number, size?: number) =>
  httpGet<Order[]>({ url: getUrl('orders', { page, size }), token })

export const exportOrders = (token: string) => httpGet<string>({ url: getUrl('export/orders'), token, parseJson: false })

export const findAllOrderLines = (token: string, page?: number, size?: number) =>
  httpGet<OrderLine[]>({ url: getUrl('order_lines', { page, size }), token })

export const exportOrderLines = (token: string) => httpGet<string>({ url: getUrl('export/order_lines'), token, parseJson: false })

export const findAllOrdersWithOrderLines = (token: string, page?: number, size?: number) =>
  httpGet<OrderWithOrderLine[]>({ url: getUrl('orders-with-order-lines', { page, size }), token })

export const exportOrdersWithOrderLines = (token: string) =>
  httpGet<string>({ url: getUrl('export/orders-with-order-lines'), token, parseJson: false })

export const findAllEvents = (token: string, page?: number, size?: number) =>
  httpGet<Event[]>({
    url: getUrl('events', { page, size }),
    token,
    mapResponse: (res) => addColorToListResponse(res, (event) => event.event)
  })

export const getConsumptionLeaderboard = (token: string, limit: number) =>
  httpGet<ConsumptionLeaderboardEntry[]>({
    url: getUrl('consumption-leaderboard', { limit }),
    token,
    mapResponse: (res) => addColorToListResponse(res, (entry) => entry.accountId?.toString() || '')
  })

export const getItemLeaderboard = (token: string, limit: number) =>
  httpGet<ItemLeaderboardEntry[]>({
    url: getUrl('item-leaderboard', { limit }),
    token,
    mapResponse: (res) => addColorToListResponse(res, (entry) => entry.itemId?.toString() || '')
  })

export const exportEvents = (token: string) => httpGet<string>({ url: getUrl('export/events'), token, parseJson: false })

export const findAllTransactions = (token: string, page?: number, size?: number) =>
  httpGet<Transaction[]>({ url: getUrl('transactions', { page, size }), token })

export const exportTransactions = (token: string) => httpGet<string>({ url: getUrl('export/transactions'), token, parseJson: false })
