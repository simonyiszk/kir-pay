import { Principal } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { PrincipalForm } from '@/page/admin/principals/PrincipalForm.tsx'
import { deletePrincipal, disablePrincipal, enablePrincipal, updatePrincipal } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

const EditPrincipalDialog = ({ open, setOpen, principal }: { open: boolean; setOpen: (open: boolean) => void; principal: Principal }) => {
  const { toast } = useToast()
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()

  const { mutate, isPending } = useMutation({
    mutationFn: (submittedPrincipal: Parameters<typeof updatePrincipal>[1]) => updatePrincipal(principal.id!, submittedPrincipal),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
        return
      }
      setError(data.error || 'A principal szerkesztése sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Principal szerkesztése</DialogTitle>
          <PrincipalForm
            error={error}
            loading={isPending}
            defaultPrincipal={{ ...principal, password: '***' }}
            onPrincipalSubmitted={(submittedPrincipal) => {
              if (principal.id === undefined) {
                toast({ description: 'A principal szerkesztése sikertelen: hiányzó azonosító!' })
                return
              }
              setError(undefined)
              mutate(submittedPrincipal)
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

export const PrincipalActions = ({ principal }: { principal: Principal }) => {
  const { toast } = useToast()
  const queryClient = useQueryClient()
  const [updateDialogOpen, setUpdateDialogOpen] = useState(false)

  const toggleActiveMutation = useMutation({
    mutationFn: () => (principal.active ? disablePrincipal : enablePrincipal)(principal.id!),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
      } else {
        toast({ description: (principal.active ? 'Letiltás' : 'Engedélyezés') + ' sikertelen' })
      }
    }
  })

  const deleteMutation = useMutation({
    mutationFn: () => deletePrincipal(principal.id!),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
        toast({ description: 'A principal törlése sikeres!' })
      } else {
        toast({ description: res.error || 'A principal törlése sikertelen!' })
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
          <DropdownMenuItem onClick={() => toggleActiveMutation.mutate()}>
            {principal.active ? 'Letiltás' : 'Engedélyezés'}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setUpdateDialogOpen(true)}>Szerkesztés</DropdownMenuItem>
          <DropdownMenuItem onClick={() => deleteMutation.mutate()}>Törlés</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EditPrincipalDialog principal={principal} open={updateDialogOpen} setOpen={setUpdateDialogOpen} />
    </>
  )
}
