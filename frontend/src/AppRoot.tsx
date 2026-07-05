import { EnableRotatedForCustomerProvider } from '@/components/EnableRotatedForCustomerContext.tsx'
import { ThemeProvider } from '@/components/Theme.tsx'
import { FC, PropsWithChildren } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppContextProvider } from '@/components/AppContext.tsx'
import { Toaster } from '@/components/ui/toaster.tsx'

const queryClient = new QueryClient()

export const AppRoot: FC<PropsWithChildren> = ({ children }) => (
  <EnableRotatedForCustomerProvider>
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AppContextProvider>
          {children}
          <Toaster />
        </AppContextProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </EnableRotatedForCustomerProvider>
)
