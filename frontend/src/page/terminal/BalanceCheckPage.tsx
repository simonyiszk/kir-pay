import { useState } from 'react'
import { useNFCScanner } from '@/lib/utils.ts'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'
import { Button } from '@/components/ui/button.tsx'

const BalanceCheckPage = () => {
  const [card, setCard] = useState<string>()
  const [loading, setLoading] = useState<boolean>(false)

  useNFCScanner(
    async (event) => {
      if (loading) return
      setCard(event.serialNumber)
    },
    [loading]
  )

  return (
    <div className="flex items-center flex-col gap-4">
      <h1 className="font-bold text-2xl pb-2 text-center">Érints kártyát az eszközhöz...</h1>

      <BalanceCheck showVouchers={true} card={card} loading={loading} setLoading={setLoading} />
      {card && !loading && (
        <Button
          variant="secondary"
          className="w-full"
          onClick={() => {
            setCard(undefined)
            setLoading(false)
          }}
        >
          Vissza
        </Button>
      )}
    </div>
  )
}

export default BalanceCheckPage
