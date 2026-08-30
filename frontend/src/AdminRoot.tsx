import { useAppContext } from '@/hooks/useAppContext.ts'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { Link } from '@tanstack/react-router'
import { buttonVariants } from '@/components/ui/button.props.ts'
import { AppLayout, AppTab, AppTabTrigger } from '@/components/AppLayout.tsx'
import { ReactNode } from 'react'
import { ChartNoAxesCombined, ListOrdered, Logs, Monitor, ShoppingBasket, SquareUser, TicketCheck, WalletMinimal } from 'lucide-react'
import { AnalyticsPage } from '@/page/admin/analytics/AnalyticsPage.tsx'
import { PrincipalsPage } from '@/page/admin/principals/PrincipalsPage.tsx'
import { AccountsPage } from '@/page/admin/accounts/AccountsPage.tsx'
import { ItemsPage } from '@/page/admin/items/ItemsPage.tsx'
import { OrderPage } from '@/page/admin/orders/OrderPage.tsx'
import { VouchersPage } from '@/page/admin/vouchers/VouchersPage.tsx'
import { TransactionsPage } from '@/page/admin/transactions/TransactionsPage.tsx'
import { SessionsPage } from '@/page/admin/sessions/SessionsPage.tsx'

const TabKey = 'adminSelectedTab'

const ANALYTICS_ICON = <ChartNoAxesCombined />
const PRINCIPALS_ICON = <SquareUser />
const ACCOUNTS_ICON = <WalletMinimal />
const ITEMS_ICON = <ShoppingBasket />
const ORDERS_ICON = <ListOrdered />
const VOUCHERS_ICON = <TicketCheck />
const TRANSACTIONS_ICON = <Logs />
const SESSIONS_ICON = <Monitor />

interface TabConfig {
  key: string
  icon: ReactNode
  component: ReactNode
}

const AdminRoot = () => {
  const { principal } = useAppContext()

  if (principal?.role !== 'ADMIN') {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Card className="w-auto">
          <CardHeader>
            <CardTitle>Te nem vagy admin!</CardTitle>
          </CardHeader>
          <CardContent>Nincs itt semmi látnivaló!</CardContent>
          <CardFooter>
            <Link className={buttonVariants()} to="/">
              Inkább lelépek
            </Link>
          </CardFooter>
        </Card>
      </div>
    )
  }

  const tabConfigs: TabConfig[] = [
    { key: 'analytics', icon: ANALYTICS_ICON, component: <AnalyticsPage /> },
    { key: 'principals', icon: PRINCIPALS_ICON, component: <PrincipalsPage /> },
    { key: 'accounts', icon: ACCOUNTS_ICON, component: <AccountsPage /> },
    { key: 'items', icon: ITEMS_ICON, component: <ItemsPage /> },
    { key: 'ordersLines', icon: ORDERS_ICON, component: <OrderPage /> },
    { key: 'vouchers', icon: VOUCHERS_ICON, component: <VouchersPage /> },
    { key: 'transactions', icon: TRANSACTIONS_ICON, component: <TransactionsPage /> },
    { key: 'sessions', icon: SESSIONS_ICON, component: <SessionsPage /> }
  ]

  const tabIcons: { [key: string]: ReactNode } = Object.fromEntries(tabConfigs.map((t) => [t.key, t.icon]))
  const visibleTabs = tabConfigs.map((t) => t.key)

  return (
    <AppLayout
      tabKey={TabKey}
      defaultTab="analytics"
      tabIcons={tabIcons}
      visibleTabs={visibleTabs}
      tabTriggers={() => (
        <>
          {tabConfigs.map((t) => (
            <AppTabTrigger key={t.key} tab={t.key} child={t.icon} />
          ))}
        </>
      )}
      tabs={() => (
        <>
          {tabConfigs.map((t) => (
            <AppTab key={t.key} tab={t.key} child={t.component} />
          ))}
        </>
      )}
    />
  )
}

export default AdminRoot
