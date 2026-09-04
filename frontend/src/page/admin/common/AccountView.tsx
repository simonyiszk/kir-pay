import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ReactNode } from 'react'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { findAccountById } from '@/lib/api/admin.api.ts'
import { Account } from '@/lib/api/model.ts'

const AccountView = ({
  accountId,
  AccountView,
  loadingPlaceholder,
  errorPlaceholder
}: {
  accountId?: number
  AccountView: ({ account }: { account: Account }) => ReactNode
  loadingPlaceholder?: ReactNode
  errorPlaceholder?: ReactNode
}) => {
  const canExecuteQuery = !(accountId === undefined || accountId === null)
  const accountQuery = useQuery({
    enabled: canExecuteQuery,
    queryFn: async () => {
      const account = await findAccountById(accountId!)
      if (account.result !== 'Ok') throw Error(account.error)
      return account.data
    },
    queryKey: [AppQueryKeys.AdminAccounts, accountId],
    placeholderData: keepPreviousData,
    staleTime: 30000
  })

  if (!canExecuteQuery) return errorPlaceholder || null
  if (accountQuery.isLoading) return loadingPlaceholder || null
  if (accountQuery.error || !accountQuery.data) return errorPlaceholder || null

  return <AccountView account={accountQuery.data} />
}

export const AccountNameView = ({ accountId }: { accountId?: number }) => (
  <AccountView
    accountId={accountId}
    loadingPlaceholder={<span>...</span>}
    errorPlaceholder={<span>???</span>}
    AccountView={({ account }) => <span>{account.name}</span>}
  />
)
