import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { WS_BASE_URL } from '@/config/env'
import type { WebSocketMessage } from '@/types/interview'

export type MessageCallback = (message: WebSocketMessage) => void

/**
 * WebSocket客户端管理类
 */
export class InterviewWebSocketClient {
  private client: Client | null = null
  private sessionId: string
  private callbacks: Map<string, MessageCallback[]> = new Map()
  private connected = false

  constructor(sessionId: string) {
    this.sessionId = sessionId
  }

  /**
   * 连接WebSocket
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      const wsUrl = WS_BASE_URL.replace(/^http/, 'ws')
      const socket = new SockJS(`${wsUrl}/ws/interview`)

      this.client = new Client({
        webSocketFactory: () => socket as WebSocket,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        onConnect: () => {
          console.log('WebSocket连接成功', this.sessionId)
          this.connected = true
          this.subscribeToTopics()
          resolve()
        },

        onStompError: (frame) => {
          console.error('WebSocket STOMP错误:', frame)
          this.connected = false
          reject(new Error('WebSocket连接失败'))
        },

        onWebSocketError: (event) => {
          console.error('WebSocket错误:', event)
          this.connected = false
          reject(new Error('WebSocket连接失败'))
        },

        onDisconnect: () => {
          console.log('WebSocket断开连接')
          this.connected = false
        },
      })

      this.client.activate()
    })
  }

  /**
   * 订阅所有相关的topic
   */
  private subscribeToTopics() {
    if (!this.client) return

    // 订阅进度更新
    this.client.subscribe(`/topic/interview/${this.sessionId}/progress`, (message: IMessage) => {
      this.handleMessage('progress', message)
    })

    // 订阅AI响应(流式)
    this.client.subscribe(`/topic/interview/${this.sessionId}/ai-response`, (message: IMessage) => {
      this.handleMessage('ai_response', message)
    })

    // 订阅AI响应完成
    this.client.subscribe(`/topic/interview/${this.sessionId}/ai-complete`, (message: IMessage) => {
      this.handleMessage('ai_complete', message)
    })

    // 订阅报告生成完成
    this.client.subscribe(`/topic/interview/${this.sessionId}/report-ready`, (message: IMessage) => {
      this.handleMessage('report_ready', message)
    })

    // 订阅错误消息
    this.client.subscribe(`/topic/interview/${this.sessionId}/error`, (message: IMessage) => {
      this.handleMessage('error', message)
    })
  }

  /**
   * 处理收到的消息
   */
  private handleMessage(type: WebSocketMessage['type'], message: IMessage) {
    try {
      const data = message.body ? JSON.parse(message.body) : message.body
      const wsMessage: WebSocketMessage = { type, data }

      // 调用注册的回调函数
      const callbacks = this.callbacks.get(type)
      if (callbacks) {
        callbacks.forEach((callback) => callback(wsMessage))
      }

      // 调用通用回调
      const allCallbacks = this.callbacks.get('*')
      if (allCallbacks) {
        allCallbacks.forEach((callback) => callback(wsMessage))
      }
    } catch (error) {
      console.error('解析WebSocket消息失败:', error)
    }
  }

  /**
   * 注册消息回调
   * @param type 消息类型，使用 '*' 接收所有消息
   * @param callback 回调函数
   */
  on(type: WebSocketMessage['type'] | '*', callback: MessageCallback) {
    if (!this.callbacks.has(type)) {
      this.callbacks.set(type, [])
    }
    this.callbacks.get(type)!.push(callback)
  }

  /**
   * 移除消息回调
   */
  off(type: WebSocketMessage['type'] | '*', callback: MessageCallback) {
    const callbacks = this.callbacks.get(type)
    if (callbacks) {
      const index = callbacks.indexOf(callback)
      if (index !== -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.connected = false
      this.callbacks.clear()
    }
  }

  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.connected
  }
}

