import { Account } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { AccountForm } from '@/page/admin/accounts/AccountForm.tsx'
import { deleteAccount, disableAccount, enableAccount, updateAccount } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

const EditAccountDialog = ({ open, setOpen, account }: { open: boolean; setOpen: (open: boolean) => void; account: Account }) => {
  const { toast } = useToast()
  const [error, setError] = useState<string>()
  const queryClient = useQueryClient()

  const { mutate, isPending } = useMutation({
    mutationFn: (submittedAccount: Account) => updateAccount(account.id!, submittedAccount),
    onSuccess: (data) => {
      if (data.result === 'Ok') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.AdminAccounts] })
        return
      }
      setError(data.error || 'A felhasználó szerkesztése sikertelen!')
    }
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Felhasználó szerkesztése</DialogTitle>
          <AccountForm
            error={error}
            loading={isPending}
            defaultAccount={account}
            onAccountSubmitted={(submittedAccount) => {
              if (account.id === undefined) {
                toast({ description: 'A felhasználó szerkesztése sikertelen: hiányzó azonosító!' })
                return
              }
              setError(undefined)
              mutate(submittedAccount)
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

export const AccountActions = ({ account }: { account: Account }) => {
  const { toast } = useToast()
  const queryClient = useQueryClient()
  const [updateDialogOpen, setUpdateDialogOpen] = useState(false)

  const toggleActiveMutation = useMutation({
    mutationFn: () => (account.active ? disableAccount : enableAccount)(account.id!),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.AdminAccounts] })
      } else {
        toast({ description: (account.active ? 'Letiltás' : 'Engedélyezés') + ' sikertelen' })
      }
    }
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteAccount(account.id!),
    onSuccess: (res) => {
      if (res.result === 'Ok') {
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.AdminAccounts] })
        toast({ description: 'A felhasználó törlése sikeres!' })
      } else {
        toast({ description: res.error || 'A felhasználó törlése sikertelen!' })
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
          <DropdownMenuItem onClick={() => toggleActiveMutation.mutate()}>{account.active ? 'Letiltás' : 'Engedélyezés'}</DropdownMenuItem>
          <DropdownMenuItem onClick={() => setUpdateDialogOpen(true)}>Szerkesztés</DropdownMenuItem>
          <DropdownMenuItem onClick={() => deleteMutation.mutate()}>Törlés</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EditAccountDialog account={account} open={updateDialogOpen} setOpen={setUpdateDialogOpen} />
    </>
  )
}
