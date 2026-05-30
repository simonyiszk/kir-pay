import { useAppContext } from '@/hooks/useAppContext.ts'
import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createBatchVoucher, createVoucher, exportVouchers, exportVoucherTemplate, importVouchers } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { SingleVoucherForm } from '@/page/admin/vouchers/SingleVoucherForm.tsx'
import { BatchVoucherForm } from '@/page/admin/vouchers/BatchVoucherForm.tsx'

const CreateVoucherDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Utalvány létrehozása</DialogTitle>
          <SingleVoucherForm
            error={error}
            loading={loading}
            onVoucherSubmitted={(voucher) => {
              setError(undefined)
              setLoading(true)
              createVoucher(token, voucher)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
                    return
                  }

                  setError(data.error || 'A utalvány létrehozása sikertelen!')
                })
                .then(() => setLoading(false))
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

const CreateBatchVoucherDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Utalványok tömeges létrehozása</DialogTitle>
          <BatchVoucherForm
            error={error}
            loading={loading}
            onVoucherSubmitted={(voucher) => {
              setError(undefined)
              setLoading(true)
              createBatchVoucher(token, voucher)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] })
                    return
                  }

                  setError(data.error || 'A utalványok létrehozása sikertelen!')
                })
                .then(() => setLoading(false))
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

export const ImportDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const { token } = useAppContext()
  const [file, setFile] = useState<File>()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Utalványok importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Utalványokkat tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
        <Input id="csv" type="file" accept="text/csv" onChange={(e) => setFile(e.target?.files?.item(0) || undefined)} />
        <DialogFooter>
          <Button
            disabled={!file}
            onClick={async () => {
              if (!file) return
              file
                .text()
                .then((csv) => importVouchers(token, csv))
                .then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error(data.error)
                })
                .then(() => toast({ description: 'Utalványok importálása sikeres' }))
                .then(() => setOpen(false))
                .then(() => queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Vouchers] }))
                .catch((e: Error) => toast({ description: `Hiba a utalványok importálása közben: ${e?.message}` }))
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
  const { token } = useAppContext()
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
                exportVouchers(token).then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error()
                })
              )
                .then(() => toast({ description: 'Utalványok exportálva' }))
                .catch(() => toast({ description: 'Hiba a utalványok exportálása közben' }))
            }
          >
            Exportálás
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('vouchers-template.csv', () =>
                exportVoucherTemplate(token).then((data) => {
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
