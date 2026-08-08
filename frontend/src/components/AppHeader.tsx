import { Logo } from '@/components/Logo.tsx'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Menu } from 'lucide-react'
import { useEnableRotatedForCustomer } from '@/hooks/useEnableRotatedForCustomer.ts'
import { useTheme } from '@/hooks/useTheme.ts'
import { logout } from '@/lib/api/common.api.ts'

export const AppHeader = () => {
  const { rotateEnabled, setRotateEnabled } = useEnableRotatedForCustomer()
  const { theme, setTheme } = useTheme()

  const handleLogout = async () => {
    await logout()
    window.location.reload()
  }

  return (
    <header className="flex flex-col w-full items-center mb-8 gap-4 sm:gap-8 m-auto relative">
      <Logo />
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button className="absolute right-0 top-0" variant="ghost" size="icon">
            <Menu />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}>
            {theme === 'light' ? 'Sötét téma' : 'Világos téma'}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setRotateEnabled(!rotateEnabled)}>
            Tükrözés {rotateEnabled ? 'kikapcsolása' : 'bekapcsolása'}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={handleLogout}>Kijelentkezés</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  )
}
