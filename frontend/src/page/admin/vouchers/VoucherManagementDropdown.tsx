import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createBatchVoucher, createVoucher, exportVouchers, exportVoucherTemplate, importVouchers } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { BatchVoucherDto } from '@/lib/api/model.ts'
import { SingleVoucherForm } from '@/page/admin/vouchers/SingleVoucherForm.tsx'
import { BatchVoucherForm } from '@/page/admin/vouchers/BatchVoucherForm.tsx'

const CreateVoucherDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()

  const { mutate, isPending } = useMutation({
    mutationFn: (voucher: Parameters<typeof createVoucher>[0]) => createVoucher(voucher),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
        return
      }
      setError(data.error || 'Az utalvány létrehozása sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Utalvány létrehozása</DialogTitle>
          <SingleVoucherForm
            error={error}
            loading={isPending}
            onVoucherSubmitted={(voucher) => {
              setError(undefined)
              mutate(voucher)
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

const CreateBatchVoucherDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()
  const idempotencyKeyRef = useRef<string>(crypto.randomUUID())
  useEffect(() => {
    if (open) idempotencyKeyRef.current = crypto.randomUUID()
  }, [open])

  const { mutate, isPending } = useMutation({
    mutationFn: (voucher: BatchVoucherDto) => createBatchVoucher({ ...voucher, idempotencyKey: idempotencyKeyRef.current }),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
        return
      }
      setError(data.error || 'Az utalványok létrehozása sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Utalványok tömeges létrehozása</DialogTitle>
          <BatchVoucherForm
            error={error}
            loading={isPending}
            onVoucherSubmitted={(voucher) => {
              setError(undefined)
              mutate(voucher)
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

const ImportDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [file, setFile] = useState<File>()
  const idempotencyKeyRef = useRef<string>(crypto.randomUUID())
  useEffect(() => {
    if (open) idempotencyKeyRef.current = crypto.randomUUID()
  }, [open])

  const { mutate, isPending } = useMutation({
    mutationFn: (csv: string) => importVouchers(csv, idempotencyKeyRef.current),
    onSuccess: (data) => {
      if (data.result !== 'Ok') {
        toast({ description: `Hiba az utalványok importálása közben: ${data.error ?? 'ismeretlen hiba'}` })
        return
      }
      const { imported, total, errors } = data.data
      if (errors.length > 0) {
        toast({ description: `Részleges siker: ${imported}/${total} importálva. Hibák: ${errors.join('; ')}` })
        return
      }
      toast({ description: `Utalványok importálása sikeres: ${imported}/${total}` })
      setOpen(false)
      queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
    },
    onError: (e: Error) => toast({ description: `Hiba az utalványok importálása közben: ${e?.message ?? 'ismeretlen hiba'}` })
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Utalványok importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Utalványokkat tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
        <Input
          id="csv"
          type="file"
          accept="text/csv"
          onChange={(e) => {
            idempotencyKeyRef.current = crypto.randomUUID()
            setFile(e.target?.files?.item(0) || undefined)
          }}
        />
        <DialogFooter>
          <Button
            disabled={!file || isPending}
            onClick={async () => {
              if (!file) return
              const csv = await file.text()
              mutate(csv)
            }}
            type="button"
          >
            Import
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export const VoucherManagementDropdown = () => {
  const [importDialogOpen, setImportDialogOpen] = useState(false)
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [createBatchDialogOpen, setCreateBatchDialogOpen] = useState(false)
  const { toast } = useToast()

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="secondary">Műveletek</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onClick={() => setCreateDialogOpen(true)}>Létrehozás</DropdownMenuItem>
          <DropdownMenuItem onClick={() => setCreateBatchDialogOpen(true)}>Tömeges Létrehozás</DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('vouchers.csv', () =>
                exportVouchers().then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error()
                })
              )
                .then(() => toast({ description: 'Utalványok exportálva' }))
                .catch(() => toast({ description: 'Hiba az utalványok exportálása közben' }))
            }
          >
            Exportálás
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('vouchers-template.csv', () =>
                exportVoucherTemplate().then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error()
                })
              )
                .then(() => toast({ description: 'Template exportálva' }))
                .catch(() => toast({ description: 'Hiba a template exportálása közben' }))
            }
          >
            Import template letöltése
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setImportDialogOpen(true)}>Importálás</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <ImportDialog open={importDialogOpen} setOpen={setImportDialogOpen} />
      <CreateVoucherDialog open={createDialogOpen} setOpen={setCreateDialogOpen} />
      <CreateBatchVoucherDialog open={createBatchDialogOpen} setOpen={setCreateBatchDialogOpen} />
    </>
  )
}
