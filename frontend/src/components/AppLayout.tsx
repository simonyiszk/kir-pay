import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs.tsx'
import { ReactNode, Suspense, useState } from 'react'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { setPersistentState, safeGetLocalStorage } from '@/lib/utils.ts'
import { AppHeader } from '@/components/AppHeader.tsx'

export const AppTab = ({ child, tab }: { child: ReactNode; tab: string }) => (
  <TabsContent value={tab}>
    <Suspense fallback={<LoadingIndicator />}>{child}</Suspense>
  </TabsContent>
)

export const AppTabTrigger = ({ child, tab }: { child: ReactNode; tab: string }) => (
  <TabsTrigger className="py-4 px-2 text-[.6rem] sm:text-base flex-1" value={tab}>
    {child}
  </TabsTrigger>
)

const getValidTab = (tabKey: string, visibleTabs: string[], defaultTab: string): string => {
  const stored = safeGetLocalStorage(tabKey)
  if (stored && visibleTabs.includes(stored)) {
    return stored
  }

  if (tabKey === 'terminalSelectedTab') {
    const oldStored = safeGetLocalStorage('selectedTab')
    if (oldStored !== null) {
      localStorage.removeItem('selectedTab')
      if (visibleTabs.includes(oldStored)) {
        localStorage.setItem(tabKey, oldStored)
        return oldStored
      }
    }
  }
  return defaultTab
}

export const AppLayout = ({
  tabs,
  tabTriggers,
  tabIcons,
  tabKey,
  defaultTab,
  visibleTabs
}: {
  tabs: () => ReactNode
  tabTriggers: () => ReactNode
  tabIcons: { [key: string]: ReactNode }
  tabKey: string
  defaultTab: string
  visibleTabs: string[]
}) => {
  const [currentTab, setCurrentTab] = useState(() => getValidTab(tabKey, visibleTabs, defaultTab))

  const currentTabIcon = tabIcons[currentTab]
  return (
    <Tabs
      onValueChange={setPersistentState(tabKey, setCurrentTab)}
      defaultValue={currentTab}
      className="max-w-6xl m-auto h-[100dvh] flex flex-col items-center relative overflow-hidden"
    >
      <main className="flex-1 w-full overflow-y-auto overflow-x-visible relative scrollbar-thin pt-4 px-4">
        <AppHeader />
        {tabs()}
      </main>
      <div className="w-full px-4 pb-4 pt-2">
        <TabsList className="flex justify-between p-0 ">{tabTriggers()}</TabsList>
      </div>
      {currentTabIcon && (
        <div
          aria-hidden={true}
          className="absolute overflow-hidden translate-x-0 -z-10 opacity-[1.75%] dark:opacity-[0.75%] scale-[20] bottom-[25%] right-[25%]"
        >
          {currentTabIcon}
        </div>
      )}
    </Tabs>
  )
}
