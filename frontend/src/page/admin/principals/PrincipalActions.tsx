import { Principal } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { PrincipalForm } from '@/page/admin/principals/PrincipalForm.tsx'
import { deletePrincipal, disablePrincipal, enablePrincipal, updatePrincipal } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

const EditPrincipalDialog = ({ open, setOpen, principal }: { open: boolean; setOpen: (open: boolean) => void; principal: Principal }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Principal szerkesztése</DialogTitle>
          <PrincipalForm
            error={error}
            loading={loading}
            defaultPrincipal={{ ...principal, password: principal.secret }}
            onPrincipalSubmitted={(submittedPrincipal) => {
              if (principal.id === undefined) return
              setError(undefined)
              setLoading(true)
              updatePrincipal(token, principal.id, submittedPrincipal)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
                    return
                  }

                  setError(data.error || 'A principal szerkesztése sikertelen!')
                })
                .then(() => setLoading(false))
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

export const PrincipalActions = ({ principal }: { principal: Principal }) => {
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
              const action = principal.active ? disablePrincipal : enablePrincipal
              action(token, principal.id!).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
                } else {
                  toast({ description: (principal.active ? 'Letiltás' : 'Engedélyezés') + ' sikertelen' })
                }
              })
            }}
          >
            {principal.active ? 'Letiltás' : 'Engedélyezés'}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setUpdateDialogOpen(true)}>Szerkesztés</DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              deletePrincipal(token, principal.id!).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Principals] })
                  toast({ description: 'A principal törlése sikeres!' })
                } else {
                  toast({ description: res.error || 'A principal törlése sikertelen!' })
                }
              })
            }
          >
            Törlés
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EditPrincipalDialog principal={principal} open={updateDialogOpen} setOpen={setUpdateDialogOpen} />
    </>
  )
}
