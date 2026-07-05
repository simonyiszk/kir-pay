import { Voucher } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQueryClient } from '@tanstack/react-query'
import { deleteVoucher, updateVoucher } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

export const VoucherActions = ({ voucher }: { voucher: Voucher }) => {
  const { toast } = useToast()
  const { token } = useAppContext()
  const queryClient = useQueryClient()

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon">
            <Ellipsis />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem
            onClick={() =>
              updateVoucher(token, voucher.id!, { count: voucher.count + 1 }).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
                  toast({ description: 'Ajándékozás sikeres!' })
                } else {
                  toast({ description: res.error || 'Ajándékozás sikertelen!' })
                }
              })
            }
          >
            +1 Ajándékoz
          </DropdownMenuItem>
          {voucher.count > 0 && (
            <DropdownMenuItem
              onClick={() =>
                updateVoucher(token, voucher.id!, { count: voucher.count - 1 }).then((res) => {
                  if (res.result === 'Ok') {
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
                    toast({ description: 'Elvétel sikeres!' })
                  } else {
                    toast({ description: res.error || 'Elvétel sikertelen!' })
                  }
                })
              }
            >
              -1 Elvesz
            </DropdownMenuItem>
          )}
          {voucher.count > 0 && (
            <DropdownMenuItem
              onClick={() =>
                updateVoucher(token, voucher.id!, { count: 0 }).then((res) => {
                  if (res.result === 'Ok') {
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
                    toast({ description: 'Elvétel sikeres!' })
                  } else {
                    toast({ description: res.error || 'Elvétel sikertelen!' })
                  }
                })
              }
            >
              Összes elvétele
            </DropdownMenuItem>
          )}
          <DropdownMenuItem
            onClick={() =>
              deleteVoucher(token, voucher.id!).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
                  toast({ description: 'A utalvány törlése sikeres!' })
                } else {
                  toast({ description: res.error || 'A utalvány törlése sikertelen!' })
                }
              })
            }
          >
            Törlés
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  )
}
