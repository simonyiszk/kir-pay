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

export const getAppData = (token: string) => httpGet<AppResponse>({ url: new URL(`${getApiRoot()}/app`), token })

export const findAllAccounts = (token: string, page?: number, size?: number) =>
  httpGet<Account[]>({
    url: getUrl('accounts', { page, size }),
    token,
    mapResponse: addColorToListResponse
  })

export const findAccountById = (token: string, accountId: number) =>
  httpGet<Account>({
    url: getUrl(`accounts/${accountId}`),
    token,
    mapResponse: addColorToResponse
  })

const accountResponseMapper = (res: Response) =>
  addColorToResponse<AccountWithVouchers>(res, (data) => ({
    vouchers: data.vouchers,
    account: { ...data.account, color: getHashedColor(data.account.id?.toString() ?? '') }
  }))

export const findAccountByCard = (token: string, card: string) =>
  httpGet<AccountWithVouchers>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}`),
    token,
    mapResponse: accountResponseMapper
  })

export const uploadBalance = (token: string, card: string, data: BalanceAmountDto) =>
  httpPost<BalanceAmountDto, Account>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}/upload`),
    data,
    token,
    mapResponse: addColorToResponse
  })

export const transferFunds = (token: string, sender: string, data: BalanceTransferDto) =>
  httpPost<BalanceTransferDto, Account>({
    url: getUrl(`account-by-card/${encodeURIComponent(sender)}/transfer`),
    data,
    token,
    mapResponse: addColorToResponse
  })

export const pay = (token: string, card: string, data: BalanceAmountDto) =>
  httpPost<BalanceAmountDto, Account>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}/pay`),
    data,
    token,
    mapResponse: addColorToResponse
  })

export const assignCard = (token: string, accountId: number, data: CardAssignDto) =>
  httpPost<CardAssignDto, Account>({
    url: getUrl(`accounts/${accountId}/card`),
    data,
    token,
    mapResponse: addColorToResponse
  })

export const findAllItems = (token: string) =>
  httpGet<Item[]>({
    url: getUrl('items'),
    token,
    mapResponse: addColorToListResponse
  })

export const checkout = (token: string, card: string, data: CheckoutDto) =>
  httpPost<CheckoutDto, undefined>({
    url: getUrl(`account-by-card/${encodeURIComponent(card)}/checkout`),
    data,
    token
  })
