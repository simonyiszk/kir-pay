import {
  Account,
  AccountWithVouchers,
  AppResponse,
  BalanceAmountDto,
  BalanceTransferDto,
  CardAssignDto,
  CheckoutDto,
  Item
} from '@/lib/api/model.ts'
import { addColorToListResponse, addColorToResponse, getApiRoot, httpGet, httpPost } from '@/lib/api/common.api.ts'
import { getHashedColor } from '@/lib/utils.ts'

const getUrl = (endpoint: string, params?: object) => {
  const url = new URL(`${getApiRoot()}/terminal/${endpoint}`)
  Object.entries(params || {})
    .filter((param) => param[1] !== undefined)
    .forEach((param) => url.searchParams.append(param[0], param[1]))
  return url
}

export const getAppData = () => httpGet<AppResponse>({ url: new URL(`${getApiRoot()}/app`) })

export const findAllAccounts = (page?: number, size?: number) =>
  httpGet<Account[]>({
    url: getUrl('accounts', { page, size }),
    mapResponse: addColorToListResponse
  })

export const findAccountById = (accountId: number) =>
  httpGet<Account>({
    url: getUrl(`accounts/${accountId}`),
    mapResponse: addColorToResponse
  })

const accountResponseMapper = (data: unknown) =>
  addColorToResponse<AccountWithVouchers>(data, (d) => ({
    vouchers: d.vouchers,
    account: { ...d.account, color: getHashedColor(d.account.id?.toString() ?? '') }
  }))

export const findAccountByCard = (card: string) =>
  httpGet<AccountWithVouchers>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}`),
    mapResponse: accountResponseMapper
  })

export const uploadBalance = (card: string, data: BalanceAmountDto) =>
  httpPost<BalanceAmountDto, Account>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}/upload`),
    data,
    mapResponse: addColorToResponse
  })

export const transferFunds = (sender: string, data: BalanceTransferDto) =>
  httpPost<BalanceTransferDto, Account>({
    url: getUrl(`account-by-card/${encodeURIComponent(sender)}/transfer`),
    data,
    mapResponse: addColorToResponse
  })

export const pay = (card: string, data: BalanceAmountDto) =>
  httpPost<BalanceAmountDto, Account>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}/pay`),
    data,
    mapResponse: addColorToResponse
  })

export const assignCard = (accountId: number, data: CardAssignDto) =>
  httpPost<CardAssignDto, Account>({
    url: getUrl(`accounts/${accountId}/card`),
    data,
    mapResponse: addColorToResponse
  })

export const findAllItems = () =>
  httpGet<Item[]>({
    url: getUrl('items'),
    mapResponse: addColorToListResponse
  })

export const checkout = (card: string, data: CheckoutDto) =>
  httpPost<CheckoutDto, undefined>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}/checkout`),
    data
  })
