import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { CircleDollarSign, CircleX } from 'lucide-react'
import { useEffect, useState } from 'react'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { RotatedForCustomer } from '@/components/RotatedForCustomer.tsx'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { AccountWithVouchers } from '@/lib/api/model.ts'
import { findAccountByCard } from '@/lib/api/terminal.api.ts'

export const BalanceCheck = ({
  showVouchers,
  card,
  loading,
  setLoading
}: {
  showVouchers: boolean
  card?: string
  loading: boolean
  setLoading: (loading: boolean) => void
}) => {
  const { token } = useAppContext()
  const [balance, setBalance] = useState<AccountWithVouchers | null>()
  const [error, setError] = useState<string>()

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setError(undefined)
    if (!card) return

    setLoading(true)
    findAccountByCard(token, card).then((balance) => {
      if (balance.result !== 'Ok') {
        setError(balance.error || 'Sikertelen leolvasás')
      } else {
        setBalance(balance.data)
      }
      setLoading(false)
    })
  }, [card, token, setLoading])

  if (!card) return null

  if (loading)
    return (
      <div className="mt-4">
        <LoadingIndicator />
      </div>
    )

  return <BalanceReadResult showVouchers={showVouchers} card={card} balance={balance} error={error} />
}

const BalanceReadResult = ({
  showVouchers,
  card,
  balance,
  error
}: {
  showVouchers: boolean
  card: string
  balance?: AccountWithVouchers | null
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
