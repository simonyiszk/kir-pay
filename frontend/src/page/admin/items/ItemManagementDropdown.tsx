import { useAppContext } from '@/hooks/useAppContext.ts'
import { useToast } from '@/components/ui/use-toast.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { createItem, exportItems, exportItemTemplate, importItems } from '@/lib/api/admin.api.ts'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Input } from '@/components/ui/input.tsx'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { ItemForm } from '@/page/admin/items/ItemForm.tsx'

const CreateItemDialog = ({ open, setOpen }: { open: boolean; setOpen: (open: boolean) => void }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Termék létrehozása</DialogTitle>
          <ItemForm
            error={error}
            loading={loading}
            onItemSubmitted={(item) => {
              setError(undefined)
              setLoading(true)
              createItem(token, item)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
                    return
                  }

                  setError(data.error || 'A termék létrehozása sikertelen!')
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
          <DialogTitle>Termékek importálása</DialogTitle>
        </DialogHeader>
        <Label htmlFor="csv">Termékekket tartalmazó .csv file (tartalmazhat több oszlopot mint a template)</Label>
        <Input id="csv" type="file" accept="text/csv" onChange={(e) => setFile(e.target?.files?.item(0) || undefined)} />
        <DialogFooter>
          <Button
            disabled={!file}
            onClick={async () => {
              if (!file) return
              file
                .text()
                .then((csv) => importItems(token, csv))
                .then((data) => {
                  if (data.result === 'Ok') return data.data
                  throw Error(data.error)
                })
                .then(() => toast({ description: 'Termékek importálása sikeres' }))
                .then(() => setOpen(false))
                .then(() => queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] }))
                .catch((e: Error) => toast({ description: `Hiba a termékek importálása közben: ${e?.message}` }))
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
              exportToCsv('items.csv', () =>
                exportItems(token).then((data) => {
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
                exportItemTemplate(token).then((data) => {
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
