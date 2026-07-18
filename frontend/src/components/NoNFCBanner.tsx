import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { SmartphoneNfc } from 'lucide-react'
import { FC, PropsWithChildren } from 'react'

export const NoNFCBanner: FC<PropsWithChildren> = ({ children }) => {
  if (!('NDEFReader' in window)) {
    return (
      <div className="flex items-center justify-center w-full h-full min-h-screen p-4">
        <Alert className="w-auto">
          <SmartphoneNfc className="px-1" />
          <AlertTitle>Nem támogatott böngésző.</AlertTitle>
          <AlertDescription>Az alkalmazásnak szüksége van olyan böngészőre, amely támogatja a Web NFC-t.</AlertDescription>
        </Alert>
      </div>
    )
  }
  return <>{children}</>
}
