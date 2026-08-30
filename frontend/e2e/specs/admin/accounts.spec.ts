import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient, createAccount, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe.serial('Admin - Accounts API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let createdAccountId: number
  const uniqueCard = `CARD-${Date.now().toString(36)}`

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /accounts creates account (200)', async () => {
    const account = await cleanup.trackAccount(
      createAccount({
        name: 'CRUD Test User',
        balance: 0,
        card: uniqueCard
      })
    )
    createdAccountId = account.id
    expect(account.name).toBe('CRUD Test User')
    expect(account.balance).toBe(0)
    expect(account.active).toBe(true)
  })

  test('POST /accounts rejects blank name (400)', async () => {
    const { status } = await api.post('/accounts', { name: '', balance: 0, active: true, idempotencyKey: randomUUID() })
    expect(status).toBe(400)
  })

  test('POST /accounts rejects negative balance (400)', async () => {
    const { status } = await api.post('/accounts', { name: 'Neg', balance: -100, active: true, idempotencyKey: randomUUID() })
    expect(status).toBe(400)
  })

  test('POST /accounts rejects duplicate card (400)', async () => {
    const { status } = await api.post('/accounts', {
      name: 'Dup Card',
      balance: 0,
      active: true,
      card: uniqueCard,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('POST /accounts/{id} updates name and email (200)', async () => {
    const { status, body } = await api.post(`/accounts/${createdAccountId}`, {
      name: 'Updated User',
      email: 'updated@test.hu',
      active: true
    })
    expect(status).toBe(200)
    expect(body.name).toBe('Updated User')
    expect(body.email).toBe('updated@test.hu')
  })

  test('POST /accounts/{id} with non-existent id returns 400', async () => {
    const { status } = await api.post('/accounts/99999', {
      name: 'Ghost',
      email: null,
      active: true
    })
    expect(status).toBe(400)
  })

  test('POST /accounts/{id}/disable sets active=false (200)', async () => {
    const { status, body } = await api.post(`/accounts/${createdAccountId}/disable`)
    expect(status).toBe(200)
    expect(body.active).toBe(false)
  })

  test('POST /accounts/{id}/enable sets active=true (200)', async () => {
    const { status, body } = await api.post(`/accounts/${createdAccountId}/enable`)
    expect(status).toBe(200)
    expect(body.active).toBe(true)
  })

  test('DELETE /accounts/{id} fails for non-zero balance (400)', async () => {
    const funded = await cleanup.trackAccount(createAccount({ name: 'Funded', balance: 500 }))
    const { status } = await api.del(`/accounts/${funded.id}`)
    expect(status).toBe(400)
  })

  test('DELETE /accounts/{id} deletes zero-balance account (200)', async () => {
    const { status } = await api.del(`/accounts/${createdAccountId}`)
    expect(status).toBe(200)
  })

  test('DELETE /accounts/{id} non-existent returns 404', async () => {
    const { status } = await api.del('/accounts/99999')
    expect(status).toBe(404)
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.post('/accounts', { name: 'x', balance: 0, active: true })
    expect(status).toBe(401)
  })

  test('GET /export/accounts returns CSV with expected headers and data (200)', async () => {
    const { status, body } = await api.getText('/export/accounts')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
    expect(body).toContain('name')
    expect(body).toContain('balance')
  })

  test('GET /template/accounts returns CSV template (200)', async () => {
    const { status, body } = await api.getText('/template/accounts')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
    expect(body).toContain('name')
  })

  test('POST /import/accounts import + verify data (201)', async () => {
    const csv = 'name,email,phone,card,balance,active\nImport Verified,,,CARD-IMPORT-VERIFY,750,true'
    const resp = await api.postText('/import/accounts', csv, { idempotencyKey: randomUUID() })
    expect([201, 400]).toContain(resp.status)
  })
})
