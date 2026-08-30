import { FC, PropsWithChildren, useEffect, useState } from 'react'
import { Theme, ThemeContext } from '@/hooks/useTheme.ts'
import { setPersistentState, safeGetLocalStorage } from '@/lib/utils.ts'

const ThemeKey = 'theme'

const getTheme = (): Theme => (safeGetLocalStorage(ThemeKey) === 'dark' ? 'dark' : 'light')

export const ThemeProvider: FC<PropsWithChildren> = ({ children }) => {
  const [theme, setTheme] = useState<Theme>(getTheme())

  useEffect(() => {
    const oldTheme = theme === 'dark' ? 'light' : 'dark'
    document.documentElement.classList.remove(oldTheme)
    document.documentElement.classList.add(theme)
  }, [theme])

  return <ThemeContext.Provider value={{ theme, setTheme: setPersistentState(ThemeKey, setTheme) }}>{children}</ThemeContext.Provider>
}
