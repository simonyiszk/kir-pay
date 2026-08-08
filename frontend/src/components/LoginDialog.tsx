import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { X } from 'lucide-react'
import { Input } from '@/components/ui/input.tsx'
import { Button } from '@/components/ui/button.tsx'
import { login } from '@/lib/api/common.api.ts'
import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'

const loginSchema = z.object({
  accountname: z.string().min(1),
  password: z.string().min(1)
})

export const LoginDialog = ({ onLogin }: { onLogin: () => void }) => {
  const [message, setMessage] = useState<string>()

  const form = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
    defaultValues: { accountname: '', password: '' }
  })

  const { mutate, isPending } = useMutation({
    mutationFn: (data: { username: string; password: string }) => login(data.username, data.password),
    onSuccess: (result) => {
      if (result.result == 'Ok') {
        onLogin()
        return
      }
      if (result.result === 'Unauthorized') {
        setMessage('Hibás felhasználónév vagy jelszó')
      } else if (result.result == 'Forbidden') {
        setMessage('Nincs jogod használni az alkalmazást!')
      } else {
        setMessage(result.error || 'A belépés sikertelen!')
      }
    },
    onError: () => setMessage('A belépés sikertelen!')
  })

  return (
    <Card className="max-w-[20rem] w-full">
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(async (data) => {
            setMessage(undefined)
            mutate({ username: data.accountname, password: data.password })
          })}
        >
          <CardHeader>
            <CardTitle>Belépés</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4">
            <FormField
              control={form.control}
              name="accountname"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Felhasználónév</FormLabel>
                  <div className="relative">
                    <FormControl>
                      <Input autoComplete="accountname" placeholder="admin" {...field} />
                    </FormControl>
                    <button
                      type="button"
                      className="absolute top-0 bottom-0 right-0 mr-3 flex items-center"
                      aria-label="Felhasználónév törlése"
                      onClick={() => form.setValue('accountname', '')}
                    >
                      <X className="w-4 h-4" aria-hidden="true" />
                    </button>
                  </div>
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
                  <div className="relative">
                    <FormControl>
                      <Input autoComplete="current-password" type="password" placeholder="admin" {...field} />
                    </FormControl>
                    <button
                      type="button"
                      className="absolute top-0 bottom-0 right-0 mr-3 flex items-center"
                      aria-label="Jelszó törlése"
                      onClick={() => form.setValue('password', '')}
                    >
                      <X className="w-4 h-4" aria-hidden="true" />
                    </button>
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
          <CardFooter className="flex flex-col">
            {isPending && (
              <div className="pb-2">
                <LoadingIndicator />
              </div>
            )}
            {!!message && (
              <div className="pb-2">
                <span className="text-destructive text-center font-bold">{message}</span>
              </div>
            )}
            <Button className="w-full" type="submit" disabled={isPending}>
              Belépés
            </Button>
          </CardFooter>
        </form>
      </Form>
    </Card>
  )
}
