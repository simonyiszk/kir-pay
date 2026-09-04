import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker, createItem, payByCard, randomUUID } from '../../fixtures/test-data'

test.describe('Admin - Analytics API', () => {
  let api: ReturnType<typeof apiClient>

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test('GET /analytics returns AnalyticsDto with all fields (200)', async () => {
    const { status, body } = await api.get<{
      accountCount: number
      transactionCount: number
      allActiveBalance: number
      income: number
      allUploads: number
      transactionVolume: number
    }>('/analytics')

    expect(status).toBe(200)
    expect(typeof body.accountCount).toBe('number')
    expect(typeof body.transactionCount).toBe('number')
    expect(typeof body.allActiveBalance).toBe('number')
    expect(typeof body.income).toBe('number')
    expect(typeof body.allUploads).toBe('number')
    expect(typeof body.transactionVolume).toBe('number')
    expect(body.accountCount).toBeGreaterThanOrEqual(0)
    expect(body.transactionCount).toBeGreaterThanOrEqual(0)
    expect(body.allActiveBalance).toBeGreaterThanOrEqual(0)
  })

  test('GET /analytics rejects unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/analytics')
    expect(status).toBe(401)
  })

  test('GET /analytics/revenue-heatmap returns the full 7x24 grid (200)', async () => {
    const { status, body } = await api.get<{ date: string; hour: number; revenue: number }[]>('/analytics/revenue-heatmap')

    expect(status).toBe(200)
    expect(body).toHaveLength(168)
    expect(new Set(body.map((row) => `${row.date}:${row.hour}`)).size).toBe(168)
    expect(new Set(body.map((row) => row.date)).size).toBe(7)
    for (const row of body) {
      expect(row.date).toMatch(/^\d{4}-\d{2}-\d{2}$/)
      expect(row.hour).toBeGreaterThanOrEqual(0)
      expect(row.hour).toBeLessThanOrEqual(23)
      expect(typeof row.revenue).toBe('number')
      expect(row.revenue).toBeGreaterThanOrEqual(0)
    }
  })

  test('GET /analytics/revenue-heatmap rejects unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/analytics/revenue-heatmap')
    expect(status).toBe(401)
  })
})

test.describe.serial('Admin - Analytics after operations', () => {
  let adminApi: ReturnType<typeof apiClient>
  let terminalApi: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()

  test.beforeAll(async ({ authToken }) => {
    adminApi = apiClient('/v1/api/admin', authToken)
    terminalApi = apiClient('/v1/api/terminal', authToken)
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('analytics values change after creating account and performing operations', async () => {
    const { body: before } = await adminApi.get<{
      accountCount: number
      transactionCount: number
      allActiveBalance: number
      income: number
      allUploads: number
    }>('/analytics')

    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Analytics Test User',
        balance: 2000
      })
    )

    const { body: afterCreate } = await adminApi.get<{ accountCount: number; allActiveBalance: number }>('/analytics')
    expect(afterCreate.accountCount).toBeGreaterThanOrEqual(before.accountCount + 1)

    const card = account.card!
    await payByCard(card, 100, randomUUID())

    const { status, body: afterOps } = await adminApi.get<{ transactionCount: number; income: number }>('/analytics')
    expect(status).toBe(200)
    expect(afterOps.transactionCount).toBeGreaterThanOrEqual(before.transactionCount + 1)
    expect(afterOps.income).toBeGreaterThanOrEqual(before.income + 100)
  })

  test('revenue heatmap increases after a checkout', async () => {
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Heatmap Test User',
        balance: 2000
      })
    )
    const item = await cleanup.trackItem(
      createItem({
        name: 'Heatmap Item',
        cost: 150,
        stock: 50,
        enabled: true
      })
    )

    const { body: before } = await adminApi.get<{ date: string; hour: number; revenue: number }[]>('/analytics/revenue-heatmap')
    const beforeSum = before.reduce((sum, row) => sum + row.revenue, 0)

    const { status } = await terminalApi.post(`/account-by-card/${account.card!}/checkout`, {
      orderLines: [{ itemId: item.id, itemCount: 1, usedVoucher: false }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(200)

    const { body: after } = await adminApi.get<{ date: string; hour: number; revenue: number }[]>('/analytics/revenue-heatmap')
    const afterSum = after.reduce((sum, row) => sum + row.revenue, 0)

    expect(afterSum).toBeGreaterThanOrEqual(beforeSum + 150)
  })
})
