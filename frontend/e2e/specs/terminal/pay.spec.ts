import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe.serial('Terminal - Pay API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testCard: string

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/terminal', authToken)
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Pay Test User',
        balance: 2000
      })
    )
    testCard = account.card!
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /account-by-card/{card}/pay deducts balance (200)', async () => {
    const { status, body } = await api.post(`/account-by-card/${testCard}/pay`, {
      amount: 500,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(200)
    expect(body.balance).toBe(1500)
  })

  test('POST /account-by-card/{card}/pay same idempotency key replay (200)', async () => {
    const key = randomUUID()
    await api.post(`/account-by-card/${testCard}/pay`, { amount: 100, idempotencyKey: key })
    const { status } = await api.post(`/account-by-card/${testCard}/pay`, { amount: 100, idempotencyKey: key })
    expect(status).toBe(200)
  })

  test('POST /account-by-card/{card}/pay idempotency key mismatch returns 422', async () => {
    const key = randomUUID()
    await api.post(`/account-by-card/${testCard}/pay`, { amount: 50, idempotencyKey: key })
    const { status } = await api.post(`/account-by-card/${testCard}/pay`, { amount: 999, idempotencyKey: key })
    expect(status).toBe(422)
  })

  test('POST /account-by-card/{card}/pay insufficient balance returns 400', async () => {
    const { status } = await api.post(`/account-by-card/${testCard}/pay`, {
      amount: 999999,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /account-by-card/{card}/pay amount <= 0 returns 400', async () => {
    const { status } = await api.post(`/account-by-card/${testCard}/pay`, {
      amount: 0,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /account-by-card/{card}/pay non-existent card returns error (4xx)', async () => {
    const { status } = await api.post('/account-by-card/NONEXISTENT/pay', {
      amount: 100,
      idempotencyKey: randomUUID()
    })
    expect(status).toBeGreaterThanOrEqual(400)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.post('/account-by-card/TEST/pay', {
      amount: 100,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(401)
  })
})
