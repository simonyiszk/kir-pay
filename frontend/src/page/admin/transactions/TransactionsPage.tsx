import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery, UseQueryResult } from '@tanstack/react-query'
import { exportTransactions, findAllTransactions } from '@/lib/api/admin.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Transaction, ValidatedApiCall } from '@/lib/api/model.ts'
import { exportToCsv, formatTimestamp } from '@/lib/utils.ts'
import { Card } from '@/components/ui/card.tsx'
import { BackAndForwardPagination } from '@/components/ui/pagination.tsx'
import { useState } from 'react'
import { Button } from '@/components/ui/button.tsx'
import { useToast } from '@/components/ui/use-toast.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { Badge } from '@/components/ui/badge.tsx'
import { AccountNameView } from '@/page/admin/common/AccountView.tsx'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'

const TransactionTileHeader = ({ transaction }: { transaction: Transaction }) => {
  const { currencySymbol } = useAppContext().config

  if (transaction.type === 'TOP_UP')
    return (
      <>
        <Badge variant="outline" className="border-green-600">
          Feltöltés
        </Badge>
        <Badge variant="secondary">
          {transaction.amount} {currencySymbol}
        </Badge>
        <span>
          <strong>
            <AccountNameView accountId={transaction.recipientId} />
          </strong>{' '}
          részére
        </span>
      </>
    )

  if (transaction.type === 'TRANSFER')
    return (
      <>
        <Badge variant="outline" className="border-blue-600">
          Átutalás
        </Badge>
        <Badge variant="secondary">
          {transaction.amount} {currencySymbol}
        </Badge>
        <span>
          Küldő:{' '}
          <strong>
            <AccountNameView accountId={transaction.senderId} />
          </strong>{' '}
          | Fogadó:{' '}
          <strong>
            <AccountNameView accountId={transaction.recipientId} />
          </strong>
        </span>
      </>
    )

  return (
    <>
      <Badge variant="outline" className="border-red-600">
        Fizetés
      </Badge>
      <Badge variant="secondary">
        {transaction.amount} {currencySymbol}
      </Badge>
      <span>
        <strong>
          <AccountNameView accountId={transaction.senderId} />
        </strong>{' '}
        számlája terhelve
      </span>
    </>
  )
}

const TransactionList = ({
  transactions,
  page,
  setPage
}: {
  page: number
  setPage: (page: number) => void
  transactions: UseQueryResult<ValidatedApiCall<Transaction[]>>
}) => {
  if (!transactions.data) return null
  if (transactions.data.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (page === 0 && !transactions.data.data.length)
    return <h1 className="font-bold text-lg pb-4 text-center">Még nincs egyetlen tranzakció sem!</h1>

  return (
    <div className="flex flex-col gap-2">
      {<BackAndForwardPagination page={page} setPage={setPage} reachedEnd={transactions.data.data.length <= 0} />}
      {transactions.data.data.length <= 0 && <span className="font-bold text-lg pt-6 pb-4 text-center">Nincs több tranzakció</span>}
      {transactions.data.data.map((transaction) => (
        <Card key={transaction.id} className="p-4 flex flex-row gap-4 items-center flex-wrap">
          <span className="text-xs font-mono">{formatTimestamp(transaction.timestamp)}</span>
          <TransactionTileHeader transaction={transaction} />
          {!!transaction.message && <span>Üzenet: {transaction.message}</span>}
        </Card>
      ))}
      {transactions.data.data.length > 0 && (
        <BackAndForwardPagination page={page} setPage={setPage} reachedEnd={transactions.data.data.length <= 0} />
      )}
    </div>
  )
}

export const TransactionsPage = () => {
  const [page, setPage] = useState(0)
  const { toast } = useToast()
  const { token } = useAppContext()
  const transactions = useQuery({
    queryKey: [AppQueryKeys.Transactions, token, page],
    queryFn: () => findAllTransactions(token, page, 25),
    refetchInterval: DataRefetchInterval,
    staleTime: DataRefetchInterval
  })

  return (
    <div className="flex-1 h-full relative">
      <div className="flex items-baseline justify-center py-6 gap-4">
        <h1 className="font-bold text-2xl pb-4 text-center">Tranzakciók</h1>
        <Button
          variant="secondary"
          onClick={() =>
            exportToCsv('transactions.csv', () =>
              exportTransactions(token).then((data) => {
                if (data.result === 'Ok') return data.data
                throw Error()
              })
            )
              .then(() => toast({ description: 'Tranzakciók exportálva' }))
              .catch(() => toast({ description: 'Hiba az tranzakciók exportálása közben' }))
          }
        >
          Exportálás
        </Button>
      </div>
      {transactions.isLoading && (
        <div className="p-4">
          <LoadingIndicator />
        </div>
      )}
      <TransactionList page={page} setPage={setPage} transactions={transactions} />
    </div>
  )
}
