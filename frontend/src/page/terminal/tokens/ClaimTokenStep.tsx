import { useEffect, useRef, useState } from 'react'
import { cn } from '@/lib/utils.ts'
import { Button } from '@/components/ui/button.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import CheckAnimation from '@/components/CheckAnimation.tsx'
import { Item, ResultType } from '@/lib/api/model.ts'
import { checkout } from '@/lib/api/terminal.api.ts'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'
import { useMutation } from '@tanstack/react-query'

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
  const [retries, setRetries] = useState(0)
  const [status, setStatus] = useState<ResultType>()
  const [message, setMessage] = useState<string>()
  const [error, setError] = useState<string>()
  const idempotencyKeyRef = useRef<string>(crypto.randomUUID())
  const submittedRef = useRef(false)

  const { mutate } = useMutation({
    mutationFn: () =>
      checkout(card, {
        idempotencyKey: idempotencyKeyRef.current,
        orderLines: [{ itemCount: 1, usedVoucher: true, itemId: item.id }]
      }),
    onSuccess: (data) => {
      setStatus(data.result)
      if (data.result !== 'Ok') {
        setMessage(data.error ?? 'Hiba történt!')
      }
    },
    onError: () => setError('A fizetés sikertelen!')
  })

  useEffect(() => {
    if (submittedRef.current) return
    submittedRef.current = true
    mutate()
  }, [card, item, retries, mutate])

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
            submittedRef.current = false
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
      <BalanceCheck showVouchers={true} card={card} />
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
