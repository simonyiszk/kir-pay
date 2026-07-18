import { useAppContext } from '@/hooks/useAppContext.ts'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { Link } from '@tanstack/react-router'
import { buttonVariants } from '@/components/ui/button.props.ts'
import { AppLayout, AppTab, AppTabTrigger } from '@/components/AppLayout.tsx'
import { ReactNode } from 'react'
import { ChartNoAxesCombined, ListOrdered, Logs, ShoppingBasket, SquareUser, TicketCheck, WalletMinimal } from 'lucide-react'
import { AnalyticsPage } from '@/page/admin/analytics/AnalyticsPage.tsx'
import { PrincipalsPage } from '@/page/admin/principals/PrincipalsPage.tsx'
import { AccountsPage } from '@/page/admin/accounts/AccountsPage.tsx'
import { ItemsPage } from '@/page/admin/items/ItemsPage.tsx'
import { OrderPage } from '@/page/admin/orders/OrderPage.tsx'
import { VouchersPage } from '@/page/admin/vouchers/VouchersPage.tsx'
import { TransactionsPage } from '@/page/admin/transactions/TransactionsPage.tsx'

const TabKey = 'adminSelectedTab'

const TabIcons: { [key: string]: ReactNode } = {
  analytics: <ChartNoAxesCombined />,
  principals: <SquareUser />,
  accounts: <WalletMinimal />,
  items: <ShoppingBasket />,
  ordersLines: <ListOrdered />,
  vouchers: <TicketCheck />,
  transactions: <Logs />
}

const AdminRoot = () => {
  const { principal } = useAppContext()

  if (principal.role !== 'ADMIN') {
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

  return (
    <AppLayout
      tabKey={TabKey}
      defaultTab="analytics"
      tabIcons={TabIcons}
      tabTriggers={() => (
        <>
          <AppTabTrigger key="analytics" tab="analytics" child={TabIcons['analytics']} />
          <AppTabTrigger key="principals" tab="principals" child={TabIcons['principals']} />
          <AppTabTrigger key="accounts" tab="accounts" child={TabIcons['accounts']} />
          <AppTabTrigger key="items" tab="items" child={TabIcons['items']} />
          <AppTabTrigger key="ordersLines" tab="ordersLines" child={TabIcons['ordersLines']} />
          <AppTabTrigger key="vouchers" tab="vouchers" child={TabIcons['vouchers']} />
          <AppTabTrigger key="transactions" tab="transactions" child={TabIcons['transactions']} />
        </>
      )}
      tabs={() => (
        <>
          <AppTab key="analytics" tab="analytics" child={<AnalyticsPage />} />
          <AppTab key="principals" tab="principals" child={<PrincipalsPage />} />
          <AppTab key="accounts" tab="accounts" child={<AccountsPage />} />
          <AppTab key="items" tab="items" child={<ItemsPage />} />
          <AppTab key="ordersLines" tab="ordersLines" child={<OrderPage />} />
          <AppTab key="vouchers" tab="vouchers" child={<VouchersPage />} />
          <AppTab key="transactions" tab="transactions" child={<TransactionsPage />} />
        </>
      )}
    />
  )
}

export default AdminRoot
