import { test, expect } from '../../fixtures/auth.fixture'
import { apiClient } from '../../fixtures/test-data'

test.describe('Terminal - App API', () => {
  let api: ReturnType<typeof apiClient>

  test.beforeAll(async ({ authToken }) => {
    api = apiClient('/v1/api', authToken)
  })

  test('GET /app returns AppConfig and PrincipalResponse (200)', async () => {
    const { status, body } = await api.get<{
      config: Record<string, unknown>
      principal: Record<string, unknown>
    }>('/app')

    expect(status).toBe(200)
    expect(body.config).toBeDefined()
    expect(body.principal).toBeDefined()
    expect(typeof body.principal.name).toBe('string')
    expect(body.principal.role).toBe('ADMIN')
  })

  test('GET /app rejects unauthenticated requests (401)', async () => {
    const noAuth = apiClient('/v1/api', '')
    const { status } = await noAuth.get('/app')
    expect(status).toBe(401)
  })
})
