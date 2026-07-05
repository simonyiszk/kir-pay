import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select.tsx'
import { FormControl } from '@/components/ui/form.tsx'
import { findAllAccounts } from '@/lib/api/terminal.api.ts'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'

export const AccountSelect = ({ onAccountSelected }: { onAccountSelected: (accountId: string) => void }) => {
  const { token } = useAppContext()
  const accountsQuery = useQuery({
    queryKey: [AppQueryKeys.Accounts, token],
    queryFn: () => findAllAccounts(token),
    refetchInterval: DataRefetchInterval,
    staleTime: DataRefetchInterval
  })
  if (accountsQuery.isLoading) return <LoadingIndicator />
  const accounts = accountsQuery.data
  if (!accounts || accounts.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  return (
    <Select onValueChange={onAccountSelected}>
      <FormControl>
        <SelectTrigger>
          <SelectValue placeholder="Felhasználó kiválasztása" />
        </SelectTrigger>
      </FormControl>
      <SelectContent>
        {accounts.data.map((account) => (
          <SelectItem key={account.id} value={account.id?.toString() || ''}>
            {account.name} {account.email}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
