import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'
import { cn } from '@/lib/utils.ts'
import CheckAnimation from '@/components/CheckAnimation.tsx'
import { Cart } from '@/page/terminal/items/cart.ts'
import { checkout } from '@/lib/api/terminal.api.ts'
import { ResultType } from '@/lib/api/model.ts'
import { useMutation } from '@tanstack/react-query'

export const CartPayStep = ({
  onReset,
  onBackToCart,
  card,
  cart
}: {
  onReset: () => void
  onBackToCart: () => void
  card: string
  cart: Cart
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
        orderLines: [
          ...cart.items.map((item) => ({
            usedVoucher: false,
            itemId: item.item.id,
            itemCount: item.quantity
          })),
          ...cart.customEntries.map((item) => ({
            usedVoucher: false,
            itemCount: item.quantity,
            message: item.name,
            paidAmount: item.price
          }))
        ]
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
  }, [card, cart, retries, mutate])

  if (error)
    return (
      <>
        <h1 className="font-bold text-2xl pb-4 text-center text-destructive">{error}</h1>
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
        <h1 className="font-bold text-2xl pb-2 text-center">Tranzakció folyamatban...</h1>
        <div className="mt-4">
          <LoadingIndicator />
        </div>
      </>
    )

  const confirmation = (
    <>
      <h1 className={cn('font-bold text-2xl pb-2 text-center', status !== 'Ok' && 'text-destructive')}>
        {status !== 'Ok' ? message : 'Sikeres tranzakció!'}
      </h1>
      <BalanceCheck showVouchers={true} card={card} />
      <Button className="w-full mt-2" onClick={onBackToCart}>
        Még egy ilyet
      </Button>
      <Button variant="secondary" className="w-full mt-2" onClick={onReset}>
        Új tranzakció
      </Button>
    </>
  )
  if (status === 'Ok') {
    return <CheckAnimation>{confirmation}</CheckAnimation>
  }

  return confirmation
}
