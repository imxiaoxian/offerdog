-- 第二批额外官方面试题（幂等：同 bank + 相同题干则不重复插入）。
-- 依赖：sql/seed_categories_offerdog.sql、各二级岗位「{岗位名} 题库」已存在。

BEGIN;

WITH job_categories AS (
  SELECT c.id AS category_id, c.name AS job_name, p.name AS bucket_name
  FROM categories c
  JOIN categories p ON p.id = c.parent_id
  WHERE c.deleted_at IS NULL AND c.level = 2 AND p.deleted_at IS NULL
),
banks AS (
  SELECT b.id AS bank_id, b.category_id, b.name AS bank_name
  FROM question_banks b
  WHERE b.deleted_at IS NULL
),
spec AS (
  SELECT * FROM (VALUES
    -- Java / Spring
    ('后端','Java','【Java后端｜技术知识】Spring 中 @Autowired 注入失败常见原因有哪些？@Qualifier 与 @Primary 如何配合？',
     '多实现类未指定限定符、循环依赖、Bean 未注册、泛型擦除导致类型不匹配等；@Qualifier 指定 bean 名；@Primary 设置默认实现。',
     '结合一个「两个 DataSource」或「两个 Parser 实现」的例子。', 'medium'::question_difficulty, ARRAY['技术知识','Spring','IoC']::text[]),
    ('后端','Java','【Java后端｜技术知识】Java 中强引用、软引用、弱引用、虚引用各适用于什么场景？',
     '强：默认；软：内存紧张可回收，适合缓存；弱：GC 即回收，WeakHashMap；虚：跟踪对象回收，需 ReferenceQueue。',
     '说明与内存泄漏排查的关系。', 'hard'::question_difficulty, ARRAY['技术知识','JVM','引用']::text[]),
    ('后端','Java','【Java后端｜场景题】线上 Full GC 频繁，你会按什么顺序排查？',
     '先看 GC 日志与指标 -> 堆 dump 看大对象与泄漏 -> 代码路径（集合增长、未关闭资源、缓存过大）-> 调优参数与容量 -> 验证发布。',
     '强调先数据后猜测。', 'hard'::question_difficulty, ARRAY['场景题','JVM','GC']::text[]),

    ('后端','Python','【Python后端｜技术知识】Python GIL 对 CPU 密集与 IO 密集任务分别有什么影响？如何绕过？',
     'GIL 限制多线程并行执行字节码，CPU 密集多线程难提速；IO 密集可因释放 GIL 受益。绕过：多进程、C 扩展、asyncio（单线程协作式）。',
     '勿绝对说「Python 不能并发」。', 'medium'::question_difficulty, ARRAY['技术知识','Python','并发']::text[]),
    ('后端','Python','【Python后端｜场景题】用 FastAPI / Django 做接口时，如何做限流与超时保护下游依赖？',
     '网关或中间件限流（令牌桶/漏桶）、熔断降级、async 超时 cancel、连接池与重试退避；监控 429/5xx 与 P99。',
     '提到幂等与重试风暴风险。', 'medium'::question_difficulty, ARRAY['场景题','Python','稳定性']::text[]),

    ('后端','C++','【C++｜技术知识】std::unique_ptr 与 std::shared_ptr 的所有权语义与典型误用？',
     'unique 独占移动语义；shared 引用计数，注意循环引用需 weak_ptr；不要从裸指针重复构造多个 shared_ptr。',
     '可提 RAII 与异常安全。', 'medium'::question_difficulty, ARRAY['技术知识','C++','智能指针']::text[]),

    ('后端','数据库','【数据库｜技术知识】什么是覆盖索引？如何利用它减少回表？',
     '索引列已包含查询所需字段则无需回表；explain 看 Using index；联合索引列顺序影响是否能覆盖。',
     '对比「只查主键」与「select *」。', 'medium'::question_difficulty, ARRAY['技术知识','MySQL','索引']::text[]),
    ('后端','数据库','【数据库｜场景题】慢查询已定位到未走索引，除加索引外还有哪些手段？',
     '改写 SQL（避免函数包列、隐式转换）、分页优化、归档冷热数据、读写分离、物化视图/汇总表、执行计划 hint（慎用）。',
     '强调统计信息与直方图更新。', 'medium'::question_difficulty, ARRAY['场景题','MySQL','优化']::text[]),

    ('后端','中间件','【中间件｜技术知识】Kafka 中 partition、consumer group、rebalance 的关系？',
     'partition 是并行与有序边界；同组内消费者瓜分 partition；rebalance 在成员变化或订阅变化时重新分配，可能造成停顿。',
     '提到 sticky assignor 可减少迁移。', 'medium'::question_difficulty, ARRAY['技术知识','Kafka','消息']::text[]),
    ('后端','中间件','【中间件｜场景题】消息队列如何保证「至少一次」投递下的消费幂等？',
     '业务幂等键、去重表、状态机只允许合法跳转、版本号/乐观锁；消费者批处理与重试隔离；死信队列人工兜底。',
     '说明「精确一次」在端到端的代价。', 'hard'::question_difficulty, ARRAY['场景题','Kafka','幂等']::text[]),

    ('后端','分布式','【分布式｜技术知识】CAP 如何理解？BASE 与 ACID 的取舍在工程中如何体现？',
     'CAP：一致性、可用性、分区容错不可三角全得；BASE：基本可用、软状态、最终一致。工程上多选 AP+最终一致，关键路径做强一致。',
     '避免背定义不举实例。', 'medium'::question_difficulty, ARRAY['技术知识','分布式','CAP']::text[]),

    -- 前端
    ('前端','React','【Web前端｜技术知识】React 18 并发特性（Concurrent Rendering）大致解决什么问题？useTransition 的典型用途？',
     '可中断渲染避免主线程长时间阻塞；useTransition 标记非紧急更新，保持输入响应；与 Suspense 配合数据流。',
     '对比 startTransition 与 debounce。', 'medium'::question_difficulty, ARRAY['技术知识','React','并发']::text[]),
    ('前端','Vue','【Web前端｜技术知识】Vue 3 编译器对静态提升、补丁标记（patch flag）做了什么优化？',
     '静态节点 hoist 减少比对；patch flag 标记动态类型，运行时只比对必要部分；提升更新性能。',
     '说明与手写 render 的性能差异来源。', 'hard'::question_difficulty, ARRAY['技术知识','Vue','编译优化']::text[]),
    ('前端','工程化','【Web前端｜场景题】Monorepo 下如何管理多包的版本发布与依赖？',
     'pnpm workspace / changesets / lerna；统一构建与缓存；语义化版本与变更日志；CI 中按 affected 构建。',
     '提到幽灵依赖与 hoisting 风险。', 'medium'::question_difficulty, ARRAY['场景题','工程化','Monorepo']::text[]),
    ('前端','性能优化','【Web前端｜技术知识】Core Web Vitals 中 LCP、FID/INP、CLS 分别衡量什么？',
     'LCP 最大内容绘制；FID/INP 交互响应；CLS 布局稳定性。优化方向：资源优先级、减少长任务、预留尺寸避免跳动。',
     '可联系 RUM 与 lab 数据。', 'medium'::question_difficulty, ARRAY['技术知识','性能','Web Vitals']::text[]),

    -- 算法
    ('算法','图与搜索','【算法｜技术知识】Dijkstra 与 Bellman-Ford 的适用条件与复杂度？',
     'Dijkstra 非负权边，堆实现 O((V+E)logV)；Bellman-Ford 可处理负权（无负环可达），O(VE)。',
     '说明为何 Dijkstra 不能处理负边。', 'medium'::question_difficulty, ARRAY['技术知识','图','最短路']::text[]),
    ('算法','动态规划','【算法｜技术知识】编辑距离（插入/删除/替换）的状态定义与边界条件？',
     'dp[i][j] 表示 a 前 i 与 b 前 j 的最少操作数；边界 dp[i][0]=i, dp[0][j]=j；转移三选一取最小。',
     '空间可优化为一维。', 'medium'::question_difficulty, ARRAY['技术知识','动态规划','字符串']::text[]),
    ('算法','并发与锁（算法题）','【算法｜技术知识】设计一个线程安全的「固定容量阻塞队列」需要哪些同步原语？',
     '互斥锁保护缓冲区、条件变量/await 协调空满、循环数组或链表；注意虚假唤醒与边界条件。',
     '可对比 JDK ArrayBlockingQueue 思路。', 'hard'::question_difficulty, ARRAY['技术知识','并发','数据结构']::text[]),

    -- DevOps
    ('DevOps','Kubernetes','【K8s｜技术知识】Pod 的 QoS 类别有哪些？OOM 时内核按什么顺序杀进程？',
     'Guaranteed > Burstable > BestEffort；资源不足时先杀 BestEffort，再 Burstable 中超限容器。',
     '结合 requests/limits 说明归类规则。', 'medium'::question_difficulty, ARRAY['技术知识','K8s','调度']::text[]),
    ('DevOps','Linux','【Linux｜场景题】服务器 CPU 不高但 load average 很高，可能原因与排查命令？',
     '不可中断 IO、大量 Runnable 线程等待调度、磁盘阻塞；iostat、pidstat、vmstat、查看 D 状态进程与挂载点。',
     '区分 load 与 CPU 利用率。', 'hard'::question_difficulty, ARRAY['场景题','Linux','排障']::text[]),

    -- 技术管理
    ('技术管理','项目管理','【技术管理｜场景题】需求中途大幅变更，你如何评估影响并与干系人对齐？',
     '影响分析（范围、工期、风险、依赖）、备选方案与取舍、书面变更记录、更新里程碑与缓冲、同步测试与运维。',
     '体现透明沟通而非单方面拒绝或全盘接受。', 'medium'::question_difficulty, ARRAY['场景题','项目管理','变更']::text[])
  ) AS t(bucket, job, content, answer, tips, difficulty, tags)
),
resolved AS (
  SELECT
    jc.category_id,
    b.bank_id,
    s.content,
    s.answer,
    s.tips,
    s.difficulty,
    s.tags
  FROM spec s
  JOIN job_categories jc ON jc.bucket_name = s.bucket AND jc.job_name = s.job
  JOIN banks b ON b.category_id = jc.category_id AND b.bank_name = (jc.job_name || ' 题库')
)
INSERT INTO questions(bank_id, category_id, content, answer, tips, difficulty, source, tags, stats, created_by, remark)
SELECT
  r.bank_id,
  r.category_id,
  r.content,
  r.answer,
  r.tips,
  r.difficulty,
  'official'::question_source,
  r.tags,
  '{}'::jsonb,
  NULL,
  'seed_questions_extra_batch2'
FROM resolved r
WHERE NOT EXISTS (
  SELECT 1 FROM questions q
  WHERE q.deleted_at IS NULL
    AND q.bank_id = r.bank_id
    AND q.content = r.content
);

COMMIT;
