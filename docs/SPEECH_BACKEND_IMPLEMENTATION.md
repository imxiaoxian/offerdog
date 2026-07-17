# 后端语音识别实现指引

本文档提供后端实现方案，帮助前端安全地将用户语音提交给后端，再由后端携带 Gemini API Key 调用 Google Gemini 语音识别接口。重点解决：

- API Key 保存在后端，前端不直接接触
- 前端将音频数据传给后端的方式
- 支持 Gemini 流式返回以缩短首字级响应

## 1. 总体流程

1. 前端 `MediaRecorder` 录音 → 生成 `audio/webm` Blob
2. 前端把音频 Blob 以 `multipart/form-data` 上传到后端 `/speech/transcribe`，并带上 `sessionId、language、chunkIndex、isLastChunk` 等元数据
3. 后端验证身份后将音频转换为 Base64，使用保存在服务器环境变量的 `GEMINI_API_KEY` 调用 Gemini（推荐 `gemini-2.0-flash` 或 `gemini-1.5-flash-latest`）
4. 后端将识别结果（流式或一次性）返回给前端；同时可写入持久化存储
5. 前端收到文字后触发 `onTranscript`，同步本地 UI

## 2. HTTP 接口约定

| Endpoint | Method | 说明 |
| --- | --- | --- |
| `/speech/session/create` | POST | 前端开启录音时调用，后端生成 `sessionId` 并返回 |
| `/speech/transcribe` | POST (multipart) | 前端上传音频分片，后端负责调用 Gemini 并返回识别结果 |
| `/speech/transcribe/stream` | POST (multipart, SSE response) | （可选）与上面请求体一致，但通过 Server-Sent Events 把 Gemini 的流式文本直接推给前端 |
| `/speech/session/end` | POST | 录音结束或超时后释放会话资源 |

### `/speech/transcribe` 请求示例

```
Content-Type: multipart/form-data

audio=<binary webm file>
sessionId=abcd-1234
language=zh-CN
chunkIndex=0
isLastChunk=true
stream=false
```

响应：

```json
{
  "text": "我需要打开计算器。",
  "latencyMs": 1680,
  "chunkIndex": 0,
  "isLastChunk": true
}
```

### `/speech/transcribe/stream` (SSE) 响应示例

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache

data: {"text":"我", "isFinal":false}

data: {"text":"我需要", "isFinal":false}

data: {"text":"我需要打开计算器。", "isFinal":true}
```

前端使用 `EventSource` 或 `ReadableStream` 订阅该 SSE，逐条更新字幕。

## 3. 后端实现建议

### 3.1 环境变量

```
GEMINI_API_KEY=your_prod_key
GEMINI_MODEL=gemini-2.0-flash
```

不要将 Key 写入代码仓库。部署平台可通过密钥管理（Vault、KMS、Secrets Manager）注入。

### 3.2 Node/Express 伪代码

```ts
import express from 'express'
import multer from 'multer'
import { GoogleGenerativeAI } from '@google/generative-ai'

const upload = multer()
const router = express.Router()
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY!)

router.post('/speech/transcribe', upload.single('audio'), async (req, res, next) => {
  try {
    const audioBuffer = req.file?.buffer
    if (!audioBuffer) {
      return res.status(400).json({ message: 'audio file missing' })
    }

    const base64Audio = audioBuffer.toString('base64')
    const model = genAI.getGenerativeModel({ model: process.env.GEMINI_MODEL ?? 'gemini-2.0-flash' })

    const request = [
      {
        inlineData: {
          mimeType: req.file.mimetype ?? 'audio/webm',
          data: base64Audio,
        },
      },
      {
        text: `请将这段音频转换为${req.body.language ?? 'zh-CN'}文字，只返回文字。`,
      },
    ]

    const result = await model.generateContent(request)
    const text = result.response.text().trim()
    res.json({ text, chunkIndex: Number(req.body.chunkIndex ?? 0) })
  } catch (error) {
    next(error)
  }
})
```

### 3.3 流式返回

```ts
router.post('/speech/transcribe/stream', upload.single('audio'), async (req, res, next) => {
  res.setHeader('Content-Type', 'text/event-stream')
  res.setHeader('Cache-Control', 'no-cache, no-transform')
  res.setHeader('Connection', 'keep-alive')

  try {
    const base64Audio = req.file!.buffer.toString('base64')
    const model = genAI.getGenerativeModel({ model: 'gemini-2.0-flash' })
    const streamResult = await model.generateContentStream([
      { inlineData: { mimeType: 'audio/webm', data: base64Audio } },
      { text: `请以${req.body.language ?? 'zh-CN'}返回识别文字。` },
    ])

    for await (const chunk of streamResult.stream) {
      const text = chunk.text()
      if (text) {
        res.write(`data: ${JSON.stringify({ text, isFinal: false })}\n\n`)
      }
    }

    res.write(`data: ${JSON.stringify({ isFinal: true })}\n\n`)
    res.end()
  } catch (error) {
    res.write(`data: ${JSON.stringify({ error: 'stream_failed' })}\n\n`)
    res.end()
    next(error)
  }
})
```

Gemini 官方 SDK 以 `generateContentStream` 或 REST `streamGenerateContent` 形式返回可迭代对象，后端逐条写入 SSE。前端只需监听 `message` 事件即可同步渲染。

## 4. 前端如何上传音频

Composable 中 `processAudio` 可以在 `sendToBackend` 为 `true` 时执行以下逻辑：

```ts
const formData = new FormData()
formData.append('audio', audioBlob, 'speech.webm')
formData.append('sessionId', sessionId ?? '')
formData.append('language', options.language ?? 'zh-CN')
formData.append('chunkIndex', chunkIndex.toString())
formData.append('isLastChunk', 'true')
formData.append('stream', 'false')

await speechApi.uploadAudio(formData)
```

`speechApi.uploadAudio` → `http.post('/speech/transcribe', formData, { headers: { 'Content-Type': 'multipart/form-data' } })`

如果启用流式，前端可以：

```ts
const eventSource = new EventSourcePolyfill('/speech/transcribe/stream?...', { withCredentials: true })
eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data)
  updateTranscript(data.text, data.isFinal)
}
```

## 5. 性能与体验优化

- **小分片上传**：MediaRecorder 支持 `timeslice`，例如每 2～3 秒发送一次 `audio/webm`，后台可以并行处理，缩短尾部延迟。
- **SSE/ReadableStream**：使用 `generateContentStream` 并透传 SSE，可在几百毫秒内获得首个 token。
- **重试 & 超时**：Gemini 偶尔会 429，后台可按指数退避重试 1～2 次；同时给每个会话加上 idle timeout。
- **缓存上下文**：同一个 sessionId 可以缓存最近一次识别结果，在流式补全时回传，以便前端做差量展示。
- **安全**：验证 `sessionId + 用户身份`，限制单会话大小（例如 10 MB）和请求频率，避免被滥用。

## 6. 交付物清单

1. 新增上传接口 `/speech/transcribe`（同步返回文本）
2. （可选）新增流式接口 `/speech/transcribe/stream`（SSE）
3. 会话管理（创建/结束/超时）
4. 服务端集成 Gemini SDK 并读取 `GEMINI_API_KEY`
5. 日志与监控：记录请求时间、流式耗时、Gemini 错误码
6. 更新前端 `speechApi`，将音频 Blob 转发到后端

完成以上步骤后，前端即可在 `sendToBackend` 模式下安全地把语音传给后端，由后端负责调用 Gemini 并把识别文本（或流式 token）返回。