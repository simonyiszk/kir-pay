import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createItem, createCleanupTracker } from '../../fixtures/test-data'

test.describe('Terminal - Items API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/terminal', authToken)
    await cleanup.trackItem(createItem({ name: 'Active Terminal Item', enabled: true }))

    await cleanup.trackItem(createItem({ name: 'Disabled Terminal Item', enabled: false }))
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('GET /items lists only active items (200)', async () => {
    const { status, body } = await api.get<Array<{ name: string; enabled: boolean }>>('/items')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)

    for (const item of body) {
      expect(item.enabled).toBe(true)
    }
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.get('/items')
    expect(status).toBe(401)
  })
})
