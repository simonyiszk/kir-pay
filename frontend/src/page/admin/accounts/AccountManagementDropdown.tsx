import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createAccount, exportAccounts, exportAccountTemplate, importAccounts } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { Account } from '@/lib/api/model.ts'
import { AccountForm } from '@/page/admin/accounts/AccountForm.tsx'

const CreateAccountDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()
  const idempotencyKeyRef = useRef<string>(crypto.randomUUID())
  useEffect(() => {
    if (open) idempotencyKeyRef.current = crypto.randomUUID()
  }, [open])

  const { mutate, isPending } = useMutation({
    mutationFn: (account: Account) => createAccount({ ...account, idempotencyKey: idempotencyKeyRef.current }),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.AdminAccounts] })
        return
      }
      setError(data.error || 'A felhasználó létrehozása sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Felhasználó létrehozása</DialogTitle>
          <AccountForm
            error={error}
            loading={isPending}
            onAccountSubmitted={(account) => {
              setError(undefined)
              mutate(account)
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
    mutationFn: (csv: string) => importAccounts(csv, idempotencyKeyRef.current),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        toast({ description: 'Felhasználók importálása sikeres' })
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.AdminAccounts] })
      } else {
        toast({ description: `Hiba a felhasználók importálása közben: ${data.error ?? 'ismeretlen hiba'}` })
      }
    },
    onError: (e: Error) => toast({ description: `Hiba a felhasználók importálása közben: ${e?.message ?? 'ismeretlen hiba'}` })
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Felhasználók importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Felhasználókkat tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
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

export const AccountManagementDropdown = () => {
  const [importDialogOpen, setImportDialogOpen] = useState(false)
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const { toast } = useToast()

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="secondary">Műveletek</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onClick={() => setCreateDialogOpen(true)}>Létrehozás</DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('accounts.csv', () =>
                exportAccounts().then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error()
                })
              )
                .then(() => toast({ description: 'Felhasználók exportálva' }))
                .catch(() => toast({ description: 'Hiba a felhasználók exportálása közben' }))
            }
          >
            Exportálás
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('accounts-template.csv', () =>
                exportAccountTemplate().then((data) => {
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
      <CreateAccountDialog open={createDialogOpen} setOpen={setCreateDialogOpen} />
    </>
  )
}
