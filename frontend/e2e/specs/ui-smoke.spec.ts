import { test, expect } from '../fixtures/auth.fixture'
import { loginViaApi } from '../fixtures/auth.fixture'
import { TerminalPage } from '../pages/terminal.page'
import { AdminPage } from '../pages/admin.page'
import { NFC_INIT_SCRIPT } from '../fixtures/nfc.fixture'

test.describe('UI Smoke - Terminal', () => {
  let terminal: TerminalPage

  test.beforeEach(async ({ page }) => {
    await loginViaApi(page)
    await page.addInitScript(NFC_INIT_SCRIPT)
    terminal = new TerminalPage(page)
    await terminal.goto()
  })

  test('terminal page loads with tabs', async ({ page }) => {
    await expect(page.locator('header')).toBeVisible()

    await expect(page.getByRole('tablist')).toBeVisible()
  })

  test('navigate to pay tab shows amount entry', async ({ page }) => {
    await terminal.navigateToTab('pay')
    await expect(page.getByText(/Add meg a fizetendő|összeget/i).first()).toBeVisible({ timeout: 5000 })
  })

  test('navigate to upload tab shows amount entry', async ({ page }) => {
    await terminal.navigateToTab('upload')
    await expect(page.getByText(/Add meg a feltöltés|mennyiségét/i).first()).toBeVisible({ timeout: 5000 })
  })

  test('logout from terminal works', async ({ page }) => {
    await terminal.openHeaderMenu()
    await page.getByRole('menuitem', { name: 'Kijelentkezés' }).click()
    await expect(page.getByRole('button', { name: 'Belépés' })).toBeVisible()
  })
})

test.describe('UI Smoke - Admin', () => {
  let admin: AdminPage

  test.beforeEach(async ({ page }) => {
    await loginViaApi(page)
    admin = new AdminPage(page)
    await admin.goto()
  })

  test('admin page loads with header', async ({ page }) => {
    await expect(page.locator('header')).toBeVisible()
  })

  test('navigate to accounts tab', async ({ page }) => {
    await admin.navigateToTab('accounts')
    await admin.expectHeading('Felhasználók')
  })

  test('navigate to items tab', async ({ page }) => {
    await admin.navigateToTab('items')
    await admin.expectHeading('Termékek')
  })

  test('navigate to analytics tab', async ({ page }) => {
    await admin.navigateToTab('analytics')
    await admin.expectHeading('Analitika')
  })

  test('logout from admin works', async ({ page }) => {
    await admin.openHeaderMenu()
    await page.getByRole('menuitem', { name: 'Kijelentkezés' }).click()
    await expect(page.getByRole('button', { name: 'Belépés' })).toBeVisible()
  })
})

test.describe('UI Smoke - NFC', () => {
  test('shows unsupported browser banner when NDEFReader is not available', async ({ page }) => {
    await loginViaApi(page)
    await page.goto('/')
    await expect(page.getByText('Nem támogatott böngésző.')).toBeVisible()
    await expect(page.getByText('Web NFC')).toBeVisible()
  })

  test('login form is NOT shown when session is valid but NDEFReader is absent', async ({ page }) => {
    await loginViaApi(page)
    await page.goto('/')
    await expect(page.getByRole('button', { name: 'Belépés' })).not.toBeVisible()
  })
})
