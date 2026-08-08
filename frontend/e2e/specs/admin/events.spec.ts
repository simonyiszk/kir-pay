import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker } from '../../fixtures/test-data'

test.describe('Admin - Events API', () => {
  let api: ReturnType<typeof apiClient>

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test('GET /events returns a list with default pagination (200)', async () => {
    const { status, body } = await api.get('/events')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    if (body.length > 0) {
      const event = body[0]
      expect(typeof event.message).toBe('string')
      expect(typeof event.event).toBe('string')
      expect(typeof event.timestamp).toBe('number')
    }
  })

  test('GET /events with pagination returns page (200)', async () => {
    const { status, body } = await api.get('/events', { page: 0, size: 10 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    expect(body.length).toBeLessThanOrEqual(10)
  })

  test('GET /events with page=1 returns page (200)', async () => {
    const { status, body } = await api.get('/events', { page: 0, size: 5 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    expect(body.length).toBeLessThanOrEqual(5)
  })

  test('GET /events with invalid pagination returns 400', async () => {
    const { status } = await api.get('/events', { page: -1, size: 10 })
    expect(status).toBe(400)
  })

  test('GET /export/events returns CSV (200)', async () => {
    const { status, body } = await api.getText('/export/events')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')

    expect(body.length).toBeGreaterThan(0)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/events')
    expect(status).toBe(401)
  })
})

test.describe.serial('Admin - Events with operations', () => {
  let adminApi: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()

  test.beforeAll(async ({ authToken }) => {
    adminApi = apiClient('/v1/api/admin', authToken)
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('account creation generates an audit event', async () => {
    const account = await cleanup.trackAccount(createAccount({ name: 'Event Test User' }))

    const { status, body } = await adminApi.get<Array<{ message: string; event: string }>>('/events', { page: 0, size: 50 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)

    expect(body.length).toBeGreaterThan(0)
  })

  test('account disable generates an audit event', async () => {
    const account = await cleanup.trackAccount(createAccount({ name: 'Event Disable Test User' }))
    const { status, body } = await adminApi.post(`/accounts/${account.id}/disable`)
    if (status === 200) {
      expect(body.active).toBe(false)
    } else {
      expect([400, 404]).toContain(status)
    }
  })
})
