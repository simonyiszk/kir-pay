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

export type IdempotentRequest = {
  idempotencyKey: string
}

export type AccountCreateDto = Account & IdempotentRequest

type AppConfig = {
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
  idempotencyKey: string
}

export type BalanceTransferDto = {
  recipientCard: string
  amount: number
  idempotencyKey: string
}

export type CardAssignDto = {
  card: string
}

export type CheckoutDto = {
  orderLines: Array<OrderLineDto>
  idempotencyKey: string
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

export type ItemCreateDto = Item & IdempotentRequest

type OrderLineDto = {
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

export type PrincipalCreateDto = PrincipalDto & IdempotentRequest

export type Transaction = {
  id?: number
  type: TransactionType
  senderId?: number
  recipientId?: number
  amount: number
  message?: string
  timestamp: number
}

const TransactionType = {
  TopUp: 'TOP_UP',
  Transfer: 'TRANSFER',
  Charge: 'CHARGE'
} as const
type TransactionType = (typeof TransactionType)[keyof typeof TransactionType]

export type Voucher = {
  id?: number
  accountId?: number
  itemId: number
  count: number
}

type VoucherWithItemName = {
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

export type BatchVoucherCreateDto = BatchVoucherDto & IdempotentRequest

export type VoucherCountDto = {
  count: number
}

export type VoucherDeltaDto = {
  delta: number
} & IdempotentRequest

export type VoucherImportResult = {
  imported: number
  total: number
  errors: string[]
}

export type ValidatedApiCall<T> = { result: OkResultType; data: T } | { result: ErrorResultType; error?: string }

export const ResultType = {
  Ok: 'Ok',
  BadRequest: 'BadRequest',
  NotFound: 'NotFound',
  Unauthorized: 'Unauthorized',
  Forbidden: 'Forbidden',
  UnprocessableEntity: 'UnprocessableEntity',
  OtherError: 'OtherError'
} as const
export type ResultType = (typeof ResultType)[keyof typeof ResultType]
export type ErrorResultType = Exclude<(typeof ResultType)[keyof typeof ResultType], typeof ResultType.Ok>
type OkResultType = typeof ResultType.Ok

export type SessionInfo = {
  sessionId: string
  principalName: string | null
  ipAddress: string | null
  userAgent: string | null
  creationTime: number
  lastAccessTime: number
  maxInactiveInterval: number
  expiryTime: number
}

export type ConsumptionLeaderboardEntry = {
  accountId: number
  name: string
  email?: string
  itemCount: number
  color?: string
}

export type ItemLeaderboardEntry = {
  itemId: number
  itemName: string
  itemCount: number
  color?: string
}
