import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createItem, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe.serial('Terminal - Checkout API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testCard: string
  let testItemId: number

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/terminal', authToken)
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Checkout Test User',
        balance: 5000
      })
    )
    testCard = account.card!
    const item = await cleanup.trackItem(
      createItem({
        name: 'Checkout Item',
        cost: 200,
        stock: 50,
        enabled: true
      })
    )
    testItemId = item.id
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /account-by-card/{card}/checkout processes order (200)', async () => {
    const { status, body } = await api.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 2, usedVoucher: false }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(200)
    expect(body.orderId).toBeDefined()
    expect(body.replayed).toBe(false)
  })

  test('POST /account-by-card/{card}/checkout with custom item (200)', async () => {
    const { status, body } = await api.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [
        {
          itemId: null,
          itemCount: 1,
          usedVoucher: false,
          message: 'Custom item description',
          paidAmount: 300
        }
      ],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(200)
    expect(body.orderId).toBeDefined()
  })

  test('POST /account-by-card/{card}/checkout idempotency replay (200)', async () => {
    const key = randomUUID()
    const payload = {
      orderLines: [{ itemId: testItemId, itemCount: 1, usedVoucher: false }],
      idempotencyKey: key
    }
    await api.post(`/account-by-card/${testCard}/checkout`, payload)
    const { status, body } = await api.post(`/account-by-card/${testCard}/checkout`, payload)
    expect(status).toBe(200)
    expect(body.replayed).toBe(true)
  })

  test('POST /account-by-card/{card}/checkout idempotency mismatch returns 422', async () => {
    const key = randomUUID()
    await api.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 1, usedVoucher: false }],
      idempotencyKey: key
    })
    const { status } = await api.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 5, usedVoucher: false }],
      idempotencyKey: key
    })
    expect(status).toBe(422)
  })

  test('POST /account-by-card/{card}/checkout empty orderLines returns 400', async () => {
    const { status } = await api.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /account-by-card/{card}/checkout insufficient stock returns error (4xx)', async () => {
    const { status } = await api.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 99999, usedVoucher: false }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBeGreaterThanOrEqual(400)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.post('/account-by-card/TEST/checkout', {
      orderLines: [{ itemId: 1, itemCount: 1, usedVoucher: false }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(401)
  })
})

test.describe.serial('Terminal - Checkout with Voucher Redemption', () => {
  let terminalApi: ReturnType<typeof apiClient>
  let adminApi: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testCard: string
  let testItemId: number
  let voucherId: number

  test.beforeAll(async ({ authToken }) => {
    terminalApi = apiClient('/v1/api/terminal', authToken)
    adminApi = apiClient('/v1/api/admin', authToken)
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Voucher Checkout User',
        balance: 5000
      })
    )
    testCard = account.card!
    const item = await cleanup.trackItem(
      createItem({
        name: 'Voucher Checkout Item',
        cost: 200,
        stock: 50,
        enabled: true
      })
    )
    testItemId = item.id

    const { status, body } = await adminApi.post<{ id: number }>('/vouchers', {
      accountId: account.id,
      itemId: testItemId,
      count: 10
    })
    if (status === 200) {
      voucherId = body.id
      cleanup.trackVoucher(Promise.resolve(body))
    }
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /checkout with usedVoucher=true redeems voucher successfully (200)', async () => {
    const { status, body } = await terminalApi.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 2, usedVoucher: true }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(200)
    expect(body.orderId).toBeDefined()
    expect(body.replayed).toBe(false)
  })

  test('POST /checkout with usedVoucher=true and paidAmount returns 400', async () => {
    const { status } = await terminalApi.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 1, usedVoucher: true, paidAmount: 100 }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /checkout with usedVoucher=true and itemId=null returns 400', async () => {
    const { status } = await terminalApi.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: null, itemCount: 1, usedVoucher: true }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /checkout with usedVoucher=true and itemCount=0 returns 400', async () => {
    const { status } = await terminalApi.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 0, usedVoucher: true }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /checkout voucher idempotency replay (200)', async () => {
    const key = randomUUID()
    const payload = {
      orderLines: [{ itemId: testItemId, itemCount: 1, usedVoucher: true }],
      idempotencyKey: key
    }
    await terminalApi.post(`/account-by-card/${testCard}/checkout`, payload)
    const { status, body } = await terminalApi.post(`/account-by-card/${testCard}/checkout`, payload)
    expect(status).toBe(200)
    expect(body.replayed).toBe(true)
  })

  test('POST /checkout with usedVoucher=true exceeds voucher count returns 400', async () => {
    const { status } = await terminalApi.post(`/account-by-card/${testCard}/checkout`, {
      orderLines: [{ itemId: testItemId, itemCount: 20, usedVoucher: true }],
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })
})
