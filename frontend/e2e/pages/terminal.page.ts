import { Page, Locator } from '@playwright/test'

export class TerminalPage {
  constructor(public readonly page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/')
    await this.page.locator('header').waitFor({ state: 'visible' })
  }

  async navigateToTab(tabValue: string): Promise<void> {
    const tab = this.page.locator(`button[role="tab"][data-value="${tabValue}"]`)
    await tab.click()
    await this.page.locator(`[role="tabpanel"][data-state="active"]`).waitFor({ state: 'visible' })
  }

  async simulateCardScan(serialNumber: string): Promise<void> {
    await this.page.waitForFunction(() => (window as any).__ndefInstance != null, { timeout: 5000 })
    await this.page.evaluate((serial) => {
      ;(window as any).__triggerNFCCard(serial)
    }, serialNumber)
  }

  async enterAmount(amount: number): Promise<void> {
    await this.page.locator('input[type="number"]').fill(String(amount))
    await this.page.getByRole('button', { name: 'Kész' }).click()
  }

  get amountInput(): Locator {
    return this.page.locator('input[type="number"]')
  }

  async openHeaderMenu(): Promise<void> {
    await this.page.locator('header button[aria-haspopup="menu"]').click()
    await this.page.locator('[role="menu"]').waitFor({ state: 'visible' })
  }

  async logout(): Promise<void> {
    await this.openHeaderMenu()
    await this.page.getByRole('menuitem', { name: 'Kijelentkezés' }).click()
  }

  getToast(): Locator {
    return this.page.locator('[role="status"]')
  }

  async expectMessage(text: string): Promise<void> {
    await this.page.getByText(text).first().waitFor({ state: 'visible' })
  }
}
