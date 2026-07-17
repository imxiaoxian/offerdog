// 开发环境使用代理，避免跨域和 Session Cookie 问题
const DEFAULT_DEV_API_BASE = '/api'
const DEFAULT_PROD_API_BASE = 'https://api.offerdog.com'
/** 与 vite 代理默认后端端口一致（Windows 上 8004-8103 常为系统保留，默认用 9080） */
const DEFAULT_DEV_WS_BASE = 'http://127.0.0.1:9080'
const DEFAULT_PROD_WS_BASE = 'https://api.offerdog.com'

const ABSOLUTE_URL_REGEX = /^https?:\/\//i

const customBase = import.meta.env.VITE_API_BASE_URL?.trim()
const customWsBase = import.meta.env.VITE_WS_BASE_URL?.trim()
const allowCrossOriginApi = import.meta.env.VITE_ALLOW_CROSS_ORIGIN_API === 'true'

const shouldForceProxyInDev =
  import.meta.env.DEV && !allowCrossOriginApi && (!customBase || ABSOLUTE_URL_REGEX.test(customBase))

if (shouldForceProxyInDev && customBase && ABSOLUTE_URL_REGEX.test(customBase)) {
  console.warn(
    '[env] 检测到在开发环境中使用绝对 API 地址，为避免跨域导致的 Session 丢失，已自动回退到 /api 代理。如需强制跨域，请设置 VITE_ALLOW_CROSS_ORIGIN_API=true',
  )
}

export const API_BASE_URL = shouldForceProxyInDev
  ? DEFAULT_DEV_API_BASE
  : customBase || (import.meta.env.DEV ? DEFAULT_DEV_API_BASE : DEFAULT_PROD_API_BASE)

// WebSocket 地址（不使用代理，直接连接）
const resolveWsBase = () => {
  if (customWsBase) return customWsBase

  // 如果 API 配置为相对路径，默认走同域的 /ws，方便通过反向代理复用 Cookie
  if (customBase) {
    return ABSOLUTE_URL_REGEX.test(customBase) ? customBase : ''
  }

  return import.meta.env.DEV ? DEFAULT_DEV_WS_BASE : DEFAULT_PROD_WS_BASE
}

export const WS_BASE_URL = resolveWsBase()
