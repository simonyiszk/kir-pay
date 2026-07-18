import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQueries } from '@tanstack/react-query'
import { exportEvents, findAllEvents, getAnalytics, getConsumptionLeaderboard, getItemLeaderboard } from '@/lib/api/admin.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { exportToCsv } from '@/lib/utils.ts'
import { useState } from 'react'
import { Button } from '@/components/ui/button.tsx'
import { useToast } from '@/components/ui/use-toast.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'
import { EventList } from '@/page/admin/analytics/EventList.tsx'
import { OverviewSection } from '@/page/admin/analytics/OverviewSection.tsx'
import { ConsumptionLeaderboard, ItemLeaderboard } from '@/page/admin/analytics/Leaderboard.tsx'

export const AnalyticsPage = () => {
  const [page, setPage] = useState(0)
  const { toast } = useToast()
  const { token } = useAppContext()
  const [analytics, events, consumptionLeaderboard, itemLeaderboard] = useQueries({
    queries: [
      {
        queryKey: [AppQueryKeys.Analytics, token],
        queryFn: () => getAnalytics(token),
        refetchInterval: DataRefetchInterval,
        staleTime: DataRefetchInterval
      },
      {
        queryKey: [AppQueryKeys.Events, token, page],
        queryFn: () => findAllEvents(token, page, 25),
        refetchInterval: DataRefetchInterval,
        staleTime: DataRefetchInterval
      },
      {
        queryKey: [AppQueryKeys.ConsumptionLeaderboard, token],
        queryFn: () => getConsumptionLeaderboard(token, 10),
        refetchInterval: DataRefetchInterval,
        staleTime: DataRefetchInterval
      },
      {
        queryKey: [AppQueryKeys.ItemLeaderboard, token],
        queryFn: () => getItemLeaderboard(token, 10),
        refetchInterval: DataRefetchInterval,
        staleTime: DataRefetchInterval
      }
    ]
  })

  return (
    <div className="flex-1 h-full relative">
      <h1 className="font-bold text-2xl py-6 text-center">Analitika</h1>
      <OverviewSection analytics={analytics} />
      {analytics.isLoading && (
        <div className="p-4">
          <LoadingIndicator />
        </div>
      )}

      <ItemLeaderboard leaderboard={itemLeaderboard} />

      <ConsumptionLeaderboard leaderboard={consumptionLeaderboard} />

      <div className="flex items-baseline justify-center pt-12 pb-6 gap-4">
        <h2 className="font-bold text-2xl  text-center">Események</h2>
        <Button
          variant="secondary"
          onClick={() =>
            exportToCsv('events.csv', () =>
              exportEvents(token).then((data) => {
                if (data.result === 'Ok') return data.data
                throw Error()
              })
            )
              .then(() => toast({ description: 'Események exportálva' }))
              .catch(() => toast({ description: 'Hiba az események exportálása közben' }))
          }
        >
          Exportálás
        </Button>
      </div>
      {events.isLoading && (
        <div className="p-4">
          <LoadingIndicator />
        </div>
      )}
      <EventList page={page} setPage={setPage} events={events} />
    </div>
  )
}
