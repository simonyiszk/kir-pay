import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { ReactNode } from 'react'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { findItemById } from '@/lib/api/admin.api.ts'
import { Item } from '@/lib/api/model.ts'

export const ItemView = ({
  itemId,
  ItemView,
  loadingPlaceholder,
  errorPlaceholder
}: {
  itemId?: number
  ItemView: ({ item }: { item: Item }) => ReactNode
  loadingPlaceholder?: ReactNode
  errorPlaceholder?: ReactNode
}) => {
  const { token } = useAppContext()

  const canExecuteQuery = !(itemId === undefined || itemId === null)
  const itemQuery = useQuery({
    enabled: canExecuteQuery,
    queryFn: async () => {
      const item = await findItemById(token, itemId!)
      if (item.result !== 'Ok') throw Error(item.error)
      return item.data
    },
    queryKey: [AppQueryKeys.Items, token, itemId],
    placeholderData: keepPreviousData,
    staleTime: 30000
  })

  if (!canExecuteQuery) return errorPlaceholder || null
  if (itemQuery.isLoading) return loadingPlaceholder || null
  if (itemQuery.error || !itemQuery.data) return errorPlaceholder || null

  return <ItemView item={itemQuery.data} />
}

export const ItemNameView = ({ itemId }: { itemId?: number }) => (
  <ItemView
    itemId={itemId}
    loadingPlaceholder={<span>...</span>}
    errorPlaceholder={<span>???</span>}
    ItemView={({ item }) => <span>{item.name}</span>}
  />
)
