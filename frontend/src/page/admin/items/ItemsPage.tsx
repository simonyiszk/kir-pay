import { Item, ValidatedApiCall } from '@/lib/api/model.ts'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table.tsx'
import { Check, X } from 'lucide-react'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { findAllItems } from '@/lib/api/admin.api.ts'
import { ItemManagementDropdown } from '@/page/admin/items/ItemManagementDropdown.tsx'
import { ItemActions } from '@/page/admin/items/ItemActions.tsx'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'

const ItemsTable = ({ items }: { items?: ValidatedApiCall<Item[]> }) => {
  const { currencySymbol } = useAppContext().config

  if (!items) return null
  if (items.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (!items.data.length) return <h1 className="font-bold text-lg pb-4 text-center">Még nincs egyetlen termék sem!</h1>

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Név</TableHead>
          <TableHead>Alias</TableHead>
          <TableHead>Ár</TableHead>
          <TableHead>Mennyiség raktáron</TableHead>
          <TableHead>Elérhető</TableHead>
          <TableHead>Toplista</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.data.map((item) => {
          return (
            <TableRow key={item.id} className="relative overflow-clip">
              <TableCell>
                <ColorMarker color={item.color} />
                {item.name}
              </TableCell>
              <TableCell>{item.alias}</TableCell>
              <TableCell>
                {item.cost} {currencySymbol}
              </TableCell>
              <TableCell>{item.stock} darab</TableCell>
              <TableCell>{item.enabled ? <Check /> : <X />}</TableCell>
              <TableCell>{item.showOnLeaderboard ? <Check /> : <X />}</TableCell>
              <TableCell>
                <ItemActions item={item} />
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}

export const ItemsPage = () => {
  const { token } = useAppContext()
  const items = useQuery({
    queryKey: [AppQueryKeys.Items, token],
    queryFn: () => findAllItems(token),
    refetchInterval: DataRefetchInterval,
    staleTime: DataRefetchInterval
  })

  return (
    <div className="flex-1 h-full relative">
      <div className="flex items-baseline justify-center py-6 gap-4">
        <h1 className="font-bold text-2xl pb-4 text-center">Termékek</h1>
        <ItemManagementDropdown />
      </div>
      {items.isLoading && <LoadingIndicator />}
      <ItemsTable items={items.data} />
    </div>
  )
}
