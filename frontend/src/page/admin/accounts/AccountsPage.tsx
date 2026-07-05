import { Account, ValidatedApiCall } from '@/lib/api/model.ts'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery } from '@tanstack/react-query'
import { findAllAccounts } from '@/lib/api/terminal.api.ts'
import { AccountManagementDropdown } from '@/page/admin/accounts/AccountManagementDropdown.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table.tsx'
import { AccountActions } from '@/page/admin/accounts/AccountActions.tsx'
import { Check, X } from 'lucide-react'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'

const AccountsTable = ({ accounts }: { accounts?: ValidatedApiCall<Account[]> }) => {
  const { currencySymbol } = useAppContext().config
  if (!accounts) return null
  if (accounts.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (!accounts.data.length) return <h1 className="font-bold text-lg pb-4 text-center">Még nincs egyetlen felhasználó sem!</h1>

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Név</TableHead>
          <TableHead>E-mail cím</TableHead>
          <TableHead>Telefonszám</TableHead>
          <TableHead>Kártya</TableHead>
          <TableHead>Egyenleg</TableHead>
          <TableHead>Aktív</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {accounts.data.map((account) => {
          return (
            <TableRow key={account.id} className="relative overflow-clip">
              <TableCell>
                <ColorMarker color={account.color} />
                {account.name}
              </TableCell>
              <TableCell>{account.email || '-'}</TableCell>
              <TableCell>{account.phone || '-'}</TableCell>
              <TableCell>{account.card || '-'}</TableCell>
              <TableCell>
                {account.balance} {currencySymbol}
              </TableCell>
              <TableCell>{account.active ? <Check /> : <X />}</TableCell>
              <TableCell>
                <AccountActions account={account} />
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}

export const AccountsPage = () => {
  const { token } = useAppContext()
  const accounts = useQuery({
    queryKey: [AppQueryKeys.Accounts, token],
    queryFn: () => findAllAccounts(token),
    refetchInterval: DataRefetchInterval
  })

  return (
    <div className="flex-1 h-full relative">
      <div className="flex items-baseline justify-center py-6 gap-4">
        <h1 className="font-bold text-2xl pb-4 text-center">Felhasználók</h1>
        <AccountManagementDropdown />
      </div>
      {accounts.isLoading && <LoadingIndicator />}
      <AccountsTable accounts={accounts.data} />
    </div>
  )
}
