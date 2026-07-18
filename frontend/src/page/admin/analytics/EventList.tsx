import { UseQueryResult } from '@tanstack/react-query'
import { BackAndForwardPagination } from '@/components/ui/pagination.tsx'
import { Card } from '@/components/ui/card.tsx'
import { formatTimestamp } from '@/lib/utils.ts'
import { Badge } from '@/components/ui/badge.tsx'
import { User } from 'lucide-react'
import { Event, ValidatedApiCall } from '@/lib/api/model.ts'

export const EventList = ({
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
