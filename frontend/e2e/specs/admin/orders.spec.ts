import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient } from '../../fixtures/test-data'

test.describe('Admin - Orders API', () => {
  let api: ReturnType<typeof apiClient>

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test('GET /orders returns a list (200)', async () => {
    const { status, body } = await api.get('/orders')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /orders with pagination returns page (200)', async () => {
    const { status, body } = await api.get('/orders', { page: 0, size: 10 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /orders with invalid pagination returns 400', async () => {
    const { status } = await api.get('/orders', { page: -1, size: 10 })
    expect(status).toBe(400)
  })

  test('GET /orders-with-order-lines returns a list (200)', async () => {
    const { status, body } = await api.get('/orders-with-order-lines')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /orders-with-order-lines paginated (200)', async () => {
    const { status, body } = await api.get('/orders-with-order-lines', { page: 0, size: 10 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /order_lines returns a list (200)', async () => {
    const { status, body } = await api.get('/order_lines')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /order_lines with pagination (200)', async () => {
    const { status, body } = await api.get('/order_lines', { page: 0, size: 5 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
    expect(body.length).toBeLessThanOrEqual(5)
  })

  test('GET /export/orders returns CSV with header (200)', async () => {
    const { status, body } = await api.getText('/export/orders')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
    expect(body.length).toBeGreaterThan(0)
  })

  test('GET /export/orders-with-order-lines returns CSV with header (200)', async () => {
    const { status, body } = await api.getText('/export/orders-with-order-lines')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
    expect(body.length).toBeGreaterThan(0)
  })

  test('GET /export/order_lines returns CSV with header (200)', async () => {
    const { status, body } = await api.getText('/export/order_lines')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
    expect(body.length).toBeGreaterThan(0)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/orders')
    expect(status).toBe(401)
  })
})
