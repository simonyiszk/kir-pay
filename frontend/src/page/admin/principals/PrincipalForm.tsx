import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { X } from 'lucide-react'
import { Input } from '@/components/ui/input.tsx'
import { Checkbox } from '@/components/ui/checkbox.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { Button } from '@/components/ui/button.tsx'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { PrincipalDto, PrincipalRole } from '@/lib/api/model.ts'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select.tsx'

const principalSchema = z.object({
  name: z.string().min(1),
  password: z.string().min(1),
  role: z.enum([PrincipalRole.Admin, PrincipalRole.Terminal]),
  canUpload: z.boolean(),
  canTransfer: z.boolean(),
  canSellItems: z.boolean(),
  canRedeemVouchers: z.boolean(),
  canAssignCards: z.boolean(),
  active: z.boolean()
})

const updatePermissionsBasedOnRole = (principal: PrincipalDto): PrincipalDto => {
  const givenPermissions =
    principal.role === PrincipalRole.Admin
      ? {
          canUpload: true,
          canTransfer: true,
          canSellItems: true,
          canRedeemVouchers: true,
          canAssignCards: true
        }
      : {}
  return { ...principal, ...givenPermissions }
}

export const PrincipalForm = ({
  loading,
  error,
  onPrincipalSubmitted,
  defaultPrincipal
}: {
  error?: string
  loading: boolean
  onPrincipalSubmitted: (principal: PrincipalDto) => void
  defaultPrincipal?: PrincipalDto
}) => {
  const form = useForm<z.infer<typeof principalSchema>>({
    resolver: zodResolver(principalSchema),
    defaultValues: {
      name: defaultPrincipal?.name || '',
      password: (defaultPrincipal?.password && '***') || '',
      role: defaultPrincipal?.role || PrincipalRole.Terminal,
      canUpload: defaultPrincipal?.canUpload ?? true,
      canTransfer: defaultPrincipal?.canTransfer ?? true,
      canSellItems: defaultPrincipal?.canSellItems ?? true,
      canRedeemVouchers: defaultPrincipal?.canRedeemVouchers ?? true,
      canAssignCards: defaultPrincipal?.canAssignCards ?? true,
      active: defaultPrincipal?.active ?? true
    }
  })

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((data) => {
          onPrincipalSubmitted(updatePermissionsBasedOnRole({ ...data }))
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
                    <Input placeholder="Példa Pultos" {...field} />
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Jelszó</FormLabel>
                <FormControl>
                  <div className="relative">
                    <X className="w-4 h-4 m-auto mr-3 absolute top-0 bottom-0 right-0" onClick={() => form.setValue('password', '')} />
                    <Input placeholder="kiscica123" {...field} />
                  </div>
                </FormControl>
                {!!defaultPrincipal?.password && <FormDescription>Ha ***, akkor marad a régi</FormDescription>}
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="role"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Jogkör</FormLabel>
                <FormControl>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Jogkör kiválasztása" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={PrincipalRole.Admin}>Admin</SelectItem>
                      <SelectItem value={PrincipalRole.Terminal}>Terminál</SelectItem>
                    </SelectContent>
                  </Select>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          {/* eslint-disable-next-line react-hooks/incompatible-library */}
          {form.watch('role') === PrincipalRole.Terminal && (
            <>
              <FormField
                control={form.control}
                name="canUpload"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-col items-start gap-2">
                      <div className="flex flex-row gap-2">
                        <FormControl>
                          <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                        <FormLabel>Tölthet kártyára</FormLabel>
                      </div>
                      <FormDescription>Tölthet-e kártyára pénzt</FormDescription>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="canTransfer"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-col items-start gap-2">
                      <div className="flex flex-row gap-2">
                        <FormControl>
                          <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                        <FormLabel>Átutalhat</FormLabel>
                      </div>
                      <FormDescription>Átruházhat-e egyenleget két kártya között</FormDescription>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="canSellItems"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-col items-start gap-2">
                      <div className="flex flex-row gap-2">
                        <FormControl>
                          <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                        <FormLabel>Levonhat egyenleget</FormLabel>
                      </div>
                      <FormDescription>Végrehajthat-e fizetést</FormDescription>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="canRedeemVouchers"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-col items-start gap-2">
                      <div className="flex flex-row gap-2">
                        <FormControl>
                          <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                        <FormLabel>Beolvashat utalványokat</FormLabel>
                      </div>
                      <FormDescription>Beválthat-e termékeket utalványokkal</FormDescription>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="canAssignCards"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex flex-col items-start gap-2">
                      <div className="flex flex-row gap-2">
                        <FormControl>
                          <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                        <FormLabel>Hozzárendelhet kártyákat</FormLabel>
                      </div>
                      <FormDescription>Összeköthet kártyákat személyekkel</FormDescription>
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </>
          )}
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
                  <FormDescription>Beléphet-e a principal</FormDescription>
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
