import { Voucher } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteVoucher, incrementVoucherCount, updateVoucher } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

export const VoucherActions = ({ voucher }: { voucher: Voucher }) => {
  const { toast } = useToast()
  const queryClient = useQueryClient()

  const invalidate = () => queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })

  const incrementMutation = useMutation({
    mutationFn: (idempotencyKey: string) => incrementVoucherCount(voucher.id!, { delta: 1, idempotencyKey }),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        invalidate()
        toast({ description: 'Ajándékozás sikeres!' })
      } else {
        toast({ description: res.error || 'Ajándékozás sikertelen!' })
      }
    }
  })

  const decrementMutation = useMutation({
    mutationFn: (idempotencyKey: string) => incrementVoucherCount(voucher.id!, { delta: -1, idempotencyKey }),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        invalidate()
        toast({ description: 'Elvétel sikeres!' })
      } else {
        toast({ description: res.error || 'Elvétel sikertelen!' })
      }
    }
  })

  const clearMutation = useMutation({
    mutationFn: () => updateVoucher(voucher.id!, { count: 0 }),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        invalidate()
        toast({ description: 'Elvétel sikeres!' })
      } else {
        toast({ description: res.error || 'Elvétel sikertelen!' })
      }
    }
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteVoucher(voucher.id!),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        invalidate()
        toast({ description: 'Az utalvány törlése sikeres!' })
      } else {
        toast({ description: res.error || 'Az utalvány törlése sikertelen!' })
      }
    }
  })

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon">
            <Ellipsis />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onClick={() => incrementMutation.mutate(crypto.randomUUID())}>+1 Ajándékoz</DropdownMenuItem>
          {voucher.count > 0 && (
            <DropdownMenuItem onClick={() => decrementMutation.mutate(crypto.randomUUID())}>-1 Elvesz</DropdownMenuItem>
          )}
          {voucher.count > 0 && <DropdownMenuItem onClick={() => clearMutation.mutate()}>Összes elvétele</DropdownMenuItem>}
          <DropdownMenuItem onClick={() => deleteMutation.mutate()}>Törlés</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  )
}
