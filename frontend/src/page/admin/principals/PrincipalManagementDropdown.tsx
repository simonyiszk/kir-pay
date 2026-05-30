import { useAppContext } from '@/hooks/useAppContext.ts'
import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createPrincipal, exportPrincipals, exportPrincipalTemplate, importPrincipals } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { PrincipalForm } from '@/page/admin/principals/PrincipalForm.tsx'

const CreatePrincipalDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Principal létrehozása</DialogTitle>
          <PrincipalForm
            error={error}
            loading={loading}
            onPrincipalSubmitted={(principal) => {
              setError(undefined)
              setLoading(true)
              createPrincipal(token, principal)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
                    return
                  }

                  setError(data.error || 'A principal létrehozása sikertelen!')
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
          <DialogTitle>Principalok importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Principalokkat tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
        <Input id="csv" type="file" accept="text/csv" onChange={(e) => setFile(e.target?.files?.item(0) || undefined)} />
        <DialogFooter>
          <Button
            disabled={!file}
            onClick={async () => {
              if (!file) return
              file
                .text()
                .then((csv) => importPrincipals(token, csv))
                .then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error(data.error)
                })
                .then(() => toast({ description: 'Principalok importálása sikeres' }))
                .then(() => setOpen(false))
                .then(() => queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] }))
                .catch((e: Error) => toast({ description: `Hiba a principalok importálása közben: ${e?.message}` }))
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

export const PrincipalManagementDropdown = () => {
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
              exportToCsv('principals.csv', () =>
                exportPrincipals(token).then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error()
                })
              )
                .then(() => toast({ description: 'Principalok exportálva' }))
                .catch(() => toast({ description: 'Hiba a principalok exportálása közben' }))
            }
          >
            Exportálás
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('principals-template.csv', () =>
                exportPrincipalTemplate(token).then((data) => {
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
      <CreatePrincipalDialog open={createDialogOpen} setOpen={setCreateDialogOpen} />
    </>
  )
}
