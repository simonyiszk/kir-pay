import { useAppContext } from '@/hooks/useAppContext.ts'
import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { findAccountByCard } from '@/lib/api/terminal.api.ts'

export const CardCheckStep = ({ onReset, onProceed, card }: { onReset: () => void; onProceed: () => void; card: string }) => {
  const { token } = useAppContext()
  const [showPrompt, setShowPrompt] = useState(false)

  useEffect(() => {
    findAccountByCard(token, card).then((response) => {
      if (response.result === 'Ok' && response.data.account.id !== undefined) {
        setShowPrompt(true)
      } else {
        onProceed()
      }
    })
  }, [card, token, onProceed])

  if (!showPrompt)
    return (
      <>
        <h1 className="font-bold text-2xl pb-2 text-center">Kártya és felhasználó összekapcsolása...</h1>
        <div className="mt-4">
          <LoadingIndicator />
        </div>
      </>
    )

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
