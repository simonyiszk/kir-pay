import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery, UseQueryResult } from '@tanstack/react-query'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { OrderWithOrderLine, ValidatedApiCall } from '@/lib/api/model.ts'
import { Card } from '@/components/ui/card.tsx'
import { BackAndForwardPagination } from '@/components/ui/pagination.tsx'
import { useState } from 'react'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { Badge } from '@/components/ui/badge.tsx'
import { ItemView } from '@/page/admin/common/ItemView.tsx'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'
import { OrderExportDropdown } from '@/page/admin/orders/OrderExportDropdown.tsx'
import { findAllOrdersWithOrderLines } from '@/lib/api/admin.api.ts'
import { AccountNameView } from '@/page/admin/common/AccountView.tsx'
import { formatTimestamp } from '@/lib/utils.ts'

const OrderLineList = ({
  orderLines,
  page,
  setPage
}: {
  page: number
  setPage: (page: number) => void
  orderLines: UseQueryResult<ValidatedApiCall<OrderWithOrderLine[]>>
}) => {
  const { currencySymbol } = useAppContext().config
  if (!orderLines.data) return null
  if (orderLines.data.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (page === 0 && !orderLines.data.data.length)
    return <h1 className="font-bold text-lg pb-4 text-center">Még nincs egyetlen rendelés sem!</h1>

  return (
    <div className="flex flex-col gap-2">
      {<BackAndForwardPagination page={page} setPage={setPage} reachedEnd={orderLines.data.data.length <= 0} />}
      {orderLines.data.data.length <= 0 && <span className="font-bold text-lg pt-6 pb-4 text-center">Nincs több rendelés</span>}
      {orderLines.data.data.map((orderLine) => (
        <Card key={orderLine.orderId + '-' + orderLine.orderLineId} className="p-4 flex flex-row gap-4 items-center flex-wrap">
          <span className="text-xs font-mono">{formatTimestamp(orderLine.timestamp)}</span>
          <Badge variant="secondary">Rendelés #{orderLine.orderId}</Badge>
          <strong>
            <AccountNameView accountId={orderLine.accountId} />
          </strong>
          {!!orderLine.paidAmount && (
            <Badge>
              fizetve {orderLine.paidAmount} {currencySymbol}
            </Badge>
          )}
          {orderLine.usedVoucher && (
            <Badge variant="outline" className="border-blue-600">
              Utalvánnyal
            </Badge>
          )}
          <Badge variant="secondary" className="font-bold">
            <span>
              <ItemView
                ItemView={({ item }) => <span>{item.name}</span>}
                itemId={orderLine.itemId}
                loadingPlaceholder={<span>...</span>}
                errorPlaceholder={<span>{orderLine.message}</span>}
              />
              : {orderLine.itemCount}x
            </span>
          </Badge>
        </Card>
      ))}
      {orderLines.data.data.length > 0 && (
        <BackAndForwardPagination page={page} setPage={setPage} reachedEnd={orderLines.data.data.length <= 0} />
      )}
    </div>
  )
}

export const OrderPage = () => {
  const [page, setPage] = useState(0)
  const { token } = useAppContext()
  const orderLines = useQuery({
    queryKey: [AppQueryKeys.OrderWithOrderLines, token, page],
    queryFn: () => findAllOrdersWithOrderLines(token, page, 25),
    refetchInterval: DataRefetchInterval,
    staleTime: DataRefetchInterval
  })

  return (
    <div className="flex-1 h-full relative">
      <div className="flex items-baseline justify-center py-6 gap-4">
        <h1 className="font-bold text-2xl pb-4 text-center">Rendelések</h1>
        <OrderExportDropdown />
      </div>
      {orderLines.isLoading && (
        <div className="p-4">
          <LoadingIndicator />
        </div>
      )}
      <OrderLineList page={page} setPage={setPage} orderLines={orderLines} />
    </div>
  )
}
