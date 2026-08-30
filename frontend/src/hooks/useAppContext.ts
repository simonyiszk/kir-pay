import { createContext, useContext } from 'react'
import { AppResponse } from '@/lib/api/model.ts'

export const AppContext = createContext<AppResponse>({} as AppResponse)

export const useAppContext = (): AppResponse => useContext(AppContext)
