import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const rootDir = fileURLToPath(new URL('.', import.meta.url))

const resolveBackendUrl = (mode: string) => {
  const env = loadEnv(mode, rootDir, '')
  const raw = env.VITE_DEV_BACKEND_URL?.trim() || 'http://127.0.0.1:9080'
  return raw.replace(/\/$/, '')
}

const toWsUrl = (httpUrl: string) => httpUrl.replace(/^http/i, 'ws')

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, rootDir, '')
  const tunnelHost = env.VITE_DEV_TUNNEL_HOST?.trim().replace(/^https?:\/\//i, '').replace(/\/$/, '') || ''
  const API_PROXY_TARGET = resolveBackendUrl(mode)
  const WS_PROXY_TARGET = toWsUrl(API_PROXY_TARGET)
  const isDev = mode === 'development'

  const createApiProxy = (): ProxyOptions => ({
    target: API_PROXY_TARGET,
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, ''),
    secure: false,
    configure: (proxy, options) => {
      proxy.on('proxyReq', (_, req) => {
        if (!isDev) return

        const requestUrl = req.url ?? ''
        const target = typeof options.target === 'string' ? options.target : API_PROXY_TARGET
        console.log('🔄 代理请求:', req.method, requestUrl)
        console.log('📤 转发到:', `${target}${requestUrl}`)
      })
    },
  })

  const createWsProxy = (): ProxyOptions => ({
    target: WS_PROXY_TARGET,
    changeOrigin: true,
    ws: true,
    secure: false,
  })

  const createPublicProxy = (): ProxyOptions => ({
    target: API_PROXY_TARGET,
    changeOrigin: true,
    secure: false,
  })

  return {
    plugins: [vue(), vueDevTools()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      // 默认可从局域网访问（手机同 WiFi）；仅本机调试可改 host: 'localhost'
      host: true,
      // Cloudflare Quick Tunnel 的 Host 为 *.trycloudflare.com，需放行否则 Vite 拒绝请求
      allowedHosts: ['.trycloudflare.com', '.loca.lt'],
      port: 5173,
      // 经 localtunnel/ngrok 用 https 打开时，必须让 HMR 走隧道域名，否则外网浏览器会连 localhost 失败
      ...(tunnelHost
        ? {
            origin: `https://${tunnelHost}`,
            hmr: {
              protocol: 'wss' as const,
              host: tunnelHost,
              clientPort: 443,
            },
          }
        : {}),
      proxy: {
        '/api': createApiProxy(),
        '/ws': createWsProxy(),
        '/public': createPublicProxy(),
      },
    },
  }
})
