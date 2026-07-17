-- Seed OfferDog demo question banks + questions (idempotent).
-- Depends on categories (level=2) existing (run sql/seed_categories_offerdog.sql first).

BEGIN;

-- Create a single official bank per level=2 category if missing.
WITH job_categories AS (
  SELECT id, name, parent_id
  FROM categories
  WHERE deleted_at IS NULL AND level = 2
),
bank_rows AS (
  SELECT
    jc.id AS category_id,
    (jc.name || ' 题库') AS bank_name,
    ('系统预置：' || jc.name || ' 模块题目（用于题库展示、向量检索与面试 RAG）。') AS bank_desc
  FROM job_categories jc
)
INSERT INTO question_banks(name, description, category_id, created_by)
SELECT br.bank_name, br.bank_desc, br.category_id, NULL
FROM bank_rows br
WHERE NOT EXISTS (
  SELECT 1
  FROM question_banks qb
  WHERE qb.deleted_at IS NULL
    AND qb.category_id = br.category_id
    AND qb.name = br.bank_name
);

-- Insert demo questions (bank is matched by category_id + name).
-- Notes:
-- - questions.category_id is required and should equal the job category id (level=2).
-- - bank_id resolved via (category_id + bank_name).
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
    -- 后端 / Java
    ('后端','Java','Spring Bean 的生命周期与常见扩展点有哪些？',
     '简述：实例化 -> 属性注入 -> Aware -> BeanPostProcessor 前置 -> 初始化(InitializingBean/@PostConstruct) -> 后置 -> 就绪；扩展点包括 BeanFactoryPostProcessor、BeanPostProcessor、InstantiationAwareBeanPostProcessor、SmartLifecycle 等。',
     '回答时给出一个你实际用过的扩展点（如用 BPP 做脱敏/埋点）。', 'medium', ARRAY['Spring','Java']),
    ('后端','Java','如何定位与解决线上 JVM 内存泄漏？',
     '思路：确认 OOM 类型与日志 -> 采集 heap dump/GC log -> MAT/YourKit 找出 dominator tree -> 结合代码定位引用链 -> 修复后回归压测并上线观察。',
     '要说清楚“泄漏”与“内存占用大”的区别，以及如何复现/验证。', 'hard', ARRAY['JVM','排查']),

    -- 后端 / 分布式
    ('后端','分布式','解释一致性哈希的原理及其在分布式缓存中的作用。',
     '将节点映射到 hash 环并把 key 映射到环上，顺时针找到第一个节点；虚拟节点降低数据倾斜；扩容/缩容只迁移局部 key。',
     '补充：与分片/取模相比迁移量与热点问题。', 'medium', ARRAY['分布式','缓存']),
    ('后端','分布式','分布式事务常见方案对比：2PC、TCC、Saga、Outbox。',
     '2PC 强一致但阻塞；TCC 预留资源+补偿；Saga 以长事务拆分+补偿；Outbox 通过本地事务+事件表+可靠投递实现最终一致。',
     '说清楚适用场景与失败恢复（重试/幂等/去重）。', 'hard', ARRAY['事务','一致性']),

    -- 前端 / JS/TS
    ('前端','JavaScript/TypeScript','解释事件循环（Event Loop）与宏任务/微任务的执行顺序。',
     '同一轮：先执行同步栈 -> 清空微任务队列(Promise.then/MutationObserver) -> 执行一个宏任务(setTimeout/I/O/UI) -> 再清空微任务……',
     '给一个输出题并解释原因。', 'medium', ARRAY['JS','异步']),
    ('前端','JavaScript/TypeScript','TypeScript 中 unknown 与 any 的区别？如何做类型收窄？',
     'any 逃逸类型系统；unknown 需要先收窄才能用。收窄方式：typeof、in、instanceof、用户自定义 type guard、判空等。',
     '给出一个自定义 type guard 示例。', 'easy', ARRAY['TS','类型系统']),

    -- 前端 / Vue
    ('前端','Vue','Vue3 的响应式系统与 Vue2 的主要区别？',
     'Vue3 用 Proxy，按需代理嵌套对象，数组/新增属性天然可追踪；Vue2 用 defineProperty 需要递归且数组/新增属性受限；Vue3 effect/track/trigger 更细粒度。',
     '联系实际：ref/reactive、toRefs、watchEffect 的使用场景。', 'medium', ARRAY['Vue','响应式']),

    -- 算法 / 动态规划
    ('算法','动态规划','讲讲 0/1 背包的状态定义与转移，并说明如何做空间优化。',
     'dp[i][w] 表示前 i 个物品容量 w 的最大价值；转移：不选 dp[i-1][w]，选 dp[i-1][w-wi]+vi；空间优化用一维 dp[w] 从大到小遍历 w。',
     '强调为什么要倒序遍历容量。', 'medium', ARRAY['DP','背包']),

    -- 算法 / 图与搜索
    ('算法','图与搜索','BFS 为什么能求无权图最短路？复杂度如何？',
     'BFS 按层扩展，第一次到达某点即为最短步数；用队列；复杂度 O(V+E)。',
     '举例说明“按层”的含义与 visited 的必要性。', 'easy', ARRAY['BFS','图']),

    -- DevOps / Docker
    ('DevOps','Docker','解释 Docker 镜像分层与容器写时复制（COW）。',
     '镜像由只读层叠加；容器层可写；修改文件触发写时复制到容器层；好处是复用与节省空间。',
     '补充：如何写 Dockerfile 让层缓存命中。', 'medium', ARRAY['Docker','镜像']),

    -- DevOps / Kubernetes
    ('DevOps','Kubernetes','Deployment、ReplicaSet、Pod 的关系是什么？滚动更新如何实现？',
     'Deployment 管理 RS，RS 管理 Pod；滚动更新通过创建新 RS，逐步扩容新 Pod/缩容旧 Pod，受 maxSurge/maxUnavailable 控制。',
     '强调探针、readiness 与流量切换。', 'medium', ARRAY['K8s','发布']),

    -- 技术管理 / 项目管理
    ('技术管理','项目管理','你如何拆解一个需求并制定里程碑与风险预案？',
     '澄清目标与边界 -> 切分工作包 -> 排期与依赖 -> 风险清单（人/技术/外部）-> 预案与监控 -> 周期复盘。',
     '用一次真实项目举例更有说服力。', 'medium', ARRAY['项目管理','协作'])
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
  r.difficulty::question_difficulty,
  'official'::question_source,
  r.tags,
  '{}'::jsonb,
  (SELECT id FROM users WHERE deleted_at IS NULL ORDER BY id ASC LIMIT 1),
  'offerdog_seed'
FROM resolved r
WHERE NOT EXISTS (
  SELECT 1 FROM questions q
  WHERE q.deleted_at IS NULL
    AND q.bank_id = r.bank_id
    AND q.content = r.content
);

COMMIT;

