import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createPrincipal, exportPrincipals, exportPrincipalTemplate, importPrincipals } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { PrincipalDto } from '@/lib/api/model.ts'
import { PrincipalForm } from '@/page/admin/principals/PrincipalForm.tsx'

const CreatePrincipalDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()
  const idempotencyKeyRef = useRef<string>(crypto.randomUUID())
  useEffect(() => {
    if (open) idempotencyKeyRef.current = crypto.randomUUID()
  }, [open])

  const { mutate, isPending } = useMutation({
    mutationFn: (principal: PrincipalDto) => createPrincipal({ ...principal, idempotencyKey: idempotencyKeyRef.current }),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
        return
      }
      setError(data.error || 'A principal létrehozása sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Principal létrehozása</DialogTitle>
          <PrincipalForm
            error={error}
            loading={isPending}
            onPrincipalSubmitted={(principal) => {
              setError(undefined)
              mutate(principal)
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
    mutationFn: (csv: string) => importPrincipals(csv, idempotencyKeyRef.current),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        toast({ description: 'Principalok importálása sikeres' })
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
      } else {
        toast({ description: `Hiba a principalok importálása közben: ${data.error ?? 'ismeretlen hiba'}` })
      }
    },
    onError: (e: Error) => toast({ description: `Hiba a principalok importálása közben: ${e?.message ?? 'ismeretlen hiba'}` })
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Principalok importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Principalokkat tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
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

export const PrincipalManagementDropdown = () => {
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
              exportToCsv('principals.csv', () =>
                exportPrincipals().then((data) => {
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
                exportPrincipalTemplate().then((data) => {
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
