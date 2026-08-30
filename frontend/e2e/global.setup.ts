import { FullConfig } from '@playwright/test'

async function globalSetup(_config: FullConfig) {
  const baseUrl = process.env.CI ? 'http://localhost:8001' : 'http://localhost:8001'

  const maxRetries = 30
  for (let i = 0; i < maxRetries; i++) {
    try {
      const res = await fetch(`${baseUrl}/v1/api/app`)
      if (res.status < 500) {
        console.log('Backend is healthy')
        return
      }
    } catch (e) {
      console.error('Fetching app data failed', e)
    }
    await new Promise((r) => setTimeout(r, 2000))
  }
  throw new Error('Backend did not become healthy in time')
}

export default globalSetup
