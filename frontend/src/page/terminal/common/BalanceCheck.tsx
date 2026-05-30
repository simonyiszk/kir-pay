import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { CircleDollarSign, CircleX } from 'lucide-react'
import { useEffect, useState } from 'react'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { RotatedForCustomer } from '@/components/RotatedForCustomer.tsx'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { Account } from '@/lib/api/model.ts'
import { findAccountByCard } from '@/lib/api/terminal.api.ts'

export const BalanceCheck = ({
  card,
  loading,
  setLoading
}: {
  card?: string
  loading: boolean
  setLoading: (loading: boolean) => void
}) => {
  const { token } = useAppContext()
  const [balance, setBalance] = useState<Account | null>()
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

  return <BalanceReadResult card={card} balance={balance} error={error} />
}

const BalanceReadResult = ({ card, balance, error }: { card: string; balance?: Account | null; error?: string }) => {
  const { currencySymbol } = useAppContext().config

  const message = error || (!balance && `A ${card} azonosítójú kártya egyenlegét nem lehet leolvasni!`)
  if (message)
    return (
      <Alert className="w-[auto]">
        <CircleX className="px-1" />
        <AlertTitle>Hiba!</AlertTitle>
        <AlertDescription>{message}</AlertDescription>
      </Alert>
    )

  return (
    <RotatedForCustomer className="w-full">
      <Alert className="relative overflow-clip">
        {balance?.color && <ColorMarker color={balance.color} />}
        <CircleDollarSign className="px-1" />
        <AlertTitle>{balance?.name}</AlertTitle>
        <AlertDescription className="font-bold text-lg flex flex-col">
          <span className="font-normal text-sm pb-2">{balance?.email}</span>
          <span>Kártya: {card.substring(0, 10)}...</span>
          <span>
            Egyenleg:{' '}
            <span className={balance!.balance > 0 ? 'text-primary' : 'text-destructive'}>
              {balance!.balance} {currencySymbol}
            </span>
          </span>
        </AlertDescription>
      </Alert>
    </RotatedForCustomer>
  )
}
