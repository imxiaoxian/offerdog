# 火山引擎实时语音转写 WebSocket 对接说明

后端已新增 `/ws/asr` 原生 WebSocket，负责把前端麦克风音频流转发到火山引擎 ASR，解析出的文字再实时推回前端。本文描述配置、消息格式及前端录音示例。

## 1. 后端配置

`src/main/resources/config/application.yml` 已追加默认配置，可通过环境变量覆盖：

```yaml
interview:
  audio:
    asr:
      enabled: true
      api-url: wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_async
      app-id: ${VOLC_ASR_APP_ID:}
      token: ${VOLC_ASR_TOKEN:}
      resource-id: volc.bigasr.sauc.duration
      model-name: bigmodel
      sample-rate: 16000
      bits: 16
      channel: 1
      enable-itn: true
      enable-punc: true
      enable-ddc: true
      show-utterances: true
      enable-nonstream: true
      segment-duration-ms: 200
```

> 部署时只需配置 `VOLC_ASR_APP_ID` 与 `VOLC_ASR_TOKEN`，其它参数保持默认即可。

## 2. 连接与消息格式

- 连接地址：`ws(s)://<host>/ws/asr`
- 认证：依赖后端持有的 APPID/token，无需前端透传 Key
- 建议流程：
  1. 建立 WebSocket 连接后发送一条文本 `{"type":"init","sessionId":"<业务会话ID>"}` 绑定会话。
  2. 按 200ms 左右将 **16kHz / 16bit / 单声道 PCM** 二进制分片推送。
  3. 录音结束发送文本 `"END"`（或 `{"type":"end"}`），后端会发送最后的 EOF 包。

后端返回的文本消息均为 JSON：

| type | 说明 | 示例字段 |
|---|---|---|
| `backend_connected` | 火山 ASR 通道建立成功 | `message` |
| `transcript` | 实时转写文本片段 | `sessionId`、`sequence`、`text`、`isFinal`、`timestamp` |
| `error` | 后端或火山异常 | `message` |

`transcript` 示例：

```json
{
  "type": "transcript",
  "sessionId": "5e879d8c-2d0b",
  "sequence": 3,
  "text": "我想应聘后端岗位，主要用 Java。",
  "isFinal": false,
  "timestamp": 1737095100123
}
```

`isFinal=true` 代表火山返回的最后一个包，可开始收尾。

## 3. 前端录音与发送示例（原生 WebSocket）

```ts
let ws: WebSocket | null = null;
let audioCtx: AudioContext | null = null;
let processor: ScriptProcessorNode | null = null;

async function startAsr(sessionId: string) {
  ws = new WebSocket(`${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/asr`);

  ws.onopen = () => {
    ws?.send(JSON.stringify({ type: 'init', sessionId }));
    console.log('asr connected');
  };
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === 'transcript') {
      // 渲染实时字幕
      console.log('[asr]', data.text, data.isFinal);
    }
  };

  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  audioCtx = new AudioContext({ sampleRate: 16000 });
  const source = audioCtx.createMediaStreamSource(stream);
  processor = audioCtx.createScriptProcessor(4096, 1, 1);

  processor.onaudioprocess = (e) => {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    const input = e.inputBuffer.getChannelData(0);
    const pcm16 = new Int16Array(input.length);
    for (let i = 0; i < input.length; i++) {
      const s = Math.max(-1, Math.min(1, input[i]));
      pcm16[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
    }
    ws.send(pcm16.buffer); // 二进制发送
  };

  source.connect(processor);
  processor.connect(audioCtx.destination);
}

function stopAsr() {
  ws?.send('END');
  ws?.close();
  processor?.disconnect();
  audioCtx?.close();
}
```

上面使用 `AudioContext` 将浏览器采样数据转为 16k/16bit PCM，每次 `onaudioprocess` 回调就近似 200ms 分片发送。

### 接收并渲染文字（核心）

```ts
// 建连时
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  switch (data.type) {
    case 'backend_connected':
      console.log('ASR backend ready');
      break;
    case 'transcript': {
      // data.text 里是实时转写片段
      // isFinal=false 时可直接追加；true 时标记这一句话结束
      const { text, isFinal, sequence, timestamp } = data;
      updateSubtitle(text, isFinal, sequence, timestamp);
      break;
    }
    case 'error':
      console.error('ASR error', data.message);
      showToast('语音识别异常，请重试');
      break;
    default:
      break;
  }
};

// 一个极简的拼接示例：把用户当前一句话显示在字幕区域
let buffer = '';
function updateSubtitle(text: string, isFinal: boolean) {
  buffer = text;
  render(buffer);
  if (isFinal) {
    commitToHistory(buffer); // 可选：把最终结果写入输入框或历史区
    buffer = '';
  }
}
```

要点：
- `type=transcript` 时，`text` 是火山返回的当前识别内容，`isFinal=true` 表示这一句话尾包（可在 UI 上把该行定格）。
- 可用 `sequence` 区分同一前端会话中的多轮录音，避免和历史识别串行混淆。
- 如果需要把实时字幕写入输入框，`isFinal=false` 时做“正在识别中”提示，`true` 时写回最终文本。

## 4. 完整链路回顾

前端麦克风 → 以 PCM 分片通过 `/ws/asr` 推给后端 → 后端用 OkHttp 持续推送到火山 ASR → 火山实时返回文字 → 后端以 `transcript` 事件推送给前端 → 前端渲染/拼接字幕。无需前端持有火山密钥，也无需改动现有 STOMP 配置。完成开发后即可把实时转写文字接入候选人作答输入框或字幕展示区域。
