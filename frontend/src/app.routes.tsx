/* eslint-disable react-refresh/only-export-components */
import { createRootRoute, createRoute, createRouter, Link, Outlet } from '@tanstack/react-router'
import { FileQuestion } from 'lucide-react'
import { AppRoot } from '@/AppRoot.tsx'
import { lazy, Suspense } from 'react'
import { NoNFCBanner } from '@/components/NoNFCBanner.tsx'
import { TerminalRoot } from '@/TerminalRoot.tsx'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { buttonVariants } from '@/components/ui/button.props.ts'
import { LoadingIndicator } from '@/components/LoadingIndicator.tsx'

const TanStackRouterDevtools = import.meta.env.PROD
  ? () => null
  : lazy(() => import('@tanstack/router-devtools').then((res) => ({ default: res.TanStackRouterDevtools })))

const rootRoute = createRootRoute({
  component: () => (
    <>
      <AppRoot>
        <Suspense>
          <Outlet />
        </Suspense>
      </AppRoot>
      <Suspense>
        <TanStackRouterDevtools />
      </Suspense>
    </>
  ),
  notFoundComponent: () => (
    <div className="flex items-center justify-center w-full h-full min-h-screen p-4">
      <Card className="w-auto">
        <CardHeader>
          <CardTitle>
            <FileQuestion className="px-1 inline" /> Kicsit eltévedtél
          </CardTitle>
        </CardHeader>
        <CardContent>Hülyeséget írtál be a link helyére</CardContent>
        <CardFooter>
          <Link className={buttonVariants()} to="/">
            Kezdjük újra
          </Link>
        </CardFooter>
      </Card>
    </div>
  )
})

const terminalRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => (
    <NoNFCBanner>
      <TerminalRoot />
    </NoNFCBanner>
  )
})

const AdminRoot = lazy(() => import('@/AdminRoot.tsx'))
const adminRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: 'admin',
  component: () => (
    <Suspense
      fallback={
        <div className="h-lvh flex justify-center">
          <LoadingIndicator />
        </div>
      }
    >
      <AdminRoot />
    </Suspense>
  )
})

const routeTree = rootRoute.addChildren([terminalRoute, adminRoute])

export const AppRouter = createRouter({ routeTree, notFoundMode: 'root' })
