import { test, expect } from '../../fixtures/auth.fixture'
import {
  apiClient,
  createAccount,
  createItem,
  createVoucher,
  createCleanupTracker,
  E2EAccount,
  E2EItem,
  randomUUID
} from '../../fixtures/test-data'

test.describe.serial('Admin - Vouchers API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testAccount: E2EAccount
  let testItem: E2EItem
  let createdVoucherId: number

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)

    testAccount = await cleanup.trackAccount(createAccount({ name: 'Voucher Test User', balance: 1000 }))
    testItem = await cleanup.trackItem(createItem({ name: 'Voucher Test Item', cost: 200 }))
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /vouchers creates voucher (200)', async () => {
    const { status } = await api.post('/vouchers', {
      accountId: testAccount.id,
      itemId: testItem.id,
      count: 5
    })
    expect(status).toBe(200)

    const { body: vouchers } = await api.get<Array<{ id: number; accountId: number; itemId: number; count: number }>>('/vouchers')
    const found = vouchers.find((v) => v.accountId === testAccount.id && v.itemId === testItem.id)
    expect(found).toBeDefined()
    expect(found!.count).toBe(5)
    createdVoucherId = found!.id
    cleanup.trackVoucher(Promise.resolve(found!))
  })

  test('POST /vouchers with count < 0 returns 400', async () => {
    const { status } = await api.post('/vouchers', {
      accountId: testAccount.id,
      itemId: testItem.id,
      count: -1
    })
    expect(status).toBe(400)
  })

  test('POST /vouchers duplicate account+item returns 400', async () => {
    const { status } = await api.post('/vouchers', {
      accountId: testAccount.id,
      itemId: testItem.id,
      count: 3
    })
    expect(status).toBe(400)
  })

  test('GET /vouchers returns list (200)', async () => {
    const { status, body } = await api.get('/vouchers')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('GET /vouchers with pagination (200)', async () => {
    const { status, body } = await api.get('/vouchers', { page: 0, size: 10 })
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)
  })

  test('POST /items/{itemId}/voucher batch creates or rejects duplicates', async () => {
    const { status } = await api.post(`/items/${testItem.id}/voucher`, {
      count: 3,
      accounts: [testAccount.id]
    })

    expect([200, 400]).toContain(status)
  })

  test('POST /vouchers/{id}/count updates count (200)', async () => {
    const { status, body } = await api.post(`/vouchers/${createdVoucherId}/count`, { count: 10 })
    expect(status).toBe(200)
    expect(body.count).toBe(10)
  })

  test('POST /vouchers/{id}/count with negative returns 400', async () => {
    const { status } = await api.post(`/vouchers/${createdVoucherId}/count`, { count: -1 })
    expect(status).toBe(400)
  })

  test('POST /vouchers/{id}/increment adds delta (200)', async () => {
    const { status, body } = await api.post(`/vouchers/${createdVoucherId}/increment`, { delta: 3, idempotencyKey: randomUUID() })
    expect(status).toBe(200)
    expect(body.count).toBe(13)
  })

  test('POST /vouchers/{id}/increment below zero returns 400', async () => {
    const { status } = await api.post(`/vouchers/${createdVoucherId}/increment`, { delta: -100, idempotencyKey: randomUUID() })
    expect(status).toBe(400)
  })

  test('DELETE /vouchers/{id} non-existent returns 400', async () => {
    const { status } = await api.del('/vouchers/99999')
    expect(status).toBe(400)
  })

  test('DELETE /vouchers/{id} deletes voucher (200)', async () => {
    const { status } = await api.del(`/vouchers/${createdVoucherId}`)
    expect(status).toBe(200)
  })

  test('GET /export/vouchers returns CSV (200)', async () => {
    const { status, body } = await api.getText('/export/vouchers')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
  })

  test('GET /template/vouchers returns CSV (200)', async () => {
    const { status, body } = await api.getText('/template/vouchers')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
  })

  test('POST /import/vouchers handles missing accountId rows', async () => {
    const csv = `accountId,itemId,count\n,${testItem.id},5`
    const { status, body } = await api.postText<{ imported: number; total: number; errors: string[] }>('/import/vouchers', csv, {
      idempotencyKey: randomUUID()
    })
    expect([200, 201]).toContain(status)

    expect(typeof body.imported).toBe('number')
    expect(typeof body.total).toBe('number')
    expect(Array.isArray(body.errors)).toBe(true)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/vouchers')
    expect(status).toBe(401)
  })
})
