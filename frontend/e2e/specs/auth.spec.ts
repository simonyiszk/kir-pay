import { test, expect } from '../fixtures/auth.fixture'
import { loginToken, loginViaApi } from '../fixtures/auth.fixture'
import { setupNFCMock } from '../fixtures/nfc.fixture'
import { apiClient, createPrincipal, createCleanupTracker, randomUUID } from '../fixtures/test-data'

test.describe('Authentication - UI', () => {
  test('login form is shown when not authenticated', async ({ page }) => {
    await setupNFCMock(page)
    await page.goto('/')
    await expect(page.getByRole('button', { name: 'Belépés' })).toBeVisible()
    await expect(page.locator('input[autocomplete="accountname"]')).toBeVisible()
    await expect(page.locator('input[autocomplete="current-password"]')).toBeVisible()
  })

  test('valid session cookie loads the app', async ({ page }) => {
    await loginViaApi(page)
    await setupNFCMock(page)
    await page.goto('/')

    await expect(page.locator('header')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Belépés' })).not.toBeVisible()
  })

  test('login dialog accepts valid credentials', async ({ page }) => {
    // Clear any session cookie from previous tests so the login form is shown
    await page.context().clearCookies()
    await setupNFCMock(page)
    await page.goto('/')

    await expect(page.getByRole('button', { name: 'Belépés' })).toBeVisible()
    const usernameInput = page.locator('input[autocomplete="accountname"]')
    const passwordInput = page.locator('input[autocomplete="current-password"]')
    await usernameInput.fill('admin')
    await passwordInput.fill('admin')

    // Wait for login API call and the subsequent re-render
    const loginPromise = page.waitForResponse((resp) => resp.url().includes('/v1/api/login') && resp.request().method() === 'POST', {
      timeout: 15000
    })
    await page.getByRole('button', { name: 'Belépés' }).click()
    const loginResponse = await loginPromise
    expect(loginResponse.status()).toBe(204)

    await expect(page.locator('header')).toBeVisible({ timeout: 15000 })
  })

  test('logout from header menu returns to login', async ({ page }) => {
    await loginViaApi(page)
    await setupNFCMock(page)
    await page.goto('/')
    await expect(page.locator('header')).toBeVisible()

    await page.locator('header button[aria-haspopup="menu"]').click()
    await page.getByRole('menuitem', { name: 'Kijelentkezés' }).click()

    await expect(page.getByRole('button', { name: 'Belépés' })).toBeVisible()
  })
})

test.describe('Authentication - API', () => {
  const cleanup = createCleanupTracker()
  let limitedToken: string

  test.beforeAll(async ({ authToken }) => {
    const adminApi = apiClient('/v1/api/admin', authToken)
    const suffix = Date.now().toString(36)
    const { status: createStatus, body: principal } = await adminApi.post<{ id: number; name: string }>('/principals', {
      name: `e2e-limited-${suffix}`,
      password: 'limited-pw',
      role: 'TERMINAL',
      canUpload: false,
      canTransfer: false,
      canSellItems: false,
      canRedeemVouchers: false,
      canAssignCards: false,
      active: true,
      idempotencyKey: randomUUID()
    })
    expect(createStatus).toBe(200)
    cleanup.trackPrincipal(Promise.resolve(principal))
    limitedToken = await loginToken(`e2e-limited-${suffix}`, 'limited-pw')
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('admin endpoint without auth returns 401', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/accounts')
    expect(status).toBe(401)
  })

  test('admin endpoint with invalid token returns 401', async () => {
    const badAuth = apiClient('/v1/api/admin', 'invalid-session-token')
    const { status } = await badAuth.get('/accounts')
    expect(status).toBe(401)
  })

  test('terminal endpoint without auth returns 401', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.get('/accounts')
    expect(status).toBe(401)
  })

  test('app endpoint without auth returns 401', async () => {
    const noAuth = apiClient('/v1/api', '')
    const { status } = await noAuth.get('/app')
    expect(status).toBe(401)
  })

  test('terminal upload with principal lacking UPLOAD_FUNDS is rejected (4xx)', async () => {
    const limitedApi = apiClient('/v1/api/terminal', limitedToken)
    const { status } = await limitedApi.post('/account-by-card/TEST/upload', {
      amount: 100,
      idempotencyKey: '11111111-1111-1111-1111-111111111111'
    })
    expect(status).toBeGreaterThanOrEqual(400)
    expect(status).not.toBe(200)
  })

  test('terminal transfer with principal lacking TRANSFER_FUNDS is rejected (4xx)', async () => {
    const limitedApi = apiClient('/v1/api/terminal', limitedToken)
    const { status } = await limitedApi.post('/account-by-card/A/transfer', {
      recipientCard: 'B',
      amount: 100,
      idempotencyKey: '22222222-2222-2222-2222-222222222222'
    })
    expect(status).toBeGreaterThanOrEqual(400)
    expect(status).not.toBe(200)
  })

  test('terminal checkout with principal lacking SELL_ITEMS is rejected (4xx)', async () => {
    const limitedApi = apiClient('/v1/api/terminal', limitedToken)
    const { status } = await limitedApi.post('/account-by-card/TEST/checkout', {
      orderLines: [{ itemId: 1, itemCount: 1, usedVoucher: false }],
      idempotencyKey: '33333333-3333-3333-3333-333333333333'
    })
    expect(status).toBeGreaterThanOrEqual(400)
    expect(status).not.toBe(200)
  })
})
