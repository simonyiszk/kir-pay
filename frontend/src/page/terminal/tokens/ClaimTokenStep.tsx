import { useAppContext } from '@/hooks/useAppContext.ts'
import { useEffect, useState } from 'react'
import { cn } from '@/lib/utils.ts'
import { Button } from '@/components/ui/button.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import CheckAnimation from '@/components/CheckAnimation.tsx'
import { Item, ResultType } from '@/lib/api/model.ts'
import { checkout } from '@/lib/api/terminal.api.ts'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'

export const ClaimTokenStep = ({
  item,
  card,
  onReset,
  onBackToScan
}: {
  item: Item
  card: string
  onReset: () => void
  onBackToScan: () => void
}) => {
  const { token } = useAppContext()
  const [retries, setRetries] = useState(0)
  const [status, setStatus] = useState<ResultType>()
  const [message, setMessage] = useState<string>()
  const [error, setError] = useState<string>()
  const [balanceCheckLoading, setBalanceCheckLoading] = useState(false)

  useEffect(() => {
    checkout(token, card, { orderLines: [{ itemCount: 1, usedVoucher: true, itemId: item.id }] })
      .then((data) => {
        setStatus(data.result)
        if (data.result !== 'Ok') {
          setMessage(data.error)
        }
      })
      .catch(() => setError('A fizetés sikertelen!'))
  }, [card, item, token, retries])

  if (error)
    return (
      <>
        <h1 className="font-bold text-2xl pb-4 text-center text-destructive">{error}</h1>
        <Button variant="secondary" className="w-full mb-2" onClick={onReset}>
          Új beolvasás
        </Button>
        <Button
          className="w-full"
          onClick={() => {
            setError(undefined)
            setStatus(undefined)
            setRetries(retries + 1)
          }}
        >
          Újra
        </Button>
      </>
    )

  if (!status)
    return (
      <>
        <h1 className="font-bold text-2xl pb-2 text-center">Beolvasás folyamatban...</h1>
        <div className="mt-4">
          <LoadingIndicator />
        </div>
      </>
    )

  const confirmation = (
    <>
      <h1 className={cn('font-bold text-2xl pb-2 text-center', status !== 'Ok' && 'text-destructive')}>
        {status !== 'Ok' ? message : 'Sikeres beolvasás!'}
      </h1>
      <BalanceCheck showVouchers={true} card={card} loading={balanceCheckLoading} setLoading={setBalanceCheckLoading} />
      <Button className="w-full mt-2" onClick={onBackToScan}>
        Még egy ilyet
      </Button>
      <Button variant="secondary" className="w-full mt-2" onClick={onReset}>
        Új beolvasás
      </Button>
    </>
  )
  if (status === 'Ok') {
    return <CheckAnimation>{confirmation}</CheckAnimation>
  }

  return confirmation
}
