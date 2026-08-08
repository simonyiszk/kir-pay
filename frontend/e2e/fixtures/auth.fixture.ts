import { test as base, expect } from '@playwright/test'
import type { Page } from '@playwright/test'

const ADMIN_USER = 'admin'
const ADMIN_PASS = 'admin'
const BACKEND_URL = 'http://localhost:8001'

/**
 * Logs in via the JSON login endpoint and returns the SESSION cookie value for
 * direct API calls (HTTP Basic is disabled in the backend).
 */
export async function loginToken(username: string, password: string): Promise<string> {
  const response = await fetch(`${BACKEND_URL}/v1/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  if (!response.ok) throw new Error(`login failed with status ${response.status}`)
  const session = (response.headers.get('set-cookie') ?? '')
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith('SESSION='))
  if (!session) throw new Error('login response did not contain a SESSION cookie')
  return session.slice('SESSION='.length)
}

/**
 * Logs in via the JSON login endpoint so the SESSION cookie lands in the
 * browser context (page.request shares the context's cookie storage).
 */
export async function loginViaApi(page: Page): Promise<void> {
  const response = await page.request.post(`${BACKEND_URL}/v1/api/login`, {
    data: { username: ADMIN_USER, password: ADMIN_PASS }
  })
  expect(response.ok()).toBeTruthy()
}

export const test = base.extend<{ authToken: string }>({
  authToken: async ({}, use) => {
    const token = await loginToken(ADMIN_USER, ADMIN_PASS)
    await use(token)
  }
})

export { expect } from '@playwright/test'
