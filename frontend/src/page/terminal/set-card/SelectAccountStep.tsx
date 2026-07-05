import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useAppContext } from '@/hooks/useAppContext.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'
import { fuzzySearch } from '@/lib/utils.ts'
import { useEffect, useState } from 'react'
import { Input } from '@/components/ui/input.tsx'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form.tsx'
import { z } from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button } from '@/components/ui/button.tsx'
import { Toggle } from '@/components/ui/toggle.tsx'
import { CircleAlert } from 'lucide-react'
import { ColorMarker } from '@/components/ColorMarker.tsx'
import { Account } from '@/lib/api/model.ts'
import { findAllAccounts } from '@/lib/api/terminal.api.ts'
import { AppQueryKeys } from '@/lib/api/common.api.ts'

export const SelectAccountStep = ({ setAccount }: { setAccount: (account: Account) => void }) => {
  const { token } = useAppContext()
  const accountsQuery = useQuery({
    queryKey: [AppQueryKeys.Accounts, token],
    queryFn: () => findAllAccounts(token),
    placeholderData: keepPreviousData
  })

  return (
    <>
      <h1 className="font-bold text-2xl pb-2 text-center">Válassz felhasználót!</h1>
      {!accountsQuery.data && (
        <div className="mt-4">
          <LoadingIndicator />
        </div>
      )}

      {!!accountsQuery.data && accountsQuery.data.result === 'Ok' && (
        <SelectAccount accounts={accountsQuery.data.data} setAccount={setAccount} />
      )}

      {!!accountsQuery.data && accountsQuery.data.result !== 'Ok' && <span>{accountsQuery.data.error}</span>}
    </>
  )
}

const accountNameSchema = z.object({ name: z.string().optional() })

const SelectAccount = ({ accounts, setAccount }: { accounts: Account[]; setAccount: (accountId: Account) => void }) => {
  const form = useForm<z.infer<typeof accountNameSchema>>({
    resolver: zodResolver(accountNameSchema),
    defaultValues: { name: '' }
  })

  const [needle, setNeedle] = useState<string>()
  const [suggestions, setSuggestions] = useState(accounts)
  const [selectedAccount, setSelectedAccount] = useState<Account>()

  useEffect(() => {
    const haystack = accounts
    if (!needle) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSuggestions(haystack)
      return
    }

    const newSuggestions = fuzzySearch({ needle, haystack, getText: (account) => account.name + account.email })
    setSuggestions(newSuggestions)
  }, [accounts, needle])

  return (
    <>
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
                  <Input placeholder="Példa Péter" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </form>
      </Form>
      <Button
        className="w-full mb-2"
        disabled={selectedAccount === undefined}
        onClick={(e) => {
          e.preventDefault()
          if (selectedAccount) {
            setAccount(selectedAccount)
          }
        }}
      >
        Kész
      </Button>

      {!!selectedAccount?.card && <p className="text-center pb-2">A felhasználóhoz már hozzá van rendelve egy kártya!</p>}
      <AccountCardGrid suggestions={suggestions} selectedAccount={selectedAccount} setSelectedAccount={setSelectedAccount} />
    </>
  )
}

const AccountCardGrid = ({
  suggestions,
  selectedAccount,
  setSelectedAccount
}: {
  suggestions: Account[]
  selectedAccount?: Account
  setSelectedAccount: (account: Account) => void
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
            pressed={selectedAccount?.id === suggestion.id}
            onPressedChange={() => setSelectedAccount(suggestion)}
          >
            <ColorMarker color={suggestion.color} />
            {!!suggestion?.card && <CircleAlert className="absolute w-5 h-5 top-2 right-2" />}
            <span className="text-wrap">{suggestion.name}</span>
            <small className="text-wrap">{suggestion.email}</small>
          </Toggle>
        ))}
      </div>
    )}

    {!suggestions.length && <p className="text-center">Nincs találat</p>}
  </>
)
