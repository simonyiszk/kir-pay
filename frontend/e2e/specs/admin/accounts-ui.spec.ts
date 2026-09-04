import { test, expect } from '../../fixtures/auth.fixture'
import { loginViaApi } from '../../fixtures/auth.fixture'
import { AdminPage } from '../../pages/admin.page'
import { createAccount, createCleanupTracker } from '../../fixtures/test-data'

test.describe('Admin - Accounts UI', () => {
  const cleanup = createCleanupTracker()
  const accountName = `UI Disable Toggle ${Date.now().toString(36)}`

  test.beforeAll(async () => {
    await cleanup.trackAccount(createAccount({ name: accountName, balance: 0 }))
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('disabled account remains visible and can be re-enabled', async ({ page }) => {
    await loginViaApi(page)
    const admin = new AdminPage(page)
    await admin.goto()
    await admin.navigateToTab('accounts')

    const row = page.getByRole('row').filter({ hasText: accountName })
    await expect(row).toBeVisible()

    await admin.openRowActions(row)
    await admin.clickMenuItem('Letiltás')

    // After the refetch the row must still be listed and offer re-enable.
    await expect(row).toBeVisible()
    await admin.openRowActions(row)
    await expect(page.getByRole('menuitem', { name: 'Engedélyezés' })).toBeVisible()
    await admin.clickMenuItem('Engedélyezés')

    await expect(row).toBeVisible()
    await admin.openRowActions(row)
    await expect(page.getByRole('menuitem', { name: 'Letiltás' })).toBeVisible()
  })
})
