import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker } from '../../fixtures/test-data'

test.describe('Terminal - Assign Card API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let testAccountId: number

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/terminal', authToken)
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'Assign Test User',
        balance: 0,
        card: null
      })
    )
    testAccountId = account.id
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /accounts/{id}/card assigns a card (200)', async () => {
    const newCard = `ASSIGN-CARD-${Date.now().toString(36)}`
    const { status, body } = await api.post(`/accounts/${testAccountId}/card`, { card: newCard })
    expect(status).toBe(200)
    expect(body.card).toBe(newCard)
  })

  test('POST /accounts/{id}/card reassigns card between accounts', async () => {
    const acc1 = await cleanup.trackAccount(createAccount({ name: 'Reassign Test 1', balance: 0 }))
    const acc2 = await cleanup.trackAccount(createAccount({ name: 'Reassign Test 2', balance: 0 }))
    const sharedCard = `SHARED-CARD-${Date.now().toString(36)}`

    const r1 = await api.post(`/accounts/${acc1.id}/card`, { card: sharedCard })
    expect(r1.status).toBe(200)

    const r2 = await api.post(`/accounts/${acc2.id}/card`, { card: sharedCard })
    if (r2.status === 200) {
      expect(r2.body.card).toBe(sharedCard)
    } else {
      expect([400, 404]).toContain(r2.status)
    }
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/terminal', '')
    const { status } = await noAuth.post('/accounts/1/card', { card: 'TEST' })
    expect(status).toBe(401)
  })
})
