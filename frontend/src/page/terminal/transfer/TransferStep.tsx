import { useEffect, useRef, useState } from 'react'
import { cn } from '@/lib/utils.ts'
import { Button } from '@/components/ui/button.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'
import CheckAnimation from '@/components/CheckAnimation.tsx'
import { ResultType } from '@/lib/api/model.ts'
import { transferFunds } from '@/lib/api/terminal.api.ts'
import { useMutation } from '@tanstack/react-query'

export const TransferStep = ({
  amount,
  sender,
  recipient,
  onReset
}: {
  amount: number
  sender: string
  recipient: string
  onReset: () => void
}) => {
  const [retries, setRetries] = useState(0)
  const [status, setStatus] = useState<ResultType>()
  const [error, setError] = useState<string>()
  const [message, setMessage] = useState<string>()
  const idempotencyKeyRef = useRef<string>(crypto.randomUUID())
  const submittedRef = useRef(false)

  const { mutate } = useMutation({
    mutationFn: () => transferFunds(sender, { recipientCard: recipient, amount, idempotencyKey: idempotencyKeyRef.current }),
    onSuccess: (data) => {
      setStatus(data.result)
      if (data.result !== 'Ok') {
        setMessage(data.error ?? 'Hiba történt!')
      }
    },
    onError: () => setError('Az átruházás sikertelen!')
  })

  useEffect(() => {
    if (submittedRef.current) return
    submittedRef.current = true
    mutate()
  }, [sender, recipient, amount, retries, mutate])

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
        <h1 className="font-bold text-2xl pb-2 text-center">Átruházás folyamatban...</h1>
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
      <BalanceCheck showVouchers={false} card={sender} />
      <BalanceCheck showVouchers={false} card={recipient} />
      <Button variant="secondary" className="w-full" onClick={onReset}>
        Új tranzakció
      </Button>
    </>
  )
  if (status == 'Ok') {
    return <CheckAnimation>{data}</CheckAnimation>
  }
  return data
}
