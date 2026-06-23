import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { X } from 'lucide-react'
import { Input } from '@/components/ui/input.tsx'
import { Checkbox } from '@/components/ui/checkbox.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Button } from '@/components/ui/button.tsx'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import type { Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Item } from '@/lib/api/model.ts'
import { useAppContext } from '@/hooks/useAppContext.ts'

const itemSchema = z.object({
  name: z.string().min(1),
  alias: z.string().optional(),
  cost: z.coerce.number().finite().int().gte(0),
  stock: z.coerce.number().finite().int().gte(0),
  enabled: z.boolean(),
  showOnLeaderboard: z.boolean()
})

type ItemOutput = z.infer<typeof itemSchema>

export const ItemForm = ({
  loading,
  error,
  onItemSubmitted,
  defaultItem
}: {
  error?: string
  loading: boolean
  onItemSubmitted: (item: Item) => void
  defaultItem?: Item
}) => {
  const { currencySymbol } = useAppContext().config
  const form = useForm<ItemOutput>({
    resolver: zodResolver(itemSchema) as Resolver<ItemOutput>,
    defaultValues: {
      name: defaultItem?.name || '',
      alias: defaultItem?.alias || '',
      cost: defaultItem?.cost ?? 0,
      stock: defaultItem?.stock ?? 100000,
      enabled: defaultItem?.enabled ?? true,
      showOnLeaderboard: defaultItem?.showOnLeaderboard ?? false
    }
  })

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((data) => onItemSubmitted({ ...data }))}>
        <div className="grid gap-4 py-4">
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Név - Kötelező</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('name', '')} />
                    <Input placeholder="Somersby Apple" {...field} />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="alias"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Aliasok</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('alias', '')} />
                    <Input placeholder="alma" {...field} />
                  </div>
                </FormControl>
                <FormDescription>Kulcsszavak melyek segítenek a keresésben</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="cost"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Ár ({currencySymbol}) - Kötelező</FormLabel>
                <FormControl>
                  <Input placeholder="1234" type="number" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="stock"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Raktáron lévő mennyiség - Kötelező</FormLabel>
                <FormControl>
                  <Input placeholder="1234" type="number" {...field} />
                </FormControl>
                <FormMessage />
                <FormDescription>Ha nem releváns, adj meg valami nagyon nagy számot</FormDescription>
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="enabled"
            render={({ field }) => (
              <FormItem>
                <div className="flex flex-col items-start gap-2">
                  <div className="flex flex-row gap-2">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                    <FormLabel>Aktív</FormLabel>
                  </div>
                  <FormDescription>Eladható-e a termék</FormDescription>
                </div>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="showOnLeaderboard"
            render={({ field }) => (
              <FormItem>
                <div className="flex flex-col items-start gap-2">
                  <div className="flex flex-row gap-2">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                    <FormLabel>Toplistán látszik</FormLabel>
                  </div>
                  <FormDescription>Számít-e a vásárlás a toplistába</FormDescription>
                </div>
                <FormMessage />
              </FormItem>
            )}
          />
          {!!error && <span className="text-destructive text-center">{error}</span>}
          {loading && <LoadingIndicator />}
        </div>
        <Button disabled={loading} className="w-full mt-4" type="submit">
          Kész
        </Button>
      </form>
    </Form>
  )
}
