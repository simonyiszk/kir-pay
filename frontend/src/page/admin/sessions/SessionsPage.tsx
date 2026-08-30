import { SessionInfo, ValidatedApiCall } from '@/lib/api/model.ts'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table.tsx'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { deleteSession, findAllSessions } from '@/lib/api/admin.api.ts'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'
import { Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button.tsx'
import { useToast } from '@/components/ui/use-toast.ts'
import { formatTimestamp, getHashedColor } from '@/lib/utils.ts'

const SessionActions = ({ session }: { session: SessionInfo }) => {
  const { toast } = useToast()
  const queryClient = useQueryClient()

  const { mutate: revoke } = useMutation({
    mutationFn: () => deleteSession(session.sessionId),
    onSuccess: (result) => {
      if (result.result === 'Ok') {
        queryClient.invalidateQueries({ queryKey: [AppQueryKeys.Sessions] })
        toast({ description: 'Session visszavonva' })
      } else {
        toast({ description: result.error || 'Sikertelen visszavonás', variant: 'destructive' })
      }
    }
  })

  return (
    <Button variant="destructive" size="sm" onClick={() => revoke()}>
      <Trash2 className="h-4 w-4" />
      Törlés
    </Button>
  )
}

const SessionsTable = ({ sessions }: { sessions?: ValidatedApiCall<SessionInfo[]> }) => {
  if (!sessions) return null
  if (sessions.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (!sessions.data.length) return <h1 className="font-bold text-lg pb-4 text-center">Nincs aktív session</h1>

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Felhasználó</TableHead>
          <TableHead>IP cím</TableHead>
          <TableHead>Eszköz</TableHead>
          <TableHead>Létrehozva</TableHead>
          <TableHead>Utolsó aktivitás</TableHead>
          <TableHead>Lejárat</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {sessions.data.map((session) => {
          const color = session.principalName ? getHashedColor(session.principalName) : undefined
          return (
            <TableRow key={session.sessionId} className="relative overflow-clip">
              <TableCell>
                {color && <span className="inline-block w-3 h-3 rounded-full mr-2 align-middle" style={{ backgroundColor: color }} />}
                {session.principalName || '-'}
              </TableCell>
              <TableCell>{session.ipAddress || '-'}</TableCell>
              <TableCell className="max-w-[200px] line-clamp-3" title={session.userAgent || undefined}>
                {session.userAgent || '-'}
              </TableCell>
              <TableCell>{formatTimestamp(session.creationTime)}</TableCell>
              <TableCell>{formatTimestamp(session.lastAccessTime)}</TableCell>
              <TableCell>{session.maxInactiveInterval > 0 ? formatTimestamp(session.expiryTime) : 'Soha'}</TableCell>
              <TableCell>
                <SessionActions session={session} />
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}

export const SessionsPage = () => {
  const sessions = useQuery({
    queryKey: [AppQueryKeys.Sessions],
    queryFn: () => findAllSessions(),
    refetchInterval: DataRefetchInterval,
    staleTime: DataRefetchInterval
  })

  return (
    <div className="flex-1 h-full relative">
      <div className="flex items-baseline justify-center py-6 gap-4">
        <h1 className="font-bold text-2xl pb-4 text-center">Sessionok</h1>
      </div>
      {sessions.isLoading && <LoadingIndicator />}
      <SessionsTable sessions={sessions.data} />
    </div>
  )
}
