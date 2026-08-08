import { useState } from 'react'
import { useNFCScanner } from '@/hooks/useNFCScanner.ts'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { CircleX, RefreshCw } from 'lucide-react'

const BalanceCheckPage = () => {
  const [card, setCard] = useState<string>()
  const [scanRetry, setScanRetry] = useState(0)

  const { error } = useNFCScanner(
    async (event) => {
      setCard(event.serialNumber)
    },
    [scanRetry]
  )

  return (
    <div className="flex items-center flex-col gap-4">
      <h1 className="font-bold text-2xl pb-2 text-center">Érints kártyát az eszközhöz...</h1>

      {error && (
        <Alert className="w-auto">
          <CircleX className="px-1" />
          <AlertTitle>NFC hiba</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
          <Button className="mt-2 w-full" onClick={() => setScanRetry((prev) => prev + 1)}>
            <RefreshCw className="mr-1 w-4 h-4" /> Újra
          </Button>
        </Alert>
      )}

      <BalanceCheck showVouchers={true} card={card} />
      {card && (
        <Button
          variant="secondary"
          className="w-full"
          onClick={() => {
            setCard(undefined)
          }}
        >
          Vissza
        </Button>
      )}
    </div>
  )
}

export default BalanceCheckPage
