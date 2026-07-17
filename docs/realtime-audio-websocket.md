# AI 面试官实时语音 WebSocket 对接说明

后端现在会在 **AI 面试官输出文本的同时** 自动触发 DashScope 实时语音合成，并把音频块通过 WebSocket 推送给前端。本文档说明推送格式以及前端如何解码播放。

## 1. 后端行为

- 触发点：`/interview/sessions/{sessionId}/opening` 与 `/interview/sessions/{sessionId}/answer` 每次返回面试官回复后立即触发。
- 语音模型：`qwen-tts-realtime`（默认 `voice=Chelsie`，`language=Chinese`），可在 `interview.audio.tts` 配置段调整。
- 音频格式：`PCM_24000HZ_MONO_16BIT`，每个消息携带 `audioFormat` 字段。
- 推送通道：`/topic/interview/audio/{sessionId}`（STOMP topic）。
- 推送事件：
  | type | 说明 | 关键字段 |
  |------|------|---------|
  | `START` | 某条 AI 文本即将播报 | `text`、`voice`、`audioFormat` |
  | `CHUNK` | PCM Base64 音频片段 | `chunkIndex`、`base64Audio` |
  | `COMPLETE` | 本条语音完成（或超时结束） | `message` |
  | `ERROR` | DashScope 或网络异常 | `message` |

## 2. 建立 WebSocket 连接

```ts
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const socket = new SockJS('/ws'); // 同域直接写相对路径，跨域需替换完整 URL
const stompClient = new Client({
  webSocketFactory: () => socket as any,
  reconnectDelay: 5000,
});

stompClient.onConnect = () => {
  stompClient.subscribe(`/topic/interview/audio/${sessionId}`, (frame) => {
    const payload = JSON.parse(frame.body);
    handleAudioEvent(payload);
  });
};

stompClient.activate();
```

## 3. 消息结构示例

### START

```json
{
  "sessionId": "5e879d8c-2d0b-4ba5-ba3d-7545c8b1595c",
  "sequenceNumber": 3,
  "type": "START",
  "voice": "Chelsie",
  "audioFormat": "PCM_24000HZ_MONO_16BIT",
  "text": "感谢你的回答，下面我想换个角度…",
  "message": "开始生成语音",
  "timestamp": 1736866862000
}
```

### CHUNK

```json
{
  "sessionId": "5e879d8c-2d0b-4ba5-ba3d-7545c8b1595c",
  "sequenceNumber": 3,
  "type": "CHUNK",
  "chunkIndex": 12,
  "audioFormat": "PCM_24000HZ_MONO_16BIT",
  "voice": "Chelsie",
  "base64Audio": "AAAAPz8/...",
  "timestamp": 1736866862055
}
```

### COMPLETE / ERROR

```json
{
  "sessionId": "5e879d8c-2d0b-4ba5-ba3d-7545c8b1595c",
  "sequenceNumber": 3,
  "type": "COMPLETE",
  "message": "语音生成完成",
  "timestamp": 1736866862300
}
```

## 4. 前端播放建议

1. 解析 Base64：`const pcm = Uint8Array.from(atob(base64Audio), c => c.charCodeAt(0));`
2. 转成 `AudioBuffer`（Web Audio API）：

```ts
const audioCtx = new AudioContext({ sampleRate: 24000 });
const buffer = audioCtx.createBuffer(1, pcm.length / 2, 24000);
const channelData = buffer.getChannelData(0);

for (let i = 0, offset = 0; i < channelData.length; i++, offset += 2) {
  const sample = (pcm[offset + 1] << 8) | pcm[offset];
  channelData[i] = sample / 0x8000;
}
```

3. 将每个 `CHUNK` 入队播放，`START` 可用于展示“AI 正在说话”，`COMPLETE`/`ERROR` 用于收尾。

> 如果前端暂未集成 STOMP，也可直接使用原生 `WebSocket` 连接 `/ws`（无 SockJS），只需遵循相同的订阅路径即可。

## 5. 调试技巧

- 建议同时输出 `sequenceNumber`，这样可以把语音播放与文本气泡对应起来。
- 若收到 `ERROR` 事件，可提示用户“语音生成失败”，但文本仍然可用。
- 如需关闭语音功能，可在 `application.yml` 中将 `interview.audio.tts.enabled` 置为 `false`。
