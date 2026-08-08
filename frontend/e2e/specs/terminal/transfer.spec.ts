import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe.serial('Terminal - Transfer API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let senderCard: string
  let recipientCard: string

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/terminal', authToken)
    const sender = await cleanup.trackAccount(
      createAccount({
        name: 'Transfer Sender',
        balance: 2000
      })
    )
    const recipient = await cleanup.trackAccount(
      createAccount({
        name: 'Transfer Recipient',
        balance: 0
      })
    )
    senderCard = sender.card!
    recipientCard = recipient.card!
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /account-by-card/{card}/transfer transfers funds (200)', async () => {
    const { status, body } = await api.post(`/account-by-card/${senderCard}/transfer`, {
      recipientCard,
      amount: 500,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(200)
    expect(body.balance).toBe(1500)
  })

  test('POST /account-by-card/{card}/transfer idempotency replay (200)', async () => {
    const key = randomUUID()
    await api.post(`/account-by-card/${senderCard}/transfer`, {
      recipientCard,
      amount: 100,
      idempotencyKey: key
    })
    const { status } = await api.post(`/account-by-card/${senderCard}/transfer`, {
      recipientCard,
      amount: 100,
      idempotencyKey: key
    })
    expect(status).toBe(200)
  })

  test('POST /account-by-card/{card}/transfer sender = recipient returns 400', async () => {
    const { status } = await api.post(`/account-by-card/${senderCard}/transfer`, {
      recipientCard: senderCard,
      amount: 100,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /account-by-card/{card}/transfer insufficient balance returns 400', async () => {
    const { status } = await api.post(`/account-by-card/${senderCard}/transfer`, {
      recipientCard,
      amount: 999999,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /account-by-card/{card}/transfer amount <= 0 returns 400', async () => {
    const { status } = await api.post(`/account-by-card/${senderCard}/transfer`, {
      recipientCard,
      amount: 0,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /account-by-card/{card}/transfer non-existent recipient returns error (4xx)', async () => {
    const { status } = await api.post(`/account-by-card/${senderCard}/transfer`, {
      recipientCard: 'NONEXISTENT-CARD',
      amount: 100,
      idempotencyKey: randomUUID()
    })
    expect(status).toBeGreaterThanOrEqual(400)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.post('/account-by-card/A/transfer', {
      recipientCard: 'B',
      amount: 100,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(401)
  })
})
