interface WindowConfig {
  readonly BACKEND_URL: string
}

declare global {
  interface Window {
    config: WindowConfig
  }
}

export {}
