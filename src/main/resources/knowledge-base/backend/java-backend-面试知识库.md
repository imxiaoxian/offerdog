# Java 后端工程师 — RAG 知识库

## 岗位定义（JD 口径）
面向企业服务端与业务中台开发：用 Java（常见为 8/11/17/21）与 Spring 生态实现高可用、可观测、可演进的 HTTP/RPC 服务。典型边界：与产品/前端对齐接口契约，与 DBA/中间件协作做容量与稳定性，对线上问题能定位到代码、SQL、依赖与基础设施层。

## 核心技术栈（面试常问范围）
- 语言与运行时：集合与并发（JUC）、JVM 内存模型与 GC、类加载、异常与泛型。
- 框架：Spring Boot、Spring MVC/WebFlux、Spring Transaction、Spring Data/JPA 或 MyBatis。
- 数据：MySQL/PostgreSQL（索引、事务隔离、锁、执行计划）、Redis（数据结构、过期、持久化、集群拓扑）。
- 消息与集成：Kafka/RocketMQ（消费语义、顺序、重试、幂等）。
- 分布式：CAP 直觉、限流熔断、分布式 ID、缓存一致性、最终一致（Outbox/Saga 等概念层）。
- 工程化：Maven/Gradle、单元与集成测试、日志规范、配置中心、CI/CD 基础。

## 集合框架与 ArrayList（面试高频）
- **Collection / Map 两支**：`Collection` 系（`List` / `Set` / `Queue`）表示单列元素；`Map` 表示键值对。`ArrayList` 实现 `List`，底层是 **可扩容的 Object 数组**（`elementData`），**随机访问 O(1)**，尾部 `add` 均摊 **O(1)**，中间插入/删除需搬移元素为 **O(n)**。
- **扩容（JDK8+）**：默认空数组，首次添加常扩到 **10**；之后容量不足时按 **`newCapacity = old + (old >> 1)`（约 1.5 倍）** 增长，用 `Arrays.copyOf` 拷贝。**批量添加前**可用 `ensureCapacity` 或 `new ArrayList<>(expectedSize)` 减少扩容次数；元素稳定后可用 `trimToSize()` 释放多余槽位。
- **与 LinkedList**：链表按节点插入删除在已知节点附近为 O(1)，但 **按索引 get 为 O(n)**；**读多写少、下标访问多**优先 `ArrayList`；**头尾频繁插入**可考虑 `LinkedList` 或 **`ArrayDeque`**（双端队列场景）。
- **fail-fast**：`ArrayList` 迭代器记录 `expectedModCount`，结构修改使 `modCount` 变化，再迭代会 **`ConcurrentModificationException`**。安全删除用 **`Iterator.remove()`**、`removeIf`，或并发场景用并发集合。`subList` 是**原列表视图**，不是独立拷贝，母列表与子列表结构性修改会相互影响（需按文档约定使用）。
- **线程安全**：`ArrayList` **非线程安全**。替代：`Collections.synchronizedList`、`CopyOnWriteArrayList`（读多写少、写时复制数组）、或业务层显式锁。`Vector` 一般不推荐新业务。
- **equals / hashCode**：作 `HashMap` 键的对象需遵守契约；`List` 的 `equals` 按元素顺序与内容比较。

## 高频考点清单
- Spring：IOC/AOP、循环依赖与三级缓存、事务传播与失效场景、Bean 生命周期。
- 并发：线程池参数含义、synchronized vs Lock、volatile、CAS、AQS 直觉。
- JVM：堆栈、GC 算法直觉、常见 OOM、CPU 飙高与死锁排查步骤。
- MySQL：B+ 树与最左前缀、覆盖索引、回表、MVCC 与幻读、慢 SQL 优化路径。
- Redis：缓存穿透/击穿/雪崩、热 key、大 key、与 DB 的一致性策略（旁路缓存、延迟双删的利弊）。
- 接口设计：幂等键、版本化、错误码、分页与深分页、鉴权（JWT/Session 取舍）。

## 优秀回答结构（STAR + 技术纵深）
Situation：业务背景、流量量级、SLA。Task：你负责的目标与约束。Action：方案对比（2 个即可）、关键代码/配置、为什么选它。Result：指标前后（QPS、P99、错误率）、复盘与遗留风险。避免背诵定义；每个论点带一个真实项目锚点。

## 范例 A：谈谈你如何设计接口幂等
要点范例：为写操作引入业务唯一键（如订单号+操作类型）或幂等 token，在 DB 唯一索引或 Redis SETNX 上做门禁；配合状态机只允许合法迁移；回调场景用「处理中」占位防重；日志带 traceId 便于对账。说明失败重试时如何保证不会重复记账。

## 范例 B：线上 P99 升高如何排查
要点范例：先看网关与应用黄金指标 → 采样链路追踪定位慢 span → 检查外部依赖（DB/Redis/MQ）与连接池 → 线程栈与 GC 日志 → 近期发布与配置变更 → 止血（降级/限流/扩容）与根因修复（SQL、索引、批量、缓存）→ 复盘监控缺口。

## 项目深挖常见追问
- 你在该模块中的**个人**产出是什么（而非团队）？
- 关键接口的容量如何验证（压测模型、数据、结论）？
- 最难的 bug 或事故：时间线、根因、永久修复是什么？
- 若重做一遍，你会在哪个决策上改变？

## 行为题取向（与后端协作强相关）
- 需求频繁变更：如何固化范围、拆分里程碑、管理干系人预期。
- 与前端/测试意见不一致：如何用数据与原型对齐接口契约。
- 技术债：如何量化影响、排期偿还、避免「永远不还」。
