import { FC, PropsWithChildren, useState } from 'react'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { AppContext } from '@/hooks/useAppContext'
import { LoginDialog } from '@/components/LoginDialog.tsx'
import { getAppData } from '@/lib/api/terminal.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { ValidatedApiCall } from '@/lib/api/model.ts'
import { Card, CardFooter, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { Button } from '@/components/ui/button.tsx'

const isAuthError = (error: unknown) => {
  const result = (error as ValidatedApiCall<unknown> | null)?.result
  return result === 'Unauthorized' || result === 'Forbidden'
}

export const AppContextProvider: FC<PropsWithChildren> = ({ children }) => {
  const [forceLogin, setForceLogin] = useState(false)
  const queryClient = useQueryClient()

  const appQuery = useQuery({
    queryKey: [AppQueryKeys.App],
    queryFn: async () => {
      const result = await getAppData()
      if (result.result !== 'Ok') {
        throw result
      }
      return result.data
    },
    placeholderData: (prev) => prev,
    refetchInterval: (query) => (isAuthError(query.state.error) ? false : 20000),
    retry: false
  })

  const error = appQuery.isError ? (appQuery.error as unknown as ValidatedApiCall<unknown> | null) : null
  const showLogin = forceLogin || isAuthError(error)

  if (showLogin) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <LoginDialog
          onLogin={() => {
            setForceLogin(false)
            queryClient.resetQueries()
          }}
        />
      </div>
    )
  }

  if (appQuery.isLoading && !appQuery.data) {
    return (
      <div className="flex w-full h-screen items-center justify-center">
        <LoadingIndicator />
      </div>
    )
  }

  if (!appQuery.data && error) {
    return (
      <div className="flex items-center justify-center w-full h-full min-h-screen p-4">
        <Card>
          <CardHeader>
            <CardTitle>Hiba történt</CardTitle>
          </CardHeader>
          <CardFooter>
            <Button onClick={() => setForceLogin(true)}>Vissza a belépéshez</Button>
          </CardFooter>
        </Card>
      </div>
    )
  }

  if (!appQuery.data) {
    return (
      <div className="flex w-full h-screen items-center justify-center">
        <LoadingIndicator />
      </div>
    )
  }

  return (
    <AppContext.Provider value={appQuery.data}>
      {appQuery.isError && (
        <div className="fixed top-0 left-0 right-0 z-50 bg-yellow-500 text-black text-center py-1 text-sm font-medium">
          Újracsatlakozás...
        </div>
      )}
      {children}
    </AppContext.Provider>
  )
}
