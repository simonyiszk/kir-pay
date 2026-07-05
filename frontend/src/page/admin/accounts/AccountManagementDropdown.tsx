import { useAppContext } from '@/hooks/useAppContext.ts'
import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createAccount, exportAccounts, exportAccountTemplate, importAccounts } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { AccountForm } from '@/page/admin/accounts/AccountForm.tsx'

const CreateAccountDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Felhasználó létrehozása</DialogTitle>
          <AccountForm
            error={error}
            loading={loading}
            onAccountSubmitted={(account) => {
              setError(undefined)
              setLoading(true)
              createAccount(token, account)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
                    return
                  }

                  setError(data.error || 'A felhasználó létrehozása sikertelen!')
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
          <DialogTitle>Felhasználók importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Felhasználókkat tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
        <Input id="csv" type="file" accept="text/csv" onChange={(e) => setFile(e.target?.files?.item(0) || undefined)} />
        <DialogFooter>
          <Button
            disabled={!file}
            onClick={async () => {
              if (!file) return
              file
                .text()
                .then((csv) => importAccounts(token, csv))
                .then((data) => {
                  if (data.result === 'Ok') {
                    toast({ description: 'Felhasználók importálása sikeres' })
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
                  } else {
                    toast({ description: `Hiba a felhasználók importálása közben: ${data.error}` })
                  }
                })
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
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('accounts.csv', () =>
                exportAccounts(token).then((data) => {
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
                exportAccountTemplate(token).then((data) => {
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
