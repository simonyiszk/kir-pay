import { ValidatedApiCall, Voucher } from '@/lib/api/model.ts'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery } from '@tanstack/react-query'
import { findAllVouchers } from '@/lib/api/admin.api.ts'
import { VoucherManagementDropdown } from '@/page/admin/vouchers/VoucherManagementDropdown.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table.tsx'
import { VoucherActions } from '@/page/admin/vouchers/VoucherActions.tsx'
import { AccountNameView } from '@/page/admin/common/AccountView.tsx'
import { ItemNameView } from '@/page/admin/common/ItemView.tsx'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'

const VouchersTable = ({ vouchers }: { vouchers?: ValidatedApiCall<Voucher[]> }) => {
  if (!vouchers) return null
  if (vouchers.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (!vouchers.data.length) return <h1 className="font-bold text-lg pb-4 text-center">Még nincs egyetlen utalvány sem!</h1>

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Tulajdonos</TableHead>
          <TableHead>Termék</TableHead>
          <TableHead>Darabszám</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {vouchers.data.map((voucher) => {
          return (
            <TableRow key={voucher.id} className="relative overflow-clip">
              <TableCell>
                <AccountNameView accountId={voucher.accountId} />
              </TableCell>
              <TableCell>
                <ItemNameView itemId={voucher.itemId} />
              </TableCell>
              <TableCell>{voucher.count} darab</TableCell>
              <TableCell>
                <VoucherActions voucher={voucher} />
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}

export const VouchersPage = () => {
  const { token } = useAppContext()
  const vouchers = useQuery({
    queryKey: [AppQueryKeys.Vouchers, token],
    queryFn: () => findAllVouchers(token),
    refetchInterval: DataRefetchInterval,
    staleTime: DataRefetchInterval
  })

  return (
    <div className="flex-1 h-full relative">
      <div className="flex items-baseline justify-center py-6 gap-4">
        <h1 className="font-bold text-2xl pb-4 text-center">Utalványok</h1>
        <VoucherManagementDropdown />
      </div>
      {vouchers.isLoading && <LoadingIndicator />}
      <VouchersTable vouchers={vouchers.data} />
    </div>
  )
}
