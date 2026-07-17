# 后端与 Spring / REST 要点

## 分层与边界
- Controller 只做参数校验与编排；业务在 Service；持久化在 Repository/Mapper。
- DTO 与领域模型分离，避免把实体直接暴露给 HTTP 层。

## 事务与一致性
- 多表写操作用 `@Transactional`，注意传播行为与只读事务。
- 幂等：支付、回调等接口用业务唯一键或 token 防重。

## 性能与可靠性
- 热点数据用缓存（Redis），注意 TTL 与缓存击穿/穿透（空值短缓存、互斥锁）。
- 数据库连接池、慢查询监控；分页查询避免 `OFFSET` 过大，深分页可用 seek（cursor）。
