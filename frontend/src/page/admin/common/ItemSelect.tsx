import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { findAllItems } from '@/lib/api/admin.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select.tsx'
import { FormControl } from '@/components/ui/form.tsx'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'

export const ItemSelect = ({ onItemSelected }: { onItemSelected: (itemId: string) => void }) => {
  const { token } = useAppContext()
  const itemsQuery = useQuery({
    queryKey: [AppQueryKeys.Items, token],
    queryFn: () => findAllItems(token),
    refetchInterval: DataRefetchInterval,
    staleTime: DataRefetchInterval
  })
  if (itemsQuery.isLoading) return <LoadingIndicator />
  const items = itemsQuery.data
  if (!items || items.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  return (
    <Select onValueChange={onItemSelected}>
      <FormControl>
        <SelectTrigger>
          <SelectValue placeholder="Termék kiválasztása" />
        </SelectTrigger>
      </FormControl>
      <SelectContent>
        {items.data.map((item) => (
          <SelectItem key={item.id} value={item.id?.toString() || ''}>
            {item.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
