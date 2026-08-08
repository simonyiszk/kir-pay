import { Page, Locator } from '@playwright/test'

export class AdminPage {
  constructor(public readonly page: Page) {}

  async navigateToTab(tabValue: string): Promise<void> {
    const tab = this.page.locator(`button[role="tab"][data-value="${tabValue}"]`)
    await tab.click()

    await this.page.locator(`[role="tabpanel"][data-state="active"]`).waitFor({ state: 'visible' })
  }

  async goto(): Promise<void> {
    await this.page.goto('/admin')

    await this.page.locator('header').waitFor({ state: 'visible' })
  }

  async openHeaderMenu(): Promise<void> {
    await this.page.locator('header button[aria-haspopup="menu"]').click()
    await this.page.locator('[role="menu"]').waitFor({ state: 'visible' })
  }

  async logout(): Promise<void> {
    await this.openHeaderMenu()
    await this.page.getByRole('menuitem', { name: 'Kijelentkezés' }).click()
  }

  getDialog(): Locator {
    return this.page.locator('[role="dialog"]')
  }

  getToast(): Locator {
    return this.page.locator('[role="status"]')
  }

  async openManagementDropdown(): Promise<void> {
    await this.page.getByRole('button', { name: 'Műveletek' }).click()
    await this.page.locator('[role="menu"]').waitFor({ state: 'visible' })
  }

  async clickMenuItem(name: string, exact = false): Promise<void> {
    await this.page.getByRole('menuitem', { name, exact }).click()
  }

  async openRowActions(rowLocator: Locator): Promise<void> {
    await rowLocator.getByRole('button').click()
    await this.page.locator('[role="menu"]').waitFor({ state: 'visible' })
  }

  getTableRows(): Locator {
    return this.page.locator('table tbody tr')
  }

  async fillField(label: string, value: string): Promise<void> {
    const words = label.split(/\s+/).filter((w) => w.length > 0)
    const pattern = new RegExp(words.map((w) => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('.*'), 'i')

    const item = this.page
      .locator('div')
      .filter({ has: this.page.getByText(pattern) })
      .last()
    const input = item.locator('input, textarea, select').first()
    await input.fill(value)
  }

  async submitForm(): Promise<void> {
    await this.page.getByRole('button', { name: 'Kész' }).click()
  }

  async expectToast(text: string): Promise<void> {
    await this.page.getByText(text).first().waitFor({ state: 'visible' })
  }

  async expectHeading(text: string): Promise<void> {
    await this.page.locator('h1').filter({ hasText: text }).waitFor({ state: 'visible' })
  }

  async expectNotAdmin(): Promise<void> {
    await this.page.getByText('Te nem vagy admin!').waitFor({ state: 'visible' })
  }
}
