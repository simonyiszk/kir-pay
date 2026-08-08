import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { exportOrderLines, exportOrders, exportOrdersWithOrderLines } from '@/lib/api/admin.api.ts'
import { toast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'

export const OrderExportDropdown = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="secondary">Műveletek</Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        <DropdownMenuItem
          onClick={() =>
            exportToCsv('orders-with-order-lines.csv', () =>
              exportOrdersWithOrderLines().then((data) => {
                if (data.result === 'Ok') return data.data
                throw Error()
              })
            )
              .then(() => toast({ description: 'Rendelések exportálva' }))
              .catch(() => toast({ description: 'Hiba a rendelések exportálása közben' }))
          }
        >
          Rendelések exportálása rendeléssorokkal (Ezt látod a táblázatban)
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() =>
            exportToCsv('orders.csv', () =>
              exportOrders().then((data) => {
                if (data.result === 'Ok') return data.data
                throw Error()
              })
            )
              .then(() => toast({ description: 'Rendelések exportálva' }))
              .catch(() => toast({ description: 'Hiba a rendelések exportálása közben' }))
          }
        >
          Rendelések exportálása
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() =>
            exportToCsv('order-lines.csv', () =>
              exportOrderLines().then((data) => {
                if (data.result === 'Ok') return data.data
                throw Error()
              })
            )
              .then(() => toast({ description: 'rendeléssorok exportálva' }))
              .catch(() => toast({ description: 'Hiba a rendeléssorok exportálása közben' }))
          }
        >
          Rendeléssorok exportálása
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
