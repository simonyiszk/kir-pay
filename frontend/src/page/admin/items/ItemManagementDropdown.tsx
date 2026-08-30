import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createItem, exportItems, exportItemTemplate, importItems } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { Item } from '@/lib/api/model.ts'
import { ItemForm } from '@/page/admin/items/ItemForm.tsx'

const CreateItemDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()
  const idempotencyKeyRef = useRef<string>(crypto.randomUUID())
  useEffect(() => {
    if (open) idempotencyKeyRef.current = crypto.randomUUID()
  }, [open])

  const { mutate, isPending } = useMutation({
    mutationFn: (item: Item) => createItem({ ...item, idempotencyKey: idempotencyKeyRef.current }),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
        return
      }
      setError(data.error || 'A termék létrehozása sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Termék létrehozása</DialogTitle>
          <ItemForm
            error={error}
            loading={isPending}
            onItemSubmitted={(item) => {
              setError(undefined)
              mutate(item)
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
    mutationFn: (csv: string) => importItems(csv, idempotencyKeyRef.current),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        toast({ description: 'Termékek importálása sikeres' })
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
      } else {
        toast({ description: `Hiba a termékek importálása közben: ${data.error ?? 'ismeretlen hiba'}` })
      }
    },
    onError: (e: Error) => toast({ description: `Hiba a termékek importálása közben: ${e?.message ?? 'ismeretlen hiba'}` })
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Termékek importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Termékekket tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
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

export const ItemManagementDropdown = () => {
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
              exportToCsv('items.csv', () =>
                exportItems().then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error()
                })
              )
                .then(() => toast({ description: 'Termékek exportálva' }))
                .catch(() => toast({ description: 'Hiba a termékek exportálása közben' }))
            }
          >
            Exportálás
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              exportToCsv('items-template.csv', () =>
                exportItemTemplate().then((data) => {
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
      <CreateItemDialog open={createDialogOpen} setOpen={setCreateDialogOpen} />
    </>
  )
}
