# 面试报告异步生成 & WebSocket 对接说明

为了避免同步等待 AI 生成报告带来的长耗时，现在报告生成以异步任务的形式执行，并通过 WebSocket 主动推送进度/结果。本文档梳理完整流程、接口定义以及前端集成示例，方便尽快替换轮询逻辑，降低数据库压力。

## 0. 整体流程速览
1. 页面结束面试后调用 `POST /interview/sessions/{sessionId}/report/generate` 触发异步任务。
2. 前端建立 WebSocket 连接并订阅 `/topic/interview/report/{sessionId}`。
3. 后端会依次推送 `QUEUED → PROCESSING → SUCCESS/FAILED` 状态，收到 `SUCCESS` 后再调用 `GET /interview/sessions/{sessionId}/report` 拉取最终数据。
4. 若任务已存在或报告已生成，后端不会重复执行，而是直接推送当前状态（PROCESSING 或 SUCCESS），前端无需继续轮询。

## 1. 触发报告生成
- **HTTP 方法**: `POST`
- **URL**: `/interview/sessions/{sessionId}/report/generate`
- **路径参数**: `sessionId` 为当前面试会话 ID（UUID 字符串）
- **响应**:
  ```json
  {
    "success": true,
    "message": "报告生成任务已提交，完成后会通过WebSocket通知。"
  }
  ```
- **说明**: 接口立即返回，仅负责把任务投入后台线程池。成功返回只表示“任务已受理”，实际状态需通过 WebSocket 推送或轮询报告详情接口 `/interview/sessions/{sessionId}/report` 来确认。

## 2. WebSocket 连接
- **Endpoint**: `/ws`
- **协议**: STOMP over WebSocket，默认开启 SockJS fallback，可直接使用 `@stomp/stompjs` + `sockjs-client`。
- **跨域**: 当前配置允许所有 Origin，前端无需额外配置。

### 建议连接步骤
1. 创建 SockJS 实例：`const socket = new SockJS('/ws');`
2. 基于 SockJS 实例创建 STOMP client：`const client = Stomp.over(socket);`
3. 在建立订阅前，确保已经拿到 sessionId。

## 3. 订阅路径
- **Destination**: `/topic/interview/report/{sessionId}`
- 每个面试会话一个独立的 Topic，后端在报告生成成功或失败后推送消息。
- 如需额外校验，可在收到推送后比对 payload 中的 `userId`。

## 4. 推送消息结构
```json
{
  "sessionId": "a0fb4a5c-7c54-4d78-90de-2a0e3b3c2f5b",
  "reportId": "6cd3540a-0daf-498f-94de-7e8f007bf788",
  "userId": 9527,
  "status": "PROCESSING",
  "message": "报告生成完成",
  "timestamp": 1715161230123
}
```
### 状态说明
- `QUEUED`: 任务已写入虚拟线程池排队，通常是毫秒级瞬间状态，可用于展示“已提交”。
- `PROCESSING`: AI 正在生成或该会话已有任务正在执行。若用户重复点击“生成报告”，会直接收到此状态提示，不会重复触发。
- `SUCCESS`: 报告已入库，`reportId` 会返回最新版本，立即使用 `GET /interview/sessions/{sessionId}/report` 拉取即可。
- `FAILED`: 本轮生成失败，可提示用户稍后重试；`message` 会包含简要原因。

字段补充：
- `reportId`: 仅在 `SUCCESS`（包含“报告此前已存在”）时返回，前端可用作去重或日志记录。
- `timestamp`: 推送产生的服务器时间，毫秒值，可用于前端排序或重试判定。

## 5. 前端示例（TypeScript）
```ts
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const sessionId = 'a0fb4a5c-7c54-4d78-90de-2a0e3b3c2f5b';
const socket = new SockJS('/ws');
const client = new Client({ webSocketFactory: () => socket as any });

client.onConnect = () => {
  client.subscribe(`/topic/interview/report/${sessionId}`, (frame) => {
    const payload = JSON.parse(frame.body);
    switch (payload.status) {
      case 'QUEUED':
        console.log('报告任务排队中');
        break;
      case 'PROCESSING':
        console.log('报告生成中...');
        break;
      case 'SUCCESS':
        fetch(`/interview/sessions/${sessionId}/report`)
          .then((resp) => resp.json())
          .then((data) => {
            // 渲染报告
          });
        break;
      case 'FAILED':
        console.error('报告生成失败', payload.message);
        break;
    }
  });

  // 可在 onConnect 中立即触发生成任务
  fetch(`/interview/sessions/${sessionId}/report/generate`, { method: 'POST' });
};

client.activate();
```
> 若需要在断线后自动重连，可通过 `Client` 的 `reconnectDelay`、`heartbeatIncoming` 等配置项实现。

## 6. 失败及异常处理建议
- 收到 `FAILED` 时提示用户稍后再试，可提供重新触发按钮。重新触发会再次走 `QUEUED → PROCESSING`。
- 如果 30~60 秒内未收到 `SUCCESS`/`FAILED`，可以提示“生成中”并允许用户手动刷新（但无需高频轮询）。
- WebSocket 断开时，保留降级策略：单次去请求 `/interview/sessions/{sessionId}/report`，确保在推送失败时仍可拿到结果。
- 若重复点击按钮导致 `PROCESSING` 状态推送，说明后端已有任务在执行，直接保持 loading 即可。
