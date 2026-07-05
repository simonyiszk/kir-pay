import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { Input } from '@/components/ui/input.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Button } from '@/components/ui/button.tsx'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import type { Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Voucher } from '@/lib/api/model.ts'
import { AccountSelect } from '@/page/admin/common/AccountSelect.tsx'
import { ItemSelect } from '@/page/admin/common/ItemSelect.tsx'

const voucherSchema = z.object({
  accountId: z.coerce.number().int(),
  itemId: z.coerce.number().int(),
  count: z.coerce.number().int().finite().gte(0)
})

type VoucherOutput = z.infer<typeof voucherSchema>

export const SingleVoucherForm = ({
  loading,
  error,
  onVoucherSubmitted
}: {
  error?: string
  loading: boolean
  onVoucherSubmitted: (voucher: Voucher) => void
}) => {
  const form = useForm<VoucherOutput>({
    resolver: zodResolver(voucherSchema) as Resolver<VoucherOutput>,
    defaultValues: { accountId: 0, itemId: 0, count: 0 }
  })

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((data) => onVoucherSubmitted({ ...data }))}>
        <div className="grid gap-4 py-4">
          <FormField
            control={form.control}
            name="accountId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Felhasználó</FormLabel>
                <FormControl>
                  <AccountSelect onAccountSelected={field.onChange} />
                </FormControl>
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
