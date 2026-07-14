import Lottie from '@/components/Lottie'
import checkAnimation from '@/assets/checkAnimation.json'
import { ReactNode, useEffect, useState } from 'react'
import { CashRegisterAudio } from '@/assets/CashRegisterAudio.ts'

export default function CheckAnimation({ children }: { children?: ReactNode }) {
  const [showAnimation, setShowAnimation] = useState<boolean>(true)

  useEffect(() => {
    new Audio(CashRegisterAudio).play()
  }, [])

  if (!showAnimation) return <>{children}</>

  return (
    <div className="w-full flex items-center justify-center">
      <Lottie
        animationData={checkAnimation}
        loop={false}
        onComplete={() => setShowAnimation(false)}
        style={{ width: '80%', maxHeight: '100%' }}
      />
    </div>
  )
}
