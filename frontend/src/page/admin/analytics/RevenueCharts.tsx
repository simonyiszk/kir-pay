import { Fragment, memo, useCallback, useMemo, useState } from 'react'
import { type UseQueryResult } from '@tanstack/react-query'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { type RevenueHeatmapEntry, type ValidatedApiCall } from '@/lib/api/model.ts'
import { cn, formatNumber } from '@/lib/utils.ts'

const HOUR_TICKS = [0, 6, 12, 18, 23]

// "2026-09-04" -> "09. 04."
const dateLabel = (date: string) => {
  const [, month, day] = date.split('-')
  return `${month}. ${day}.`
}

const cellLabel = (date: string, hour: number, revenue: number, currencySymbol: string) =>
  `${dateLabel(date)}, ${hour}:00 – ${formatNumber(revenue)} ${currencySymbol}`

// 6-step sequential scale, log-compressed so sparse days stay readable
const heatmapStep = (value: number, maxRevenue: number) =>
  maxRevenue === 0 ? 0 : Math.min(5, Math.floor(6 * Math.log10(1 + (9 * value) / maxRevenue)))

type HeatmapCellProps = {
  date: string
  hour: number
  value: number
  maxRevenue: number
  currencySymbol: string
  isHovered: boolean
  onHover: (date: string, hour: number) => void
  onLeave: () => void
}

const HeatmapCell = memo(({ date, hour, value, maxRevenue, currencySymbol, isHovered, onHover, onLeave }: HeatmapCellProps) => (
  <div
    aria-label={cellLabel(date, hour, value, currencySymbol)}
    className={cn(
      'h-4 rounded-[3px] md:h-7 md:rounded-[4px]',
      value === 0 ? 'border border-border/60' : 'hover:ring-1 hover:ring-ring',
      isHovered && 'ring-1 ring-ring'
    )}
    style={value === 0 ? undefined : { backgroundColor: `var(--revenue-${heatmapStep(value, maxRevenue) + 1})` }}
    onMouseEnter={() => onHover(date, hour)}
    onMouseLeave={onLeave}
  />
))

type RevenueChartsProps = {
  query: UseQueryResult<ValidatedApiCall<RevenueHeatmapEntry[]>>
}

export const RevenueCharts = ({ query }: RevenueChartsProps) => {
  const { currencySymbol } = useAppContext().config
  const [hoveredCell, setHoveredCell] = useState<{ date: string; hour: number } | null>(null)

  const rows = useMemo(() => (query.data?.result === 'Ok' ? query.data.data : []), [query.data])

  const { dates, grid, maxRevenue } = useMemo(() => {
    const map = new Map<string, number>()
    rows.forEach((row) => map.set(`${row.date}:${row.hour}`, row.revenue))
    return {
      dates: [...new Set(rows.map((row) => row.date))], // server orders date desc
      grid: map,
      maxRevenue: Math.max(0, ...map.values())
    }
  }, [rows])

  const handleHover = useCallback((date: string, hour: number) => setHoveredCell({ date, hour }), [])
  const handleLeave = useCallback(() => setHoveredCell(null), [])

  const hoveredCaption = useMemo(() => {
    if (!hoveredCell) return ''
    const key = `${hoveredCell.date}:${hoveredCell.hour}`
    if (!grid.has(key)) return ''
    return cellLabel(hoveredCell.date, hoveredCell.hour, grid.get(key) ?? 0, currencySymbol)
  }, [hoveredCell, grid, currencySymbol])

  return (
    <div className="flex flex-col gap-6 pt-10">
      {query.isLoading && (
        <div className="p-4">
          <LoadingIndicator />
        </div>
      )}
      {query.isError && !query.data && <span className="text-destructive text-center">Sikertelen betöltés</span>}
      {query.isError && query.data && (
        <span className="text-center text-xs text-muted-foreground">Nem sikerült frissíteni az adatokat</span>
      )}

      {query.data?.result === 'Ok' && (
        <Card>
          <CardHeader>
            <CardTitle>Bevétel az elmúlt 7 napban</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <div className="grid gap-px [grid-template-columns:2.5rem_repeat(24,minmax(0,1fr))] md:min-w-[560px] md:gap-0.5 md:[grid-template-columns:3rem_repeat(24,minmax(0,1fr))]">
                <div />
                {Array.from({ length: 24 }, (_, hour) => (
                  <div key={hour} className="overflow-hidden text-center text-[10px] leading-4 text-muted-foreground md:leading-6">
                    {HOUR_TICKS.includes(hour) ? hour : ''}
                  </div>
                ))}
                {dates.map((date) => (
                  <Fragment key={date}>
                    <div className="overflow-hidden text-[10px] leading-4 text-muted-foreground md:text-xs md:leading-7">
                      {dateLabel(date)}
                    </div>
                    {Array.from({ length: 24 }, (_, hour) => (
                      <HeatmapCell
                        key={hour}
                        date={date}
                        hour={hour}
                        value={grid.get(`${date}:${hour}`) ?? 0}
                        maxRevenue={maxRevenue}
                        currencySymbol={currencySymbol}
                        isHovered={hoveredCell?.date === date && hoveredCell?.hour === hour}
                        onHover={handleHover}
                        onLeave={handleLeave}
                      />
                    ))}
                  </Fragment>
                ))}
              </div>
            </div>
            <div className="flex items-center gap-2 pt-3">
              <span className="text-xs text-muted-foreground">Kevés</span>
              <div
                className="h-2 flex-1 rounded-full"
                style={{
                  background:
                    'linear-gradient(90deg, var(--revenue-1), var(--revenue-2), var(--revenue-3), var(--revenue-4), var(--revenue-5), var(--revenue-6))'
                }}
              />
              <span className="text-xs text-muted-foreground">Sok</span>
            </div>
            <div className="h-5 pt-1 text-center text-xs text-muted-foreground">{hoveredCaption}</div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
