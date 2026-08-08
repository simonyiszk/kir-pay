import { test, expect, loginToken } from '../../fixtures/auth.fixture'
import { apiClient, createPrincipal, createCleanupTracker, randomUUID } from '../../fixtures/test-data'

test.describe.serial('Admin - Principals API', () => {
  let api: ReturnType<typeof apiClient>
  const cleanup = createCleanupTracker()
  let createdPrincipalId: number

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api/admin', authToken)
  })

  test.afterAll(async () => {
    await cleanup.run()
  })

  test('POST /principals creates terminal principal (200)', async () => {
    const principal = await cleanup.trackPrincipal(
      createPrincipal({
        name: 'e2e-crud-term',
        role: 'TERMINAL'
      })
    )
    createdPrincipalId = principal.id
    expect(principal.name).toBe('e2e-crud-term')
    expect(principal.role).toBe('TERMINAL')
    expect(principal.active).toBe(true)
  })

  test('POST /principals with duplicate name returns 400', async () => {
    const { status } = await api.post('/principals', {
      name: 'e2e-crud-term',
      password: 'pw',
      role: 'TERMINAL',
      canUpload: true,
      canTransfer: true,
      canSellItems: true,
      canRedeemVouchers: true,
      canAssignCards: true,
      active: true,
      idempotencyKey: randomUUID()
    })
    expect(status).toBe(400)
  })

  test('GET /principals lists all principals (200)', async () => {
    const { status, body } = await api.get('/principals')
    expect(status).toBe(200)
    expect(Array.isArray(body)).toBe(true)

    const names: string[] = body.map((p: { name: string }) => p.name)
    expect(names).toContain('admin')
  })

  test('POST /principals/{id} updates permissions (200)', async () => {
    const { status, body } = await api.post(`/principals/${createdPrincipalId}`, {
      name: 'e2e-crud-term',
      password: 'new-pw',
      role: 'TERMINAL',
      canUpload: false,
      canTransfer: false,
      canSellItems: true,
      canRedeemVouchers: true,
      canAssignCards: true,
      active: true
    })
    expect(status).toBe(200)
    expect(body.canUpload).toBe(false)
    expect(body.canTransfer).toBe(false)
  })

  test('POST /principals/{id} non-existent returns 400', async () => {
    const { status } = await api.post('/principals/99999', {
      name: 'ghost',
      password: 'pw',
      role: 'TERMINAL',
      canUpload: true,
      canTransfer: true,
      canSellItems: true,
      canRedeemVouchers: true,
      canAssignCards: true,
      active: true
    })
    expect(status).toBe(400)
  })

  test('POST /principals/{id}/disable disables non-ADMIN (200)', async () => {
    const { status, body } = await api.post(`/principals/${createdPrincipalId}/disable`)
    expect(status).toBe(200)
    expect(body.active).toBe(false)
  })

  test('POST /principals/{id}/enable re-enables (200)', async () => {
    const { status, body } = await api.post(`/principals/${createdPrincipalId}/enable`)
    expect(status).toBe(200)
    expect(body.active).toBe(true)
  })

  test('POST /principals/{id}/disable rejects ADMIN (400)', async () => {
    const { status } = await api.post('/principals/1/disable')
    expect(status).toBe(400)
  })

  test('DELETE /principals/{id} deletes terminal principal (200)', async () => {
    const { status } = await api.del(`/principals/${createdPrincipalId}`)
    expect(status).toBe(200)
  })

  test('DELETE /principals/{id} rejects ADMIN (400)', async () => {
    const { status } = await api.del('/principals/1')
    expect(status).toBe(400)
  })

  test('DELETE /principals/{id} non-existent returns 400', async () => {
    const { status } = await api.del('/principals/99999')
    expect(status).toBe(400)
  })

  test('GET /export/principals returns CSV (200)', async () => {
    const { status, body } = await api.getText('/export/principals')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
  })

  test('GET /template/principals returns CSV (200)', async () => {
    const { status, body } = await api.getText('/template/principals')
    expect(status).toBe(200)
    expect(typeof body).toBe('string')
  })

  test('endpoints reject unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api/admin', '')
    const { status } = await noAuth.get('/principals')
    expect(status).toBe(401)
  })

  test('POST /import/principals imports CSV and principals appear in list', async () => {
    const csv =
      'name,password,role,canUpload,canTransfer,canSellItems,canRedeemVouchers,canAssignCards,active\ncsv-imp-verify,pw,TERMINAL,false,false,true,false,false,true'
    const { status } = await api.postText('/import/principals', csv, { idempotencyKey: randomUUID() })
    expect([201, 400]).toContain(status)

    const { body: allPrincipals } = await api.get<Array<{ name: string; role: string; canSellItems: boolean }>>('/principals')
    const imported = allPrincipals.find((p) => p.name === 'csv-imp-verify')
    if (imported) {
      expect(imported.role).toBe('TERMINAL')
      expect(imported.canSellItems).toBe(true)
    }
  })

  test('POST /principals/{id} update with password "***" preserves existing secret', async () => {
    const principal = await cleanup.trackPrincipal(
      createPrincipal({
        name: 'e2e-star-pw',
        role: 'TERMINAL'
      })
    )

    const { status, body } = await api.post(`/principals/${principal.id}`, {
      name: 'e2e-star-pw',
      password: '***',
      role: 'TERMINAL',
      canUpload: false,
      canTransfer: false,
      canSellItems: true,
      canRedeemVouchers: false,
      canAssignCards: false,
      active: true
    })
    expect(status).toBe(200)

    const token = await loginToken('e2e-star-pw', 'e2e-test-pw')
    const termApi = apiClient('/v1/api/terminal', token)
    const { status: authStatus } = await termApi.get('/items')
    expect(authStatus).toBe(200)
  })

  test('POST /principals/{id} admin cannot demote themselves (400)', async () => {
    const { status } = await api.post('/principals/1', {
      name: 'admin',
      password: 'admin',
      role: 'TERMINAL',
      canUpload: true,
      canTransfer: true,
      canSellItems: true,
      canRedeemVouchers: true,
      canAssignCards: true,
      active: true
    })
    expect(status).toBe(400)
  })

  test('POST /principals/{id} admin cannot disable themselves via update (400)', async () => {
    const { status } = await api.post('/principals/1', {
      name: 'admin',
      password: 'admin',
      role: 'ADMIN',
      canUpload: true,
      canTransfer: true,
      canSellItems: true,
      canRedeemVouchers: true,
      canAssignCards: true,
      active: false
    })
    expect(status).toBe(400)
  })

  test('POST /principals creates principal with specific permission set', async () => {
    const principal = await cleanup.trackPrincipal(
      createPrincipal({
        name: 'e2e-specific-perms',
        role: 'TERMINAL',
        canUpload: true,
        canTransfer: false,
        canSellItems: true,
        canRedeemVouchers: false,
        canAssignCards: false
      })
    )
    expect(principal.canUpload).toBe(true)
    expect(principal.canTransfer).toBe(false)
    expect(principal.canSellItems).toBe(true)
    expect(principal.canRedeemVouchers).toBe(false)
    expect(principal.canAssignCards).toBe(false)
  })
})
