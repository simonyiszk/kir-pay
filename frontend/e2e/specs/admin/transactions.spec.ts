import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient } from '../../fixtures/test-data'

test.describe('Admin - Transactions API', () => {
  let api: ReturnType<typeof apiClient>

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test('GET /transactions returns a list (200)', async () => {
    const { status, body } = await api.get('/transactions')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /transactions with pagination returns page (200)', async () => {
    const { status, body } = await api.get('/transactions', { page: 0, size: 10 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    expect(body.length).toBeLessThanOrEqual(10)
  })

  test('GET /transactions with invalid pagination returns 400', async () => {
    const { status } = await api.get('/transactions', { page: -1, size: 10 })
    expect(status).toBe(400)
  })

  test('GET /export/transactions returns CSV with header (200)', async () => {
    const { status, body } = await api.getText('/export/transactions')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
    expect(body.length).toBeGreaterThan(0)
  })

  test('GET /transactions with invalid size (0) returns 400', async () => {
    const { status } = await api.get('/transactions', { page: 0, size: 0 })
    expect(status).toBe(400)
  })

  test('GET /transactions with negative size returns 400', async () => {
    const { status } = await api.get('/transactions', { page: 0, size: -1 })
    expect(status).toBe(400)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/transactions')
    expect(status).toBe(401)
  })
})
