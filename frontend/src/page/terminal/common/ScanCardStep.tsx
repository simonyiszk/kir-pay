import { Button } from '@/components/ui/button.tsx'
import { useNFCScanner } from '@/hooks/useNFCScanner.ts'
import { RotatedForCustomer } from '@/components/RotatedForCustomer.tsx'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { CircleX } from 'lucide-react'

export const ScanCardStep = ({
  setCard,
  amount,
  message,
  onAbort
}: {
  setCard: (card: string) => void
  amount?: number
  message?: string
  onAbort?: () => void
}) => {
  const { currencySymbol } = useAppContext().config
  const { error } = useNFCScanner((event) => {
    setCard(event.serialNumber)
  }, [])

  return (
    <>
      <div className="flex flex-col gap-4 relative flex-1">
        <RotatedForCustomer>
          {message && <h1 className="font-bold text-2xl text-center">{message}</h1>}
          {amount && (
            <h1 className="font-bold text-2xl text-center">
              {amount} {currencySymbol}
            </h1>
          )}
        </RotatedForCustomer>
        {error && (
          <Alert className="w-auto">
            <CircleX className="px-1" />
            <AlertTitle>NFC hiba</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <h1 className="font-bold text-2xl pb-8 text-center">Érints kártyát az eszközhöz...</h1>
        {onAbort && (
          <Button variant="secondary" onClick={onAbort}>
            Vissza
          </Button>
        )}
      </div>
    </>
  )
}
