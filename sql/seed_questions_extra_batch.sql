-- 额外官方面试题（幂等：同 bank + 相同题干则不重复插入）。
-- 依赖：sql/seed_categories_offerdog.sql、各二级岗位「{岗位名} 题库」已存在（见 seed_offerdog_question_bank_and_questions.sql）。

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
    -- ========== Java：集合与并发（贴合「集合框架 / ArrayList / LinkedList」薄弱点）==========
    ('后端','Java','【Java后端｜技术知识】Java 集合框架中 Collection 与 Map 两大支系分别解决什么问题？List、Set、Queue 各举 1 个常用实现类。',
     'Collection 表示单列元素：List 有序可重复（如 ArrayList、LinkedList），Set 不重复（如 HashSet、LinkedHashSet、TreeSet），Queue/Deque 排队（如 ArrayDeque、PriorityQueue）。Map 表示键值映射（如 HashMap、LinkedHashMap、TreeMap、ConcurrentHashMap）。',
     '避免只背名字，能说出「有序/无序、是否允许 null、是否线程安全」差异更好。', 'easy'::question_difficulty, ARRAY['技术知识','Java','集合','Collection','Map']::text[]),
    ('后端','Java','【Java后端｜技术知识】ArrayList 与 LinkedList 的底层数据结构分别是什么？各自 get/add/remove 的典型时间复杂度与适用场景？',
     'ArrayList：动态数组，随机访问 O(1)，尾部追加均摊 O(1)，中间插入删除需搬移 O(n)。LinkedList：双向链表，头尾插入 O(1)，按索引访问 O(n)。高频随机读选 ArrayList；频繁头尾增删或实现队列/双端队列可考虑 LinkedList（或 ArrayDeque）。',
     '可追问：内存局部性与 CPU 缓存对 ArrayList 更友好的原因。', 'medium'::question_difficulty, ARRAY['技术知识','Java','ArrayList','LinkedList','数据结构']::text[]),
    ('后端','Java','【Java后端｜技术知识】HashMap 的 put 流程要点：如何计算桶下标？何时扩容？JDK8+ 链表与红黑树如何转换？',
     'hash 扰动后 (n-1)&hash 定位桶；元素数超过阈值*容量则扩容为 2 倍并重哈希；桶内链表长度超过树化阈值且容量≥最小树化容量时转红黑树，退化条件满足时可退化为链表。',
     '与 Hashtable、ConcurrentHashMap 的并发差异可简要对比。', 'hard'::question_difficulty, ARRAY['技术知识','Java','HashMap','集合']::text[]),
    ('后端','Java','【Java后端｜技术知识】为何重写 equals 时通常必须重写 hashCode？违反契约会在哪些集合上出问题？',
     '相等对象必须 hash 一致，否则在 HashMap/HashSet 等基于哈希的集合中会出现「逻辑相等却存两份」或 contains/get 失败。',
     '可举 String 包装类或自定义 DTO 作 key 的例子。', 'medium'::question_difficulty, ARRAY['技术知识','Java','集合','equals']::text[]),
    ('后端','Java','【Java后端｜技术知识】什么是 fail-fast？ArrayList 迭代中修改为何会抛 ConcurrentModificationException？如何安全遍历删除？',
     '迭代器检测 modCount 与 expectedModCount；结构修改触发快速失败。可用 Iterator.remove()、removeIf，或复制后遍历；并发场景用并发集合。',
     '区分 fail-fast 与 fail-safe（如 COW）语义。', 'medium'::question_difficulty, ARRAY['技术知识','Java','集合','并发']::text[]),
    ('后端','Java','【Java后端｜技术知识】ConcurrentHashMap（JDK8+）读与写的基本并发策略是什么？与 Hashtable、Collections.synchronizedMap 相比优势？',
     '分段/桶级锁或 synchronized+CAS；读大多无锁或 volatile 可见性；扩容协助转移等。比全局锁 Hashtable 粒度更细、吞吐更高；比单纯 synchronizedMap 更复杂但性能更好。',
     '不必背源码每行，讲清「粒度」与「读多写少」场景。', 'hard'::question_difficulty, ARRAY['技术知识','Java','ConcurrentHashMap','并发']::text[]),

    ('后端','Java','【Java后端｜技术知识】synchronized 与 ReentrantLock 的主要区别？何时更倾向用 Lock？',
     'synchronized JVM 内置监视器，自动释放；Lock 需手动 unlock，可 tryLock/可中断/可公平；Lock 常配合 Condition。高竞争或需超时/中断时倾向 Lock。',
     '提到内存语义与 JMM Happens-Before 即可。', 'medium'::question_difficulty, ARRAY['技术知识','Java','锁','并发']::text[]),
    ('后端','Java','【Java后端｜场景题】线程池队列满且达到 maxPoolSize 后，RejectedExecutionHandler 有哪些策略？线上一般如何选？',
     'Abort（抛异常）、CallerRuns（调用者执行）、Discard（静默丢）、DiscardOldest（丢最老）。线上常用有界队列+Abort/CallerRuns 并配合监控；重要任务需自定义降级或持久化队列。',
     '说明无界队列+固定线程的 OOM 风险。', 'medium'::question_difficulty, ARRAY['场景题','线程池','Java']::text[]),

    -- ========== 中间件 / 数据库 ==========
    ('后端','中间件','【中间件｜技术知识】Redis 持久化 RDB 与 AOF 各有什么特点？混合持久化解决什么问题？',
     'RDB 快照恢复快但可能丢一段数据；AOF 日志更耐久 fsync 策略可调；混合结合两者优势；根据业务 RPO/RTO 选型。',
     '补充：fork、Copy-on-Write 对大内存快照的影响。', 'medium'::question_difficulty, ARRAY['技术知识','Redis','持久化']::text[]),
    ('后端','中间件','【中间件｜场景题】缓存穿透、击穿、雪崩分别指什么？常见治理手段？',
     '穿透：查不存在绕过缓存打 DB — 布隆过滤器/空值缓存/接口校验。击穿：热点 key 过期 — 互斥重建/逻辑过期/永不过期+异步刷新。雪崩：大量 key 同时过期 — 随机 TTL、多级缓存、限流降级。',
     '各举一条你线上用过的手段。', 'hard'::question_difficulty, ARRAY['场景题','Redis','缓存']::text[]),

    ('后端','数据库','【数据库｜技术知识】MySQL InnoDB 四种隔离级别分别解决什么读问题？RR 下如何避免部分幻读？',
     'RU/RC/RR/Serializable；RR 通过 MVCC + 间隙锁（next-key）抑制部分幻读；说明快照读与当前读差异。',
     '避免与「完全无幻读」绝对化表述。', 'hard'::question_difficulty, ARRAY['技术知识','MySQL','事务']::text[]),
    ('后端','数据库','【数据库｜技术知识】为什么 InnoDB 常用 B+ 树而不是 B 树作为索引结构？联合索引的最左前缀如何理解？',
     'B+ 树非叶子不存数据页指针利于范围扫描与顺序 IO；叶子链表支持范围查询。联合索引按列顺序排序，跳过左列往往无法用索引后续列。',
     '可画 (a,b,c) 与三条查询例子。', 'medium'::question_difficulty, ARRAY['技术知识','MySQL','索引','B+树']::text[]),

    ('后端','分布式','【分布式｜场景题】微服务接口如何保证幂等？常见去重键与存储选型？',
     '业务幂等键（订单号、请求号）+ 去重表/Redis SETNX+TTL；状态机只允许合法转移；消息消费 ack 与重试；说明至少一次与精确一次语义取舍。',
     '结合支付或下单场景举例。', 'hard'::question_difficulty, ARRAY['场景题','幂等','分布式']::text[]),

    -- ========== 前端 ==========
    ('前端','Vue','【Web前端｜技术知识】Vue 3 中 <keep-alive> 的作用是什么？include/exclude 与 max 如何影响缓存行为？',
     '缓存动态组件实例避免反复创建销毁；include/exclude 按 name 过滤；max LRU 淘汰最久未用；注意与路由 meta 配合及内存占用。',
     '说明与 activated/deactivated 钩子关系。', 'medium'::question_difficulty, ARRAY['技术知识','Vue','性能']::text[]),
    ('前端','JavaScript/TypeScript','【Web前端｜技术知识】ES Module 与 CommonJS 在加载时机、导出绑定、this 方面有何主要差异？',
     'ESM 静态分析、编译期确定依赖、live binding；CJS 运行时加载、拷贝导出值（除对象引用）；ESM 顶层 this 为 undefined（严格模式语义）。',
     '结合打包器 tree-shaking 说明 ESM 优势。', 'medium'::question_difficulty, ARRAY['技术知识','工程化','模块']::text[]),

    -- ========== 算法 ==========
    ('算法','数据结构','【Python算法｜技术知识】双向链表与数组在「任意位置插入删除」与「按索引访问」上的复杂度对比？何时选哪个？',
     '链表已知节点指针插入删除 O(1)，按索引访问 O(n)；数组按索引 O(1)，中间插入删除 O(n)。频繁下标访问用数组；频繁头尾或已知节点链式结构用链表。',
     '与 Python list（动态数组）心智区分。', 'easy'::question_difficulty, ARRAY['技术知识','数据结构','链表']::text[]),
    ('算法','排序与双指针','【Python算法｜技术知识】快排最坏 O(n²) 在什么输入下出现？工业界常见优化有哪些？',
     '已排序+固定 pivot；优化：随机 pivot、三数取中、小数组改插排、尾递归优化、双路/三路快排处理大量重复键。',
     '说明 Python/Java 内置排序为何不用裸快排。', 'medium'::question_difficulty, ARRAY['技术知识','排序','快排']::text[]),

    ('后端','Golang','【Golang｜技术知识】goroutine 与 OS 线程相比，调度与栈模型上的主要区别是什么？',
     'M:N 调度，用户态切换成本低；栈开始小可增长；GOMAXPROCS 与 P/M/G 模型；阻塞 syscall 时线程与 goroutine 关系。',
     '避免说「goroutine 永远比线程轻」而无边界。', 'medium'::question_difficulty, ARRAY['技术知识','Go','并发']::text[]),
    ('后端','Golang','【Golang｜场景题】channel 带缓冲与不带缓冲在同步语义上有何不同？select 常用于解决什么问题？',
     '无缓冲同步握手；有缓冲异步至满/空阻塞；select 多路复用、超时、默认分支实现非阻塞。',
     '提到 close channel 与接收方规则。', 'medium'::question_difficulty, ARRAY['场景题','Go','channel']::text[])
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
  'seed_questions_extra_batch'
FROM resolved r
WHERE NOT EXISTS (
  SELECT 1 FROM questions q
  WHERE q.deleted_at IS NULL
    AND q.bank_id = r.bank_id
    AND q.content = r.content
);

COMMIT;
