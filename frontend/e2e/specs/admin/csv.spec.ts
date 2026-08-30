import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createItem, createPrincipal, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe('Admin - CSV Import API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /import/accounts endpoint exists and processes CSV', async () => {
    const csv = 'name,email,phone,card,balance,active\nImport Test,,,CARD-IMP-001,500,true'
    const resp = await api.postText('/import/accounts', csv, { idempotencyKey: randomUUID() })

    expect([201, 400]).toContain(resp.status)
  })

  test('POST /import/accounts fails without auth (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.postText('/import/accounts', 'name,balance,active\nTest,100,true')
    expect(status).toBe(401)
  })

  test('POST /import/items imports valid CSV (2xx)', async () => {
    const csv = 'name,cost,stock,enabled\nImport Item,100,10,true'
    const { status } = await api.postText('/import/items', csv, { idempotencyKey: randomUUID() })
    expect(status).toBeGreaterThanOrEqual(200)
    expect(status).toBeLessThan(300)
  })

  test('POST /import/items fails without auth (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.postText('/import/items', 'name,cost,stock,enabled\nTest,100,10,true')
    expect(status).toBe(401)
  })

  test('POST /import/principals endpoint exists and processes CSV', async () => {
    const csv =
      'name,password,role,canUpload,canTransfer,canSellItems,canRedeemVouchers,canAssignCards,active\ncsv-imp-term,pw,TERMINAL,true,true,true,true,true,true'
    const resp = await api.postText('/import/principals', csv, { idempotencyKey: randomUUID() })

    expect([201, 400]).toContain(resp.status)
  })

  test('POST /import/principals fails without auth (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.postText('/import/principals', 'name,password,role\nTest,pw,TERMINAL')
    expect(status).toBe(401)
  })

  test('POST /import/vouchers imports valid CSV and returns stats (201)', async () => {
    const account = await cleanup.trackAccount(createAccount({ name: 'Voucher Import User' }))
    const item = await cleanup.trackItem(createItem({ name: 'Voucher Import Item' }))

    const csv = `accountId,itemId,count\n${account.id},${item.id},5`
    const { status, body } = await api.postText<{ imported: number; total: number; errors: string[] }>('/import/vouchers', csv, {
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(201)
    expect(typeof body.imported).toBe('number')
    expect(typeof body.total).toBe('number')
    expect(Array.isArray(body.errors)).toBe(true)
  })

  test('POST /import/vouchers fails without auth (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.postText('/import/vouchers', 'accountId,itemId,count\n1,1,5')
    expect(status).toBe(401)
  })
})
