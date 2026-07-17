# 更新日志

## v1.0.1 - 2025-11-17 (Bug修复)

### 🐛 Bug修复

#### 1. 面试结束时未调用报告生成接口
**问题**: AI判断面试结束或用户手动结束面试时，没有调用后端的报告生成接口

**修复**:
- 添加 `generateReport` API方法
- 在面试结束时自动调用 `POST /interview/sessions/{sessionId}/report/generate`
- 确保报告生成任务被提交

**影响文件**:
- `src/services/interview.ts` - 添加generateReport方法
- `src/views/InterviewRoomPage.vue` - 在两处结束面试的地方调用生成接口

#### 2. 等待报告页面样式问题
**问题**: 报告生成期间，加载状态显示不够友好

**修复**:
- 优化加载状态UI，显示进度条
- 显示重试次数和进度
- 添加友好的提示文字
- 改进loading动画效果

**新功能**:
```
- 显示"正在生成面试报告..."标题
- 显示"AI正在分析你的面试表现，这可能需要30-60秒"提示
- 实时进度条（基于重试次数）
- 显示"已尝试 X / 12 次"
- 自动重试12次（约1分钟）
```

**影响文件**:
- `src/views/InterviewReportPage.vue` - 重构loading逻辑和UI

#### 3. 对话框不会自动滚动
**问题**: 新消息添加后，需要手动滚动才能看到

**修复**:
- 使用 Vue 的 `watch` 监听消息数组变化
- 监听AI输入状态变化
- 每次有新消息时自动滚动到底部
- 使用平滑滚动效果 (`behavior: 'smooth'`)

**技术细节**:
```typescript
// 监听消息变化
watch(() => messages.value.length, () => {
  scrollToBottom()
})

// 监听AI输入状态
watch(isAITyping, () => {
  if (isAITyping.value) {
    scrollToBottom()
  }
})
```

**影响文件**:
- `src/views/InterviewRoomPage.vue` - 添加watch监听，优化scrollToBottom函数

### 🎨 UI优化

1. **报告加载页面**
   - 更大的加载区域
   - 居中显示
   - 白色背景卡片
   - 圆角边框
   - 进度条动画

2. **对话滚动**
   - 平滑滚动动画
   - 自动触发
   - 性能优化

### 📝 代码优化

- 移除重复的 `scrollToBottom()` 调用
- 使用响应式的 `watch` 替代手动调用
- 优化报告加载重试逻辑
- 改进错误提示文案

---

## v1.0.0 - 2025-11-17 (首次发布)

### ✨ 新功能

#### 核心页面
- AI面试主页（模板选择和历史记录）
- 面试房间页面（实时对话）
- 面试报告页面（详细评估）

#### 技术实现
- 完整的TypeScript类型系统
- 规范的API服务封装
- WebSocket客户端支持
- Vite代理配置
- Session自动管理

#### 功能特性
- 面试模板展示
- 创建面试会话
- AI开场白
- 实时对话交互
- AI智能判断（追问/下一题/结束）
- 多维度评分报告
- 简历验证分析
- 录用建议

### 🔧 技术栈

- Vue 3 + TypeScript
- Ant Design Vue 4.x
- WebSocket (SockJS + STOMP)
- Axios
- Vue Router
- Pinia

### 📦 依赖

新增:
- `sockjs-client`: ^1.6.1
- `@stomp/stompjs`: ^7.0.0
- `@types/sockjs-client`: ^1.5.4

---

## 升级指南

### 从头开始

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
```

### 重启前端服务（应用代理配置）

如果之前已经运行，需要重启：

```bash
# 停止当前服务 (Ctrl+C)
# 然后重新启动
npm run dev
```

---

## 已知问题

### WebSocket流式推送
- 当前版本暂未启用WebSocket流式推送
- AI响应通过HTTP同步返回
- 如需打字机效果，可启用WebSocket代码

### 历史消息持久化
- 刷新页面会丢失对话历史
- 建议后端实现 `/messages` 接口
- 或使用localStorage缓存

---

## 路线图

### v1.1.0（计划中）
- [ ] 启用WebSocket流式AI响应
- [ ] 添加对话历史持久化
- [ ] 支持语音输入
- [ ] 优化移动端体验

### v1.2.0（计划中）
- [ ] 报告导出（PDF/Word）
- [ ] 数据可视化增强（图表）
- [ ] 面试回放功能
- [ ] 性能优化

---

**维护者**: AI面试团队  
**最后更新**: 2025-11-17

