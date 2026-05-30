import { useAppContext } from '@/hooks/useAppContext.ts'
import { useQueries, UseQueryResult } from '@tanstack/react-query'
import { exportEvents, findAllEvents, getAnalytics } from '@/lib/api/admin.api.ts'
import { OverviewCard } from '@/components/OverviewCard.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { AnalyticsDto, Event, ValidatedApiCall } from '@/lib/api/model.ts'
import { ArrowLeftRight, Banknote, User } from 'lucide-react'
import { Badge } from '@/components/ui/badge.tsx'
import { exportToCsv, formatTimestamp } from '@/lib/utils.ts'
import { Card } from '@/components/ui/card.tsx'
import { BackAndForwardPagination } from '@/components/ui/pagination.tsx'
import { useState } from 'react'
import { Button } from '@/components/ui/button.tsx'
import { useToast } from '@/components/ui/use-toast.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { DataRefetchInterval } from '@/page/admin/common/constants.ts'

const OverviewSection = ({ analytics }: { analytics: UseQueryResult<ValidatedApiCall<AnalyticsDto>> }) => {
  const { currencySymbol } = useAppContext().config
  if (!analytics.data) return null
  if (analytics.data.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  const { accountCount, allActiveBalance, allUploads, income, transactionCount, transactionVolume } = analytics.data.data

  return (
    <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
      <OverviewCard title="Felhasználók" body={<span className="text-blue-500 font-bold">{accountCount} darab</span>} icon={<User />} />
      <OverviewCard title="Tranzakciók száma" body={<span className="font-bold">{transactionCount}</span>} icon={<ArrowLeftRight />} />
      <OverviewCard
        title="Tranzakciós volumen"
        body={
          <span className="font-bold">
            {transactionVolume} {currencySymbol}
          </span>
        }
        icon={<ArrowLeftRight />}
      />
      <OverviewCard
        title="Össz aktív egyenleg"
        body={
          <span className="text-primary font-bold">
            {allActiveBalance} {currencySymbol}
          </span>
        }
        icon={<Banknote />}
      />
      <OverviewCard
        title="Befizetések összege"
        body={
          <span className="text-primary font-bold">
            {allUploads} {currencySymbol}
          </span>
        }
        icon={<Banknote />}
      />
      <OverviewCard
        title="Teljes bevétel"
        body={
          <span className="text-primary font-bold">
            {income} {currencySymbol}
          </span>
        }
        icon={<Banknote />}
      />
    </div>
  )
}

const EventList = ({
  events,
  page,
  setPage
}: {
  page: number
  setPage: (page: number) => void
  events: UseQueryResult<ValidatedApiCall<Event[]>>
}) => {
  if (!events.data) return null
  if (events.data.result !== 'Ok') return <span className="text-destructive text-center">Sikertelen betöltés</span>

  if (page === 0 && !events.data.data.length) return <h1 className="font-bold text-lg pb-4 text-center">Még nincs egyetlen esemény sem!</h1>

  return (
    <div className="flex flex-col gap-2">
      {<BackAndForwardPagination page={page} setPage={setPage} reachedEnd={events.data.data.length <= 0} />}
      {events.data.data.length <= 0 && <span className="font-bold text-lg pt-6 pb-4 text-center">Nincs több esemény</span>}
      {events.data.data.map((event) => (
        <Card key={event.id} className="p-4 flex flex-row gap-4 items-center flex-wrap">
          <Badge variant="outline" style={{ borderColor: event.color }}>
            {event.event}
          </Badge>
          <span className="text-xs font-mono">{formatTimestamp(event.timestamp)}</span>
          <Badge variant="secondary">
            <User className="w-4 mr-2" /> {event.performedBy}
          </Badge>
          <span>{event.message}</span>
        </Card>
      ))}
      {events.data.data.length > 0 && <BackAndForwardPagination page={page} setPage={setPage} reachedEnd={events.data.data.length <= 0} />}
    </div>
  )
}

export const AnalyticsPage = () => {
  const [page, setPage] = useState(0)
  const { toast } = useToast()
  const { token } = useAppContext()
  const [analytics, events] = useQueries({
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
