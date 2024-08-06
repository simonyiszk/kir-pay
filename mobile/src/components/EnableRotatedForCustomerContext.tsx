import { FC, PropsWithChildren, useState } from 'react'
import { RotateEnabledContext } from '@/hooks/useEnableRotatedForCustomer.ts'

const EnabledKey = 'enableRotated'
const getEnabled = () => localStorage.getItem(EnabledKey) != 'no'
const saveEnabled = (value: boolean) => localStorage.setItem(EnabledKey, value ? 'yes' : 'no')

export const EnableRotatedForCustomerProvider: FC<PropsWithChildren> = ({ children }) => {
  const [rotateEnabled, setRotateEnabled] = useState(getEnabled())

  return (
    <RotateEnabledContext.Provider
      value={{
        rotateEnabled,
        setRotateEnabled: (enabled) => {
          saveEnabled(enabled)
          setRotateEnabled(enabled)
        }
      }}
    >
      {children}
    </RotateEnabledContext.Provider>
  )
}
