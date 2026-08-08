import { EnterAmountStep } from '@/page/terminal/common/EnterAmountStep.tsx'
import { Button } from '@/components/ui/button.tsx'
import { BalanceCheck } from '@/page/terminal/common/BalanceCheck.tsx'

export const TransferAmountEnterStep = ({
  sender,
  setAmount,
  onAbort
}: {
  sender: string
  setAmount: (card: number) => void
  onAbort?: () => void
}) => {
  return (
    <>
      <h1 className="font-bold text-2xl pb-2 text-center">Add meg az átruházandó mennyiséget!</h1>
      <BalanceCheck showVouchers={false} card={sender} />
      <div className="w-full">
        <EnterAmountStep setAmount={setAmount} />
        <Button variant="secondary" className="mt-2 w-full" onClick={onAbort}>
          Vissza
        </Button>
      </div>
    </>
  )
}
