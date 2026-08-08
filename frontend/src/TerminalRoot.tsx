import { useAppContext } from '@/hooks/useAppContext'
import { ArrowLeftRight, ArrowUpFromLine, CircleDollarSign, CircleHelp, Link, ShoppingBasket, TicketCheck } from 'lucide-react'
import { lazy, ReactNode } from 'react'
import { AppLayout, AppTab, AppTabTrigger } from '@/components/AppLayout.tsx'

const SetCardPage = lazy(() => import('@/page/terminal/set-card/SetCardPage.tsx'))
const UploadPage = lazy(() => import('@/page/terminal/upload/UploadPage.tsx'))
const PayPage = lazy(() => import('@/page/terminal/pay/PayPage.tsx'))
const TransferPage = lazy(() => import('@/page/terminal/transfer/TransferPage.tsx'))
const ItemsPage = lazy(() => import('@/page/terminal/items/ItemsPage.tsx'))
const BalanceCheckPage = lazy(() => import('@/page/terminal/BalanceCheckPage.tsx'))
const TokensPage = lazy(() => import('@/page/terminal/tokens/TokensPage.tsx'))

const TabKey = 'terminalSelectedTab'

const BALANCE_ICON = <CircleHelp />
const ASSIGN_ICON = <Link />
const PAY_ICON = <CircleDollarSign />
const TRANSFER_ICON = <ArrowLeftRight />
const UPLOAD_ICON = <ArrowUpFromLine />
const ITEMS_ICON = <ShoppingBasket />
const TOKENS_ICON = <TicketCheck />

interface TabConfig {
  key: string
  icon: ReactNode
  visible: boolean
  component: ReactNode
}

export const TerminalRoot = () => {
  const { config, principal } = useAppContext()
  const { showBalanceTab, showCartTab, showPayTab, showSetCardTab, showTokenTab, showUploadTab, showTransferTab } = config

  const { canAssignCards, canRedeemVouchers, canSellItems, canTransfer, canUpload } = principal

  const tabConfigs: TabConfig[] = [
    { key: 'balance', icon: BALANCE_ICON, visible: showBalanceTab, component: <BalanceCheckPage /> },
    { key: 'assign', icon: ASSIGN_ICON, visible: showSetCardTab && canAssignCards, component: <SetCardPage /> },
    { key: 'pay', icon: PAY_ICON, visible: showPayTab && canSellItems, component: <PayPage /> },
    { key: 'transfer', icon: TRANSFER_ICON, visible: showTransferTab && canTransfer, component: <TransferPage /> },
    { key: 'upload', icon: UPLOAD_ICON, visible: showUploadTab && canUpload, component: <UploadPage /> },
    { key: 'items', icon: ITEMS_ICON, visible: showCartTab && canSellItems, component: <ItemsPage /> },
    { key: 'tokens', icon: TOKENS_ICON, visible: showTokenTab && canRedeemVouchers, component: <TokensPage /> }
  ]

  const visibleTabs = tabConfigs.filter((t) => t.visible).map((t) => t.key)

  const tabIcons: { [key: string]: ReactNode } = Object.fromEntries(tabConfigs.map((t) => [t.key, t.icon]))

  return (
    <AppLayout
      tabKey={TabKey}
      defaultTab="balance"
      tabIcons={tabIcons}
      visibleTabs={visibleTabs}
      tabTriggers={() => <>{tabConfigs.map((t) => t.visible && <AppTabTrigger key={t.key} tab={t.key} child={t.icon} />)}</>}
      tabs={() => <>{tabConfigs.map((t) => t.visible && <AppTab key={t.key} tab={t.key} child={t.component} />)}</>}
    />
  )
}
