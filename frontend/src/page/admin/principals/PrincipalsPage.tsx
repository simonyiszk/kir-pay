import { Principal, PrincipalRole, ValidatedApiCall } from '@/lib/api/model.ts'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table.tsx'
import { Check, X } from 'lucide-react'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQuery } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { findAllPrincipals } from '@/lib/api/admin.api.ts'
import { PrincipalManagementDropdown } from '@/page/admin/principals/PrincipalManagementDropdown.tsx'
import { PrincipalActions } from '@/page/admin/principals/PrincipalActions.tsx'

const PrincipalsTable = ({ principals }: { principals?: ValidatedApiCall<Principal[]> }) => {
  if (!principals) return null
  if (principals.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (!principals.data.length) return <h1 className="font-bold text-lg pb-4 text-center">Még nincs egyetlen principal sem!</h1>

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Név</TableHead>
          <TableHead>Jogkör</TableHead>
          <TableHead>Feltölthet</TableHead>
          <TableHead>Átruházhat</TableHead>
          <TableHead>Levonhat</TableHead>
          <TableHead>Beolvashat Utalványt</TableHead>
          <TableHead>Összekapcsolhat</TableHead>
          <TableHead>Aktív</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {principals.data.map((principal) => {
          return (
            <TableRow key={principal.id} className="relative overflow-clip">
              <TableCell>{principal.name}</TableCell>
              <TableCell>{principal.role === PrincipalRole.Admin ? 'Admin' : 'Terminál'}</TableCell>
              <TableCell>{principal.canUpload ? <Check /> : <X />}</TableCell>
              <TableCell>{principal.canTransfer ? <Check /> : <X />}</TableCell>
              <TableCell>{principal.canSellItems ? <Check /> : <X />}</TableCell>
              <TableCell>{principal.canRedeemVouchers ? <Check /> : <X />}</TableCell>
              <TableCell>{principal.canAssignCards ? <Check /> : <X />}</TableCell>
              <TableCell>{principal.active ? <Check /> : <X />}</TableCell>
              <TableCell>
                <PrincipalActions principal={principal} />
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}

export const PrincipalsPage = () => {
  const { token } = useAppContext()
  const principals = useQuery({ queryKey: [AppQueryKeys.Principals, token], queryFn: () => findAllPrincipals(token) })

  return (
    <div className="flex-1 h-full relative">
      <div className="flex items-baseline justify-center py-6 gap-4">
        <h1 className="font-bold text-2xl pb-4 text-center">Principalok</h1>
        <PrincipalManagementDropdown />
      </div>
      {principals.isLoading && <LoadingIndicator />}
      <PrincipalsTable principals={principals.data} />
    </div>
  )
}
