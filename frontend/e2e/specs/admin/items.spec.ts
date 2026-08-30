import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createItem, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe.serial('Admin - Items API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let createdItemId: number

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /items creates item (200)', async () => {
    const item = await cleanup.trackItem(createItem({ name: 'CRUD Test Item', cost: 300, stock: 50 }))
    createdItemId = item.id
    expect(item.name).toBe('CRUD Test Item')
    expect(item.cost).toBe(300)
    expect(item.stock).toBe(50)
    expect(item.enabled).toBe(true)
  })

  test('POST /items with missing name returns error (400)', async () => {
    const { status } = await api.post('/items', { cost: 100, stock: 10, enabled: true, idempotencyKey: randomUUID() })
    expect(status).toBe(400)
  })

  test('GET /items returns list (200)', async () => {
    const { status, body } = await api.get('/items')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /items with pagination (200)', async () => {
    const { status, body } = await api.get('/items', { page: 0, size: 10 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /items with invalid pagination returns 400', async () => {
    const { status } = await api.get('/items', { page: -1, size: 10 })
    expect(status).toBe(400)
  })

  test('GET /items/{id} returns item (200)', async () => {
    const { status, body } = await api.get(`/items/${createdItemId}`)
    expect(status).toBe(200)
    expect(body.name).toBe('CRUD Test Item')
  })

  test('GET /items/{id} non-existent returns 400', async () => {
    const { status } = await api.get('/items/99999')
    expect(status).toBe(400)
  })

  test('PUT /items/{id} updates item (200)', async () => {
    const { status, body } = await api.put(`/items/${createdItemId}`, {
      name: 'Updated Item',
      cost: 600,
      stock: 75,
      enabled: true
    })
    expect(status).toBe(200)
    expect(body.name).toBe('Updated Item')
    expect(body.cost).toBe(600)
  })

  test('PUT /items/{id} non-existent returns 400', async () => {
    const { status } = await api.put('/items/99999', {
      name: 'Ghost',
      cost: 100,
      stock: 10,
      enabled: true
    })
    expect(status).toBe(400)
  })

  test('POST /items/{id}/disable sets enabled=false (200)', async () => {
    const { status, body } = await api.post(`/items/${createdItemId}/disable`)
    expect(status).toBe(200)
    expect(body.enabled).toBe(false)
  })

  test('POST /items/{id}/enable sets enabled=true (200)', async () => {
    const { status, body } = await api.post(`/items/${createdItemId}/enable`)
    expect(status).toBe(200)
    expect(body.enabled).toBe(true)
  })

  test('DELETE /items/{id} deletes item (200)', async () => {
    const { status } = await api.del(`/items/${createdItemId}`)
    expect(status).toBe(200)
  })

  test('DELETE /items/{id} non-existent returns 400', async () => {
    const { status } = await api.del('/items/99999')
    expect(status).toBe(400)
  })

  test('GET /export/items returns CSV (200)', async () => {
    const { status, body } = await api.getText('/export/items')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
  })

  test('GET /template/items returns CSV (200)', async () => {
    const { status, body } = await api.getText('/template/items')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/items')
    expect(status).toBe(401)
  })
})
