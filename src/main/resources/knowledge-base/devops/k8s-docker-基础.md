# 运维与容器 / K8s 要点

## 容器
- 镜像多阶段构建减小体积；非 root 用户运行进程。
- 健康检查：`liveness` 与 `readiness` 分离，避免未就绪流量切入。

## Kubernetes
- Deployment 设置 `resources.requests/limits`；HPA 依据 CPU/自定义指标扩缩。
- ConfigMap/Secret 管理配置；敏感信息用 Secret + 卷挂载或外部密钥管理。

## 可观测性
- 日志结构化（JSON）+ 统一 traceId；指标（RED/USE）与告警阈值。
