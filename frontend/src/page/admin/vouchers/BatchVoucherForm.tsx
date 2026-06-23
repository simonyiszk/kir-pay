import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { Input } from '@/components/ui/input.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Button } from '@/components/ui/button.tsx'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import type { Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ItemSelect } from '@/page/admin/common/ItemSelect.tsx'
import { X } from 'lucide-react'
import { BatchVoucherDto } from '@/lib/api/model.ts'

const voucherSchema = z.object({
  accounts: z
    .string()
    .min(1)
    .refine((text) => text.match(/[, 0-9]/), 'A bemenet azonosítók listája kell, hogy legyen'),
  itemId: z.coerce.number().int(),
  count: z.coerce.number().int().finite().gte(0)
})

type BatchVoucherOutput = z.infer<typeof voucherSchema>

export const BatchVoucherForm = ({
  loading,
  error,
  onVoucherSubmitted
}: {
  error?: string
  loading: boolean
  onVoucherSubmitted: (voucher: BatchVoucherDto) => void
}) => {
  const form = useForm<BatchVoucherOutput>({
    resolver: zodResolver(voucherSchema) as Resolver<BatchVoucherOutput>,
    defaultValues: { accounts: '', itemId: 0, count: 0 }
  })

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((data) =>
          onVoucherSubmitted({
            ...data,
            accounts: data.accounts
              .split(/[, ]/)
              .filter(Boolean)
              .map(Number)
              .filter((num) => Number.isFinite(num))
          })
        )}
      >
        <div className="grid gap-4 py-4">
          <FormField
            control={form.control}
            name="accounts"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Felhasználók - kötelező</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('accounts', '')} />
                    <Input placeholder="1,2,3,4,5" {...field} />
                  </div>
                </FormControl>
                <FormDescription>Felhasználók azonosítója vesszővel elválasztva</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="itemId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Termék</FormLabel>
                <FormControl>
                  <ItemSelect onItemSelected={field.onChange} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="count"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Hány darab - Kötelező</FormLabel>
                <FormControl>
                  <Input placeholder="1234" type="number" {...field} />
                </FormControl>
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
