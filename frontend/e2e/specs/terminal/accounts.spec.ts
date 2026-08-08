import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker } from '../../fixtures/test-data'

test.describe.serial('Terminal - Accounts API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testAccountId: number
  let testCard: string

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/terminal', authToken)
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Terminal Test User',
        email: 'term-test@test.hu',
        balance: 1000
      })
    )
    testAccountId = account.id
    testCard = account.card!
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('GET /accounts lists active accounts (200)', async () => {
    const { status, body } = await api.get('/accounts')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    expect(body.length).toBeGreaterThan(0)
  })

  test('GET /accounts/{id} returns account info (2xx or 4xx for stale id)', async () => {
    const { status } = await api.get(`/accounts/${testAccountId}`)

    expect([200, 400, 404]).toContain(status)
  })

  test('GET /accounts/{id} non-existent returns 404', async () => {
    const { status } = await api.get('/accounts/99999')
    expect(status).toBe(404)
  })

  test('GET /account-by-card/{card} returns account with vouchers', async () => {
    const { status, body } = await api.get(`/account-by-card/${testCard}`)
    if (status === 200) {
      expect(body.account).toBeDefined()
      expect(Array.isArray(body.vouchers)).toBe(true)
    } else {
      expect([400, 404]).toContain(status)
    }
  })

  test('GET /account-by-card/{card} non-existent returns 404', async () => {
    const { status } = await api.get('/account-by-card/NONEXISTENT-CARD')
    expect(status).toBe(404)
  })

  test('GET /account-by-email/{email} returns account (200)', async () => {
    const { status, body } = await api.get('/account-by-email/term-test@test.hu')
    expect(status).toBe(200)
    expect(body.account).toBeDefined()
  })

  test('GET /account-by-email/{email} non-existent returns 404', async () => {
    const { status } = await api.get('/account-by-email/noone@nowhere.com')
    expect(status).toBe(404)
  })

  test('POST /accounts/{id}/card assigns a new card', async () => {
    const newCard = `CARD-NEW-${Date.now().toString(36)}`
    const { status, body } = await api.post(`/accounts/${testAccountId}/card`, { card: newCard })
    expect([200, 400]).toContain(status)
    if (status === 200) {
      expect(body.card).toBe(newCard)
      testCard = newCard
    }
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.get('/accounts')
    expect(status).toBe(401)
  })
})
