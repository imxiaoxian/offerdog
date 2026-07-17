-- 确保「Java 题库」下有题：按题库名称解析 bank_id/category_id，不依赖 seed 中的 bucket JOIN。
-- 幂等：同一 bank_id + content 已存在则跳过。
-- 适用：仅有空壳 Java 题库、或历史种子 JOIN 未写入题目时，手动或随 bundle 执行。

BEGIN;

WITH java_bank AS (
  SELECT b.id AS bank_id, b.category_id
  FROM question_banks b
  WHERE b.deleted_at IS NULL
    AND b.name = 'Java 题库'
  ORDER BY b.id
  LIMIT 1
),
spec(content, answer, tips, difficulty, tags) AS (
  SELECT * FROM (VALUES
    ('Spring Bean 的生命周期与常见扩展点有哪些？',
     '简述：实例化 -> 属性注入 -> Aware -> BeanPostProcessor 前置 -> 初始化(InitializingBean/@PostConstruct) -> 后置 -> 就绪；扩展点包括 BeanFactoryPostProcessor、BeanPostProcessor、InstantiationAwareBeanPostProcessor、SmartLifecycle 等。',
     '回答时给出一个你实际用过的扩展点（如用 BPP 做脱敏/埋点）。', 'medium'::question_difficulty, ARRAY['Spring','Java']::text[]),
    ('如何定位与解决线上 JVM 内存泄漏？',
     '思路：确认 OOM 类型与日志 -> 采集 heap dump/GC log -> MAT/YourKit 找出 dominator tree -> 结合代码定位引用链 -> 修复后回归压测并上线观察。',
     '要说清楚「泄漏」与「内存占用大」的区别，以及如何复现/验证。', 'hard'::question_difficulty, ARRAY['JVM','排查']::text[]),
    ('【Java｜集合】Java 集合框架中 Collection 与 Map 两大支系分别解决什么问题？List、Set、Queue 各举 1 个常用实现类。',
     'Collection 表示单列元素：List 有序可重复（如 ArrayList、LinkedList），Set 不重复（如 HashSet、LinkedHashSet、TreeSet），Queue/Deque 排队（如 ArrayDeque、PriorityQueue）。Map 表示键值映射（如 HashMap、LinkedHashMap、TreeMap、ConcurrentHashMap）。',
     '避免只背名字，能说出「有序/无序、是否允许 null、是否线程安全」差异更好。', 'easy'::question_difficulty, ARRAY['技术知识','Java','集合','Collection','Map']::text[]),
    ('【Java｜ArrayList】ArrayList 与 LinkedList 的底层数据结构分别是什么？各自 get/add/remove 的典型时间复杂度与适用场景？',
     'ArrayList：动态数组，随机访问 O(1)，尾部追加均摊 O(1)，中间插入删除需搬移 O(n)。LinkedList：双向链表，头尾插入 O(1)，按索引访问 O(n)。高频随机读选 ArrayList；频繁头尾增删或双端队列可考虑 LinkedList 或 ArrayDeque。',
     '可追问：内存局部性与 CPU 缓存对 ArrayList 更友好的原因。', 'medium'::question_difficulty, ARRAY['技术知识','Java','ArrayList','LinkedList']::text[]),
    ('【Java｜HashMap】HashMap 的 put 流程要点：如何计算桶下标？何时扩容？JDK8+ 链表与红黑树如何转换？',
     'hash 扰动后 (n-1)&hash 定位桶；元素数超过阈值*容量则扩容为 2 倍并重哈希；桶内链表长度超过树化阈值且容量≥最小树化容量时转红黑树，退化条件满足时可退化为链表。',
     '与 Hashtable、ConcurrentHashMap 的并发差异可简要对比。', 'hard'::question_difficulty, ARRAY['技术知识','Java','HashMap','集合']::text[]),
    ('【Java后端｜技术知识】结合 JDK 源码直觉，说明 HashMap 在 Java 8+ 中链表与红黑树转换的触发条件及时间复杂度考量。',
     '链表长度超过树化阈值（默认 8）且桶数组长度达到最小树化容量（默认 64）时链表转红黑树，查找从 O(n) 改善为 O(log n)；退化条件满足时会退化为链表。需提到 resize、hash 扰动与 equals。',
     '避免只背阈值数字，说明「为何需要树化」与 resize 对性能影响。', 'hard'::question_difficulty, ARRAY['技术知识','Java','集合']::text[]),
    ('【Java后端｜技术知识】ThreadPoolExecutor 核心参数有哪些？各自如何影响稳定性与吞吐？',
     'corePoolSize、maximumPoolSize、keepAliveTime、workQueue、threadFactory、handler；队列选型（有界/无界）影响背压与 OOM 风险；拒绝策略与监控指标（队列长度、活跃线程、完成任务数）。',
     '结合一个线上线程池调参或事故例子。', 'medium'::question_difficulty, ARRAY['技术知识','并发','线程池']::text[]),
    ('【Java后端｜技术知识】Spring @Transactional 在什么常见场景下会「看起来不生效」？如何避免？',
     '自调用绕过代理、异常类型不匹配（默认仅 rollback Runtime）、非 public、多数据源未走同一 TM、异步线程脱离事务边界等；可通过拆 Bean、AopContext、明确 rollbackFor、编程式事务等修复。',
     '回答时至少举 2 个真实踩坑点。', 'medium'::question_difficulty, ARRAY['技术知识','Spring','事务']::text[]),
    ('【Java｜并发】synchronized 与 ReentrantLock 的主要区别？何时更倾向用 Lock？',
     'synchronized JVM 内置监视器，自动释放；Lock 需手动 unlock，可 tryLock/可中断/可公平；Lock 常配合 Condition。高竞争或需超时/中断时倾向 Lock。',
     '提到内存语义与 JMM Happens-Before 即可。', 'medium'::question_difficulty, ARRAY['技术知识','Java','锁']::text[]),
    ('【Java｜IO】BIO、NIO、AIO 的主要区别？NIO 中 Buffer、Channel、Selector 各起什么作用？',
     'BIO 一连接一线程阻塞读写；NIO 多路复用非阻塞+Selector 监听就绪事件；AIO 异步回调（平台支持差异）。Buffer 读写缓冲，Channel 双向传输，Selector 单线程管理多 Channel。',
     '结合 Netty 或 Tomcat NIO 连接器谈工程实践。', 'hard'::question_difficulty, ARRAY['技术知识','Java','NIO']::text[])
  ) AS t(content, answer, tips, difficulty, tags)
)
INSERT INTO questions(bank_id, category_id, content, answer, tips, difficulty, source, tags, stats, created_by, remark)
SELECT
  jb.bank_id,
  jb.category_id,
  s.content,
  s.answer,
  s.tips,
  s.difficulty,
  'official'::question_source,
  s.tags,
  '{}'::jsonb,
  NULL,
  'ensure_java_bank_questions'
FROM spec s
CROSS JOIN java_bank jb
WHERE NOT EXISTS (
  SELECT 1 FROM questions q
  WHERE q.deleted_at IS NULL
    AND q.bank_id = jb.bank_id
    AND q.content = s.content
);

COMMIT;
