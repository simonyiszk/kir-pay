import { FC, PropsWithChildren, useState } from 'react'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { NoPermissionBanner } from '@/components/NoPermissionBanner.tsx'
import { useQuery } from '@tanstack/react-query'
import { AppContext, AppData } from '@/hooks/useAppContext'
import { LoginDialog } from '@/components/LoginDialog.tsx'
import { getAppData } from '@/lib/api/terminal.api.ts'
import { setPersistentState } from '@/lib/utils.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'

const TokenKey = 'AuthToken'

export const AppContextProvider: FC<PropsWithChildren> = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem(TokenKey))
  const persistentTokenSetter = setPersistentState(TokenKey, setToken)
  const appQuery = useQuery({
    queryKey: [AppQueryKeys.App, token],
    queryFn: () => getAppData(token!),
    enabled: !!TokenKey,
    select: (data) => {
      if (data.result !== 'Ok') {
        return null
      }

      return { ...data.data, token: token! } satisfies Omit<AppData, 'logOut'>
    },
    refetchInterval: 20000
  })

  if (!token) {
    return (
      <div className="flex justify-center items-center min-h-[100vh]">
        <LoginDialog setToken={persistentTokenSetter} />
      </div>
    )
  }

  if (appQuery.isLoading) {
    return (
      <div className="flex w-full h-[100vh] items-center justify-center">
        <LoadingIndicator />
      </div>
    )
  }

  if (!appQuery.data) {
    return <NoPermissionBanner goToLogin={() => persistentTokenSetter(null)} />
  }

  return <AppContext.Provider value={{ ...appQuery.data, logOut: () => persistentTokenSetter(null) }}>{children}</AppContext.Provider>
}
