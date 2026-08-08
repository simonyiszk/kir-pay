import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { CircleDollarSign, CircleX } from 'lucide-react'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { RotatedForCustomer } from '@/components/RotatedForCustomer.tsx'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { AccountWithVouchers } from '@/lib/api/model.ts'
import { findAccountByCard } from '@/lib/api/terminal.api.ts'
import { useQuery } from '@tanstack/react-query'

export const BalanceCheck = ({ showVouchers, card }: { showVouchers: boolean; card?: string }) => {
  const { data, isLoading } = useQuery({
    queryKey: ['AccountByCard', card],
    queryFn: () => findAccountByCard(card!),
    enabled: !!card
  })

  if (!card) return null

  if (isLoading)
    return (
      <div className="mt-4">
        <LoadingIndicator />
      </div>
    )

  const balanceError = data && data.result !== 'Ok' ? data.error || `Sikertelen leolvasás (${data.result})` : undefined
  return (
    <BalanceReadResult
      showVouchers={showVouchers}
      card={card}
      balance={data?.result === 'Ok' ? data.data : undefined}
      error={balanceError}
    />
  )
}

const BalanceReadResult = ({
  showVouchers,
  card,
  balance,
  error
}: {
  showVouchers: boolean
  card: string
  balance?: AccountWithVouchers
  error?: string
}) => {
  const { currencySymbol } = useAppContext().config

  const message = error || (!balance && `A ${card} azonosítójú kártya egyenlegét nem lehet leolvasni!`)
  if (message)
    return (
      <Alert className="w-auto">
        <CircleX className="px-1" />
        <AlertTitle>Hiba!</AlertTitle>
        <AlertDescription>{message}</AlertDescription>
      </Alert>
    )

  const account = balance?.account
  const vouchers = balance?.vouchers?.filter((voucher) => voucher.count > 0) || []
  const voucherCount = vouchers.length
  return (
    <RotatedForCustomer className="w-full">
      <Alert className="relative overflow-clip">
        {account?.color && <ColorMarker color={account?.color} />}
        <CircleDollarSign className="px-1" />
        <AlertTitle>{account?.name}</AlertTitle>
        <AlertDescription className="font-bold text-lg flex flex-col">
          <span className="font-normal text-sm pb-2">{account?.email}</span>
          <span>Kártya: {card.substring(0, 6)}...</span>
          <span>
            Egyenleg:{' '}
            <span className={account!.balance > 0 ? 'text-primary' : 'text-destructive'}>
              {account!.balance} {currencySymbol}
            </span>
          </span>
          {showVouchers && !!voucherCount && (
            <span className="flex flex-wrap gap-x-2">
              Tokenek:
              {vouchers.map((voucher, i) => (
                <span key={voucher.itemId}>
                  {voucher.count}× {voucher.itemName}
                  {i < voucherCount - 1 ? ', ' : null}
                </span>
              ))}
            </span>
          )}
          {showVouchers && !voucherCount && 'Nincsenek tokenek.'}
        </AlertDescription>
      </Alert>
    </RotatedForCustomer>
  )
}
