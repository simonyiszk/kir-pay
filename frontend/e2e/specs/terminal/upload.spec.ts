import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe.serial('Terminal - Upload API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testCard: string

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/terminal', authToken)
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Upload Test User',
        balance: 0
      })
    )
    testCard = account.card!
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /account-by-card/{card}/upload adds balance (200)', async () => {
    const { status, body } = await api.post(`/account-by-card/${testCard}/upload`, {
      amount: 500,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(200)
    expect(body.balance).toBe(500)
  })

  test('POST /account-by-card/{card}/upload same idempotency key replay (200)', async () => {
    const key = randomUUID()
    await api.post(`/account-by-card/${testCard}/upload`, { amount: 300, idempotencyKey: key })
    const { status } = await api.post(`/account-by-card/${testCard}/upload`, { amount: 300, idempotencyKey: key })
    expect(status).toBe(200)
  })

  test('POST /account-by-card/{card}/upload idempotency key mismatch returns 422', async () => {
    const key = randomUUID()
    await api.post(`/account-by-card/${testCard}/upload`, { amount: 100, idempotencyKey: key })
    const { status } = await api.post(`/account-by-card/${testCard}/upload`, { amount: 999, idempotencyKey: key })
    expect(status).toBe(422)
  })

  test('POST /account-by-card/{card}/upload amount <= 0 returns 400', async () => {
    const { status } = await api.post(`/account-by-card/${testCard}/upload`, {
      amount: 0,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /account-by-card/{card}/upload non-existent card returns error (4xx)', async () => {
    const { status } = await api.post('/account-by-card/NONEXISTENT/upload', {
      amount: 100,
      idempotencyKey: randomUUID()
    })
    expect(status).toBeGreaterThanOrEqual(400)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.post('/account-by-card/TEST/upload', {
      amount: 100,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(401)
  })
})
