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
import { Account } from '@/lib/api/model.ts'
import { useAppContext } from '@/hooks/useAppContext.ts'

const accountSchema = z.object({
  name: z.string().min(1),
  email: z.string().optional(),
  phone: z.string().optional(),
  card: z.string().optional(),
  balance: z.coerce.number().int().finite().gte(0),
  active: z.coerce.boolean()
})

type AccountOutput = z.infer<typeof accountSchema>

export const AccountForm = ({
  loading,
  error,
  onAccountSubmitted,
  defaultAccount
}: {
  error?: string
  loading: boolean
  onAccountSubmitted: (account: Account) => void
  defaultAccount?: Account
}) => {
  const { currencySymbol } = useAppContext().config
  const form = useForm<AccountOutput>({
    resolver: zodResolver(accountSchema) as Resolver<AccountOutput>,
    defaultValues: {
      name: defaultAccount?.name || '',
      email: defaultAccount?.email || '',
      phone: defaultAccount?.phone || '',
      card: defaultAccount?.card || '',
      balance: defaultAccount?.balance ?? 0,
      active: defaultAccount?.active ?? true
    }
  })

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((data) => {
          onAccountSubmitted({ ...data })
        })}
      >
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
                    <Input placeholder="Példa Petra" {...field} />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>E-mail cím</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('email', '')} />
                    <Input placeholder="email@kir-dev.hu" {...field} />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="phone"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Telefonszám</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('phone', '')} />
                    <Input placeholder="69-420-13-37" {...field} />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="card"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Kártya azonosító</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('card', '')} />
                    <Input placeholder="Fogadunk, hogy ezt nem tudod?" {...field} />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="balance"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Egyenleg ({currencySymbol}) - Kötelező</FormLabel>
                <FormControl>
                  <Input placeholder="1234" type="number" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="active"
            render={({ field }) => (
              <FormItem>
                <div className="flex flex-col items-start gap-2">
                  <div className="flex flex-row gap-2">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                    <FormLabel>Aktív</FormLabel>
                  </div>
                  <FormDescription>Végrehajthat-e tranzakciókat</FormDescription>
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
