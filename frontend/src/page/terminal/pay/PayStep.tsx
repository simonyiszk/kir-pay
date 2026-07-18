import { useAppContext } from '@/hooks/useAppContext.ts'
import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'
import { cn } from '@/lib/utils.ts'
import CheckAnimation from '@/components/CheckAnimation.tsx'
import { pay } from '@/lib/api/terminal.api.ts'
import { ResultType } from '@/lib/api/model.ts'

export const PayStep = ({ onReset, card, amount }: { onReset: () => void; card: string; amount: number }) => {
  const { token } = useAppContext()
  const [retries, setRetries] = useState(0)
  const [status, setStatus] = useState<ResultType>()
  const [error, setError] = useState<string>()
  const [message, setMessage] = useState<string>()
  const [balanceCheckLoading, setBalanceCheckLoading] = useState(false)

  useEffect(() => {
    pay(token, card, { amount })
      .then((data) => {
        setStatus(data.result)
        if (data.result !== 'Ok') {
          setMessage(data.error)
        }
      })
      .catch(() => setError('A fizetés sikertelen!'))
  }, [card, amount, retries, token])

  if (error)
    return (
      <>
        <h1 className="font-bold text-2xl pb-2 text-center text-destructive">{error}</h1>
        <Button variant="secondary" className="w-full mb-2" onClick={onReset}>
          Új tranzakció
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
        <h1 className="font-bold text-2xl pb-2 text-center">Tranzakció folyamatban...</h1>
        <div className="mt-4">
          <LoadingIndicator />
        </div>
      </>
    )
  const data = (
    <>
      <h1 className={cn('font-bold text-2xl pb-2 text-center', status !== 'Ok' && 'text-destructive')}>
        {status !== 'Ok' ? message : 'Sikeres tranzakció!'}
      </h1>
      <BalanceCheck showVouchers={true} card={card} loading={balanceCheckLoading} setLoading={setBalanceCheckLoading} />
      <Button variant="secondary" className="w-full" onClick={onReset}>
        Új tranzakció
      </Button>
    </>
  )
  if (status === 'Ok') {
    return <CheckAnimation>{data}</CheckAnimation>
  }
  return data
}
