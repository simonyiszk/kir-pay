import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo, useState } from 'react'
import { fuzzySearch } from '@/lib/utils.ts'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Toggle } from '@/components/ui/toggle.tsx'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { Item } from '@/lib/api/model.ts'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { findAllItems } from '@/lib/api/terminal.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'

const itemNameSchema = z.object({ name: z.string().optional() })

export const ItemSearchStep = ({ setItem }: { setItem: (item: Item) => void }) => {
  const { token } = useAppContext()
  const itemsQuery = useQuery({
    queryKey: [AppQueryKeys.Items, token],
    queryFn: () => findAllItems(token),
    placeholderData: keepPreviousData
  })
  const form = useForm<z.infer<typeof itemNameSchema>>({
    resolver: zodResolver(itemNameSchema),
    defaultValues: { name: '' }
  })

  const items: Item[] = useMemo(() => (itemsQuery.data?.result === 'Ok' ? itemsQuery.data.data : []), [itemsQuery.data])
  const [selectedItem, setSelectedItem] = useState<Item>()
  const [needle, setNeedle] = useState<string>()
  const [suggestions, setSuggestions] = useState(items)

  useEffect(() => {
    if (!needle) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSuggestions(items)
      return
    }

    const newSuggestions = fuzzySearch({
      needle,
      haystack: items,
      getText: (item) => item.name + ' ' + item.alias
    })
    setSuggestions(newSuggestions)
  }, [items, needle])

  return (
    <>
      <h1 className="font-bold text-2xl pb-2 text-center">Token beváltása</h1>
      <Form {...form}>
        <form
          onChange={form.handleSubmit((data) => {
            setNeedle(data.name ?? '')
          })}
        >
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem className="pb-2">
                <FormLabel>Kereső</FormLabel>
                <FormControl>
                  <Input placeholder="Sör" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </form>
      </Form>
      <Button
        className="w-full mb-2"
        disabled={selectedItem === undefined}
        onClick={(e) => {
          e.preventDefault()
          if (selectedItem) {
            setItem(selectedItem)
          }
        }}
      >
        Kész
      </Button>

      {itemsQuery.isLoading && <LoadingIndicator />}
      {!itemsQuery.isLoading && itemsQuery.data?.result !== 'Ok' && (
        <span className="text-destructive text-center">Betöltés sikertelen</span>
      )}
      <ItemCardGrid suggestions={suggestions} selectedItem={selectedItem} setSelectedItem={setSelectedItem} />
    </>
  )
}

const ItemCardGrid = ({
  suggestions,
  selectedItem,
  setSelectedItem
}: {
  suggestions: Item[]
  selectedItem?: Item
  setSelectedItem: (item: Item) => void
}) => (
  <>
    {!!suggestions.length && (
      <div className="grid grid-cols-[repeat(auto-fit,_minmax(175px,1fr))] gap-2 justify-center pb-2">
        {suggestions.map((suggestion) => (
          <Toggle
            className="px-4 py-2 flex flex-col relative overflow-clip"
            variant="outline"
            size="unset"
            key={suggestion.id}
            pressed={selectedItem?.id === suggestion.id}
            onPressedChange={() => setSelectedItem(suggestion)}
          >
            <ColorMarker color={suggestion.color} />
            <span className="text-wrap">{suggestion.name}</span>
            <small className="text-wrap">{suggestion.alias}</small>
          </Toggle>
        ))}
      </div>
    )}

    {!suggestions.length && <p className="text-center">Nincs találat</p>}
  </>
)
