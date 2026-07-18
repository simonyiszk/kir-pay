import { useAppContext } from '@/hooks/useAppContext.ts'
import { UseQueryResult } from '@tanstack/react-query'
import { OverviewCard } from '@/components/OverviewCard.tsx'
import { AnalyticsDto, ValidatedApiCall } from '@/lib/api/model.ts'
import { ArrowLeftRight, Banknote, User } from 'lucide-react'

export const OverviewSection = ({ analytics }: { analytics: UseQueryResult<ValidatedApiCall<AnalyticsDto>> }) => {
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
