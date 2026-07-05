import { Item } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { ItemForm } from '@/page/admin/items/ItemForm.tsx'
import { deleteItem, disableItem, enableItem, updateItem } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

const EditItemDialog = ({ open, setOpen, item }: { open: boolean; setOpen: (open: boolean) => void; item: Item }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Termék szerkesztése</DialogTitle>
          <ItemForm
            error={error}
            loading={loading}
            defaultItem={item}
            onItemSubmitted={(submittedItem) => {
              if (item.id === undefined) return
              setError(undefined)
              setLoading(true)
              updateItem(token, item.id, submittedItem)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
                    return
                  }

                  setError(data.error || 'A termék szerkesztése sikertelen!')
                })
                .then(() => setLoading(false))
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

export const ItemActions = ({ item }: { item: Item }) => {
  const { toast } = useToast()
  const { token } = useAppContext()
  const queryClient = useQueryClient()
  const [updateDialogOpen, setUpdateDialogOpen] = useState(false)

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
            onClick={() => {
              const action = item.enabled ? disableItem : enableItem
              action(token, item.id!).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
                } else {
                  toast({ description: (item.enabled ? 'Letiltás' : 'Engedélyezés') + ' sikertelen' })
                }
              })
            }}
          >
            {item.enabled ? 'Letiltás' : 'Engedélyezés'}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setUpdateDialogOpen(true)}>Szerkesztés</DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              deleteItem(token, item.id!).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
                  toast({ description: 'A termék törlése sikeres!' })
                } else {
                  toast({ description: res.error || 'A termék törlése sikertelen!' })
                }
              })
            }
          >
            Törlés
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EditItemDialog item={item} open={updateDialogOpen} setOpen={setUpdateDialogOpen} />
    </>
  )
}
