import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { findAccountByCard } from '@/lib/api/terminal.api.ts'
import { ResultType } from '@/lib/api/model.ts'
import { useQuery } from '@tanstack/react-query'

export const CardCheckStep = ({ onReset, onProceed, card }: { onReset: () => void; onProceed: () => void; card: string }) => {
  const onProceedRef = useRef(onProceed)
  const [retries, setRetries] = useState(0)

  useEffect(() => {
    onProceedRef.current = onProceed
  }, [onProceed])

  const { data } = useQuery({
    queryKey: ['CardCheck', card, retries],
    queryFn: () => findAccountByCard(card)
  })

  const navigateCalled = useRef(false)
  useEffect(() => {
    if (data?.result === ResultType.NotFound && !navigateCalled.current) {
      navigateCalled.current = true
      onProceedRef.current()
    }
  }, [data])

  if (data?.result === 'Ok' && data.data.account.id !== undefined) {
    return (
      <>
        <h1 className="font-bold text-2xl pb-4 text-center">
          Ez a kártya már hozzá van rendelve valakihez, ha folytatod elveszed a jelenlegi tulajdonostól!
        </h1>
        <Button variant="secondary" className="w-full mb-2" onClick={onReset}>
          Vissza
        </Button>
        <Button className="w-full" onClick={onProceed}>
          Tovább
        </Button>
      </>
    )
  }

  if (data && data.result !== 'Ok' && data.result !== ResultType.NotFound) {
    const error = (data as { error?: string }).error ?? 'Hiba történt!'
    return (
      <>
        <h1 className="font-bold text-2xl pb-4 text-center text-destructive">{error}</h1>
        <Button variant="secondary" className="w-full mb-2" onClick={onReset}>
          Vissza
        </Button>
        <Button
          className="w-full"
          onClick={() => {
            navigateCalled.current = false
            setRetries(retries + 1)
          }}
        >
          Újra
        </Button>
      </>
    )
  }

  if (!data || data.result === ResultType.NotFound || (data.result === 'Ok' && data.data.account.id === undefined)) {
    return (
      <>
        <h1 className="font-bold text-2xl pb-2 text-center">Kártya és felhasználó összekapcsolása...</h1>
        <div className="mt-4">
          <LoadingIndicator />
        </div>
      </>
    )
  }

  return null
}
