import { Item } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { ItemForm } from '@/page/admin/items/ItemForm.tsx'
import { deleteItem, disableItem, enableItem, updateItem } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

const EditItemDialog = ({ open, setOpen, item }: { open: boolean; setOpen: (open: boolean) => void; item: Item }) => {
  const { toast } = useToast()
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()

  const { mutate, isPending } = useMutation({
    mutationFn: (submittedItem: Item) => updateItem(item.id!, submittedItem),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
        return
      }
      setError(data.error || 'A termék szerkesztése sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Termék szerkesztése</DialogTitle>
          <ItemForm
            error={error}
            loading={isPending}
            defaultItem={item}
            onItemSubmitted={(submittedItem) => {
              if (item.id === undefined) {
                toast({ description: 'A termék szerkesztése sikertelen: hiányzó azonosító!' })
                return
              }
              setError(undefined)
              mutate(submittedItem)
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

export const ItemActions = ({ item }: { item: Item }) => {
  const { toast } = useToast()
  const queryClient = useQueryClient()
  const [updateDialogOpen, setUpdateDialogOpen] = useState(false)

  const toggleEnabledMutation = useMutation({
    mutationFn: () => (item.enabled ? disableItem : enableItem)(item.id!),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
      } else {
        toast({ description: (item.enabled ? 'Letiltás' : 'Engedélyezés') + ' sikertelen' })
      }
    }
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteItem(item.id!),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Items] })
        toast({ description: 'A termék törlése sikeres!' })
      } else {
        toast({ description: res.error || 'A termék törlése sikertelen!' })
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
          <DropdownMenuItem onClick={() => toggleEnabledMutation.mutate()}>{item.enabled ? 'Letiltás' : 'Engedélyezés'}</DropdownMenuItem>
          <DropdownMenuItem onClick={() => setUpdateDialogOpen(true)}>Szerkesztés</DropdownMenuItem>
          <DropdownMenuItem onClick={() => deleteMutation.mutate()}>Törlés</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EditItemDialog item={item} open={updateDialogOpen} setOpen={setUpdateDialogOpen} />
    </>
  )
}
