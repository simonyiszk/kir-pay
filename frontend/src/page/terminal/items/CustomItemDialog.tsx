import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from '@/components/ui/dialog.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Plus, X } from 'lucide-react'
import { CustomItem } from '@/page/terminal/items/cart.ts'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import type { Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { useState } from 'react'
import { useAppContext } from '@/hooks/useAppContext.ts'

const customItemSchema = z.object({
  name: z.string().min(1),
  price: z.coerce.number().int().finite().gt(0)
})

type CustomItemOutput = z.infer<typeof customItemSchema>

const NameKey = 'CustomItemName'

export const CustomItemDialog = ({ onSave }: { onSave: (item: CustomItem) => void }) => {
  const [open, setOpen] = useState(false)

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button className="h-full w-full" variant="secondary">
          <Plus className="mr-1" /> Egyedi tétel
        </Button>
      </DialogTrigger>

      <DialogContent className="sm:max-w-[425px] top-[35%]">
        <CustomItemForm
          onSave={(item) => {
            onSave(item)
            setOpen(false)
          }}
        />
      </DialogContent>
    </Dialog>
  )
}

const CustomItemForm = ({ onSave }: { onSave: (item: CustomItem) => void }) => {
  const { currencySymbol } = useAppContext().config
  const form = useForm<CustomItemOutput>({
    resolver: zodResolver(customItemSchema) as Resolver<CustomItemOutput>,
    defaultValues: { name: localStorage.getItem(NameKey) || '', price: 0 }
  })

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((data) => {
          onSave(data)
          localStorage.setItem(NameKey, data.name)
        })}
      >
        <DialogHeader>
          <DialogTitle>Egyedi tétel felvétele</DialogTitle>
          <DialogDescription>A darabszámot a kosárban változtathatod!</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Miez</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('name', '')} />
                    <Input placeholder="Unicum-sör" {...field} />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="price"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Mennyiség ({currencySymbol})</FormLabel>
                <FormControl>
                  <Input placeholder="1234" type="number" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>
        <DialogFooter>
          <Button className="w-full" type="submit">
            Kész
          </Button>
        </DialogFooter>
      </form>
    </Form>
  )
}
