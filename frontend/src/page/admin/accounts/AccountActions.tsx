import { Account } from '@/lib/api/model.ts'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Ellipsis } from 'lucide-react'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog.tsx'
import { AccountForm } from '@/page/admin/accounts/AccountForm.tsx'
import { deleteAccount, disableAccount, enableAccount, updateAccount } from '@/lib/api/admin.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useToast } from '@/components/ui/use-toast.ts'

const EditAccountDialog = ({ open, setOpen, account }: { open: boolean; setOpen: (open: boolean) => void; account: Account }) => {
  const { token } = useAppContext()
  const [error, setError] = useState<string>()
  const [loading, setLoading] = useState(false)
  const queryClient = useQueryClient()

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Felhasználó szerkesztése</DialogTitle>
          <AccountForm
            error={error}
            loading={loading}
            defaultAccount={account}
            onAccountSubmitted={(submittedAccount) => {
              if (account.id === undefined) return
              setError(undefined)
              setLoading(true)
              updateAccount(token, account.id, submittedAccount)
                .then((data) => {
                  if (data.result === 'Ok') {
                    setOpen(false)
                    queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
                    return
                  }

                  setError(data.error || 'A felhasználó szerkesztése sikertelen!')
                })
                .then(() => setLoading(false))
            }}
          />
        </DialogHeader>
      </DialogContent>
    </Dialog>
  )
}

export const AccountActions = ({ account }: { account: Account }) => {
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
              const action = account.active ? disableAccount : enableAccount
              action(token, account.id!).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
                } else {
                  toast({ description: (account.active ? 'Letiltás' : 'Engedélyezés') + ' sikertelen' })
                }
              })
            }}
          >
            {account.active ? 'Letiltás' : 'Engedélyezés'}
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() =>
              deleteAccount(token, account.id!).then((res) => {
                if (res.result === 'Ok') {
                  queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Accounts] })
                  toast({ description: 'A felhasználó törlése sikeres!' })
                } else {
                  toast({ description: res.error || 'A felhasználó törlése sikertelen!' })
                }
              })
            }
          >
            Törlés
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <EditAccountDialog account={account} open={updateDialogOpen} setOpen={setUpdateDialogOpen} />
    </>
  )
}
