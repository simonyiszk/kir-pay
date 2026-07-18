export type Account = {
  id?: number
  name: string
  email?: string
  phone?: string
  card?: string
  balance: number
  active: boolean
  color?: string
}

export type AppConfig = {
  currencySymbol: string
  showUploadTab: boolean
  showPayTab: boolean
  showBalanceTab: boolean
  showSetCardTab: boolean
  showCartTab: boolean
  showTokenTab: boolean
  showTransferTab: boolean
}

export type AppResponse = {
  config: AppConfig
  principal: Principal
}

export type BalanceAmountDto = {
  amount: number
}

export type BalanceTransferDto = {
  recipientCard: string
  amount: number
}

export type CardAssignDto = {
  card: string
}

export type CheckoutDto = {
  orderLines: Array<OrderLineDto>
}

export type Event = {
  id?: number
  event: string
  timestamp: number
  message: string
  performedBy: string
  color?: string
}

export type AnalyticsDto = {
  accountCount: number
  transactionCount: number
  allActiveBalance: number
  income: number
  allUploads: number
  transactionVolume: number
}

export type Item = {
  id?: number
  name: string
  alias?: string
  cost: number
  stock: number
  enabled: boolean
  showOnLeaderboard: boolean
  color?: string
}

export type Order = {
  id?: number
  accountId: number
  timestamp: number
}

export type OrderLine = {
  id?: number
  orderId?: number
  itemId?: number
  itemCount: number
  message?: string
  usedVoucher: boolean
  paidAmount: number
}

export type OrderLineDto = {
  itemId?: number
  itemCount: number
  usedVoucher: boolean
  message?: string
  paidAmount?: number
}

export type OrderWithOrderLine = {
  orderId: number
  accountId: number
  timestamp: number
  orderLineId: number
  itemId?: number
  itemCount: number
  message?: string
  usedVoucher: boolean
  paidAmount: number
}

export type Principal = {
  id?: number
  name: string
  secret: string
  role: PrincipalRole
  active: boolean
  canUpload: boolean
  canTransfer: boolean
  canSellItems: boolean
  canRedeemVouchers: boolean
  canAssignCards: boolean
  createdAt: number
  lastUsed: number
}

export const PrincipalRole = {
  Admin: 'ADMIN',
  Terminal: 'TERMINAL'
} as const
export type PrincipalRole = (typeof PrincipalRole)[keyof typeof PrincipalRole]

export type PrincipalDto = {
  name: string
  password: string
  role: PrincipalRole
  canUpload: boolean
  canTransfer: boolean
  canSellItems: boolean
  canRedeemVouchers: boolean
  canAssignCards: boolean
  active: boolean
}

export type Transaction = {
  id?: number
  type: TransactionType
  senderId?: number
  recipientId?: number
  amount: number
  message?: string
  timestamp: number
}

export const TransactionType = {
  TopUp: 'TOP_UP',
  Transfer: 'TRANSFER',
  Charge: 'CHARGE'
} as const
export type TransactionType = (typeof TransactionType)[keyof typeof TransactionType]

export type Voucher = {
  id?: number
  accountId?: number
  itemId: number
  count: number
}

export type VoucherWithItemName = {
  voucherId: number
  accountId?: number
  itemId: number
  itemName: string
  count: number
}

export type AccountWithVouchers = {
  account: Account
  vouchers: VoucherWithItemName[]
}

export type BatchVoucherDto = {
  accounts: number[]
  itemId: number
  count: number
}

export type VoucherCountDto = {
  count: number
}

export type ValidatedApiCall<T> = { result: OkResultType; data: T } | { result: ErrorResultType; error?: string }

export const ResultType = {
  Ok: 'Ok',
  BadRequest: 'BadRequest',
  NotFound: 'NotFound',
  Unauthorized: 'Unauthorized',
  Forbidden: 'Forbidden',
  OtherError: 'OtherError'
} as const
export type ResultType = (typeof ResultType)[keyof typeof ResultType]
export type ErrorResultType = Exclude<(typeof ResultType)[keyof typeof ResultType], typeof ResultType.Ok>
export type OkResultType = typeof ResultType.Ok
