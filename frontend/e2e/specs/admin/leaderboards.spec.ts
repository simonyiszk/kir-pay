import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createItem, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe('Admin - Leaderboards API', () => {
  let api: ReturnType<typeof apiClient>

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test('GET /consumption-leaderboard returns array with default limit (200)', async () => {
    const { status, body } = await api.get('/consumption-leaderboard')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /consumption-leaderboard with custom limit (200)', async () => {
    const { status, body } = await api.get('/consumption-leaderboard', { limit: 5 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    expect(body.length).toBeLessThanOrEqual(5)
  })

  test('GET /item-leaderboard returns array with default limit (200)', async () => {
    const { status, body } = await api.get('/item-leaderboard')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /item-leaderboard with custom limit (200)', async () => {
    const { status, body } = await api.get('/item-leaderboard', { limit: 5 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    expect(body.length).toBeLessThanOrEqual(5)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/consumption-leaderboard')
    expect(status).toBe(401)
  })
})

test.describe.serial('Admin - Leaderboards with Data', () => {
  let adminApi: ReturnType<typeof apiClient>
  let terminalApi: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testCard: string
  let testItemId: number

  test.beforeAll(async ({ authToken }) => {
    adminApi = apiClient('/v1/api/admin', authToken)
    terminalApi = apiClient('/v1/api/terminal', authToken)
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Leaderboard Test User',
        balance: 5000
      })
    )
    testCard = account.card!
    const item = await cleanup.trackItem(
      createItem({
        name: 'Leaderboard Item',
        cost: 200,
        stock: 50,
        enabled: true,
        showOnLeaderboard: true
      })
    )
    testItemId = item.id

    await terminalApi.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 3, usedVoucher: false }],
      idempotencyKey: randomUUID()
    })
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('GET /consumption-leaderboard shows data after checkout', async () => {
    const { status, body } = await adminApi.get<Array<{ name?: string; itemCount?: number }>>('/consumption-leaderboard', { limit: 10 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    const entry = body.find((e) => e.name === 'Leaderboard Test User')
    expect(entry).toBeDefined()
    expect(typeof entry!.itemCount).toBe('number')
  })

  test('GET /item-leaderboard shows data after checkout', async () => {
    const { status, body } = await adminApi.get<Array<{ itemId?: number; itemName?: string; itemCount?: number }>>('/item-leaderboard', {
      limit: 10
    })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    const entry = body.find((e) => e.itemName === 'Leaderboard Item')
    expect(entry).toBeDefined()
    expect(typeof entry!.itemId).toBe('number')
    expect(typeof entry!.itemName).toBe('string')
    expect(entry!.itemCount).toBe(3)
  })
})
