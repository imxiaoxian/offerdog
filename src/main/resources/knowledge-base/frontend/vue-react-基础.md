# 前端工程与 Vue/React 要点

## 组件与状态
- 组件应单一职责，可组合；复杂页面拆分为容器组件与展示组件。
- 本地状态用 `ref`/`reactive`（Vue）或 `useState`（React）；跨层共享用 Pinia/Redux/Zustand，避免 prop drilling 过深。

## 性能
- 列表渲染使用稳定 `key`；大列表做虚拟滚动（如 `vue-virtual-scroller`、TanStack Virtual）。
- 路由级代码分割：`import()` 懒加载页面，减小首包体积。

## 工程化
- 环境变量区分 `development`/`production`，敏感配置不进仓库。
- ESLint + Prettier 统一风格；提交前可用 lint-staged 限制改动范围。
