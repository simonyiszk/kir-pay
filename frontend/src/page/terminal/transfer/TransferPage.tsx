import { useState } from 'react'
import { ScanCardStep } from '@/page/terminal/common/ScanCardStep.tsx'
import { TransferAmountEnterStep } from '@/page/terminal/transfer/TransferAmountEnterStep.tsx'
import { TransferStep } from '@/page/terminal/transfer/TransferStep.tsx'

const TransferPage = () => {
  const [sender, setSender] = useState<string>()
  const [recipient, setRecipient] = useState<string>()
  const [amount, setAmount] = useState<number>()

  const reset = () => {
    setAmount(undefined)
    setSender(undefined)
    setRecipient(undefined)
  }
  let currentStep
  if (!sender) {
    currentStep = <ScanCardStep setCard={setSender} message="Scanneld le a küldő kártyáját!" onAbort={reset} />
  } else if (!amount) {
    currentStep = <TransferAmountEnterStep sender={sender} setAmount={setAmount} onAbort={reset} />
  } else if (!recipient) {
    currentStep = (
      <ScanCardStep setCard={setRecipient} message="Scanneld le a szerencsés fogadó kártyáját!" amount={amount} onAbort={reset} />
    )
  } else {
    currentStep = <TransferStep amount={amount} recipient={recipient} sender={sender} onReset={reset} />
  }

  return <div className="flex items-center flex-col gap-4">{currentStep}</div>
}

export default TransferPage
