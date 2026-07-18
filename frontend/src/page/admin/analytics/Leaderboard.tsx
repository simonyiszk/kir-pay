import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table.tsx'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { ConsumptionLeaderboardEntry, ItemLeaderboardEntry, ValidatedApiCall } from '@/lib/api/model.ts'
import { UseQueryResult } from '@tanstack/react-query'

export const ConsumptionLeaderboard = ({
  leaderboard
}: {
  leaderboard: UseQueryResult<ValidatedApiCall<ConsumptionLeaderboardEntry[]>>
}) => {
  if (leaderboard.data?.result !== 'Ok' || !leaderboard.data?.data?.length) return null

  return (
    <div className="flex flex-col items-center justify-center pt-6 pb-6 gap-4">
      <h1 className="font-bold text-2xl py-3 text-center">Top 10 fogyasztó</h1>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Név</TableHead>
            <TableHead>Mennyiség</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {leaderboard.data.data.map((entry) => (
            <TableRow key={entry.accountId} className="relative overflow-clip">
              <TableCell>
                <ColorMarker color={entry.color} />
                {entry.name}
              </TableCell>
              <TableCell>{entry.itemCount}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

export const ItemLeaderboard = ({ leaderboard }: { leaderboard: UseQueryResult<ValidatedApiCall<ItemLeaderboardEntry[]>> }) => {
  if (leaderboard.data?.result !== 'Ok' || !leaderboard.data?.data?.length) return null

  return (
    <div className="flex flex-col items-center justify-center pt-6 pb-6 gap-4">
      <h1 className="font-bold text-2xl py-3 text-center">Legnépszerűbb termékek</h1>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Termék</TableHead>
            <TableHead>Eladva</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {leaderboard.data.data.map((entry) => (
            <TableRow key={entry.itemId} className="relative overflow-clip">
              <TableCell>
                <ColorMarker color={entry.color} />
                {entry.itemName}
              </TableCell>
              <TableCell>{entry.itemCount}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
