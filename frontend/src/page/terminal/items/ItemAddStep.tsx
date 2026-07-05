import { ItemCard } from '@/page/terminal/items/ItemCard.tsx'
import { Cart, CustomCartEntry, CustomItem, getCustomItemQuantity, getItemQuantity } from '@/page/terminal/items/cart.ts'
import { CustomItemDialog } from '@/page/terminal/items/CustomItemDialog.tsx'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo, useState } from 'react'
import { fuzzySearch } from '@/lib/utils.ts'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Item } from '@/lib/api/model.ts'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { AppQueryKeys } from '@/lib/api/common.api.ts'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { findAllItems } from '@/lib/api/terminal.api.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'

const itemNameSchema = z.object({ name: z.string().optional() })

export const ItemAddStep = ({
  cart,
  onFinished,
  onItemAdded,
  onItemRemoved,
  onCustomItemAdded,
  onCustomItemRemoved
}: {
  cart: Cart
  onFinished: () => void
  onItemAdded: (item: Item) => void
  onItemRemoved: (item: Item) => void
  onCustomItemAdded: (item: CustomItem) => void
  onCustomItemRemoved: (item: CustomItem) => void
}) => {
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
  const [needle, setNeedle] = useState<string>()
  const [itemSuggestions, setItemSuggestions] = useState<Item[]>()
  const [customItemSuggestions, setCustomItemSuggestions] = useState<CustomCartEntry[]>()

  const items: Item[] = useMemo(() => (itemsQuery.data?.result === 'Ok' ? itemsQuery.data.data : []), [itemsQuery.data])
  useEffect(() => {
    if (!needle) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setItemSuggestions(items)
      setCustomItemSuggestions(cart.customEntries)
      return
    }

    const newItems = fuzzySearch({ needle, haystack: items, getText: (item) => item.name + ' ' + item.alias })
    const newCustomItems = fuzzySearch({ needle, haystack: cart.customEntries, getText: (item) => item.name })
    setItemSuggestions(newItems)
    setCustomItemSuggestions(newCustomItems)
  }, [cart, items, needle]) // the assumption here is that the cart is immutable, so if there is a difference, the cart reference must not be the same

  return (
    <>
      <h1 className="font-bold text-2xl pb-2 text-center">Rendelés összeállítása</h1>
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
                  <Input placeholder="Jägermeister" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </form>
      </Form>
      <Button
        className="w-full mb-2"
        disabled={!cart.customEntries.length && !cart.items.length}
        onClick={(e) => {
          e.preventDefault()
          onFinished()
        }}
      >
        Tovább
      </Button>
      <div className="grid grid-cols-[repeat(auto-fit,_minmax(250px,1fr))] gap-2 justify-center pb-2">
        <CustomItemDialog onSave={onCustomItemAdded} />

        {customItemSuggestions?.map((item) => (
          <ItemCard
            key={`custom-${item.name}_${item.price}`}
            name={item.name}
            price={item.price}
            color="#00000000"
            isDeletedAfterRemovingAll={true}
            inStock={Infinity} // there is no stock on custom items
            addItem={() => onCustomItemAdded(item)}
            removeItem={() => onCustomItemRemoved(item)}
            quantityInCart={getCustomItemQuantity(cart, item)}
          />
        ))}

        {itemsQuery.isLoading && <LoadingIndicator />}
        {!itemsQuery.isLoading && itemsQuery.data?.result !== 'Ok' && (
          <span className="text-destructive text-center">Betöltés sikertelen</span>
        )}
        {itemSuggestions?.map((item) => (
          <ItemCard
            key={item.id}
            name={item.name}
            price={item.cost}
            color={item.color}
            isDeletedAfterRemovingAll={false}
            inStock={item.stock}
            addItem={() => onItemAdded(item)}
            removeItem={() => onItemRemoved(item)}
            quantityInCart={getItemQuantity(cart, item)}
          />
        ))}
      </div>
    </>
  )
}
