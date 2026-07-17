-- 针对报告薄弱点表述（如「集合框架整体结构（Collection/Map接口）、常用实现类、ArrayList与LinkedList…」）补充面试题，便于关键词与向量检索命中。
-- 幂等：同一 bank_id + content 已存在则跳过。
-- 依赖：存在「后端 / Java」二级分类下的题库（优先 name = Java 题库）。

BEGIN;

WITH candidates AS (
  SELECT
    b.id AS bank_id,
    b.category_id,
    CASE WHEN b.name = 'Java 题库' THEN 0 ELSE 1 END AS sort_pri
  FROM question_banks b
  JOIN categories c ON c.id = b.category_id AND c.deleted_at IS NULL
  JOIN categories p ON p.id = c.parent_id AND p.deleted_at IS NULL
  WHERE b.deleted_at IS NULL
    AND p.name = '后端'
    AND c.name = 'Java'
),
chosen AS (
  SELECT bank_id, category_id
  FROM candidates
  ORDER BY sort_pri ASC, bank_id ASC
  LIMIT 1
),
spec(content, answer, tips, difficulty, tags) AS (
  SELECT * FROM (VALUES
    (
      '【集合框架整体结构】请说明 Java 集合框架中 Collection 与 Map 两大接口体系的职责差异，并各列举 2 个常用实现类。',
      'Collection 侧描述单列元素：List/Set/Queue 等；Map 描述键值映射。常用实现如 ArrayList、LinkedList、HashSet、PriorityQueue；HashMap、LinkedHashMap、TreeMap、ConcurrentHashMap 等。',
      '回答时带一句「是否允许 null、是否有序、是否线程安全」更易得分。',
      'medium'::question_difficulty,
      ARRAY['集合框架', 'Collection', 'Map', '常用实现类', 'Java', '集合']::text[]
    ),
    (
      '【Collection/Map 接口】List、Set、Queue 与 Map 在语义上的根本区别是什么？HashMap 与 Hashtable 能否放入 null 键值？',
      'List 有序可重复；Set 不重复；Queue 按 FIFO/优先级等规则出队；Map 是键到值的映射。HashMap 允许 null 键（一个）与 null 值；Hashtable 不允许 null 键值。',
      '可追问 ConcurrentHashMap 对 null 的限制。',
      'medium'::question_difficulty,
      ARRAY['Collection', 'Map', 'HashMap', 'Java', '集合框架']::text[]
    ),
    (
      '【常用实现类】日常开发中 ArrayList、LinkedList、HashSet、LinkedHashSet、TreeSet 的典型选型依据是什么？',
      '随机访问多读少插删用 ArrayList；头尾频繁增删或双端队列场景可考虑 LinkedList/ArrayDeque；只需去重用 HashSet；需插入顺序用 LinkedHashSet；需排序/范围操作用 TreeSet（元素需可比）。',
      '避免「一律 ArrayList」的笼统答案。',
      'medium'::question_difficulty,
      ARRAY['常用实现类', 'ArrayList', 'LinkedList', 'HashSet', 'Java', '集合']::text[]
    ),
    (
      '【ArrayList与LinkedList底层数据结构对比】二者底层分别是什么？get、中间插入删除的复杂度与内存局部性差异？',
      'ArrayList：动态数组，索引访问 O(1)，中间插入删除 O(n) 且需搬移；内存连续缓存友好。LinkedList：双向链表，按索引访问 O(n)，头尾插入 O(1)；节点分散缓存不友好。',
      '可补充：LinkedList 实现 Deque；大量中间插入未必总快于 ArrayList（常数因子与 CPU cache）。',
      'hard'::question_difficulty,
      ARRAY['ArrayList', 'LinkedList', '底层数据结构', '对比', 'Java', '集合']::text[]
    ),
    (
      '【集合框架】Iterable、Iterator、ListIterator 与 fail-fast 迭代器是什么关系？并发修改时为什么会抛 ConcurrentModificationException？',
      'Iterable 提供 iterator()；Iterator 单向遍历；ListIterator 支持 List 双向与元素替换。迭代中结构性修改导致 modCount 与 expectedModCount 不一致触发 fail-fast（非强一致保证）。',
      '区分 fail-fast 与 fail-safe（如 COW 迭代器）语义。',
      'hard'::question_difficulty,
      ARRAY['集合框架', 'Iterator', 'fail-fast', 'Java', '集合']::text[]
    ),
    (
      '【Map 实现类】HashMap 在 JDK8+ 下 put 的主要步骤：hash 扰动、寻址、冲突处理、链表与红黑树转换条件？',
      '扰动后 (n-1)&hash 定位桶；equals 比较键；冲突用链表/红黑树；链表长度超过树化阈值且容量达最小树化容量时转树，退化条件满足可链表化；扩容为 2 倍并重分布。',
      '与 LinkedHashMap 插入顺序、accessOrder 的区别可提一句。',
      'hard'::question_difficulty,
      ARRAY['HashMap', 'Map', '集合', '红黑树', 'Java']::text[]
    ),
    (
      '【集合】Collections.unmodifiableList / synchronizedList 与 Guava ImmutableList 大致解决什么问题？',
      'unmodifiable 包装后不可变视图（底层仍可能被改）；synchronizedList 对方法加锁粗粒度同步；ImmutableList 真正不可变实例，适合安全共享与并发读。',
      '说明「视图」与「拷贝」差异。',
      'medium'::question_difficulty,
      ARRAY['集合', 'Collections', '不可变', 'Java']::text[]
    ),
    (
      '【ArrayList】如何减少 ArrayList 频繁扩容带来的性能抖动？subList 返回的是独立集合吗？',
      '预估容量用 new ArrayList<>(n) 或 ensureCapacity；批量 addAll 前可 ensureCapacity。subList 是原列表视图，非拷贝，结构性修改会相互影响。',
      '结合线上一次 list 预分配优化案例。',
      'easy'::question_difficulty,
      ARRAY['ArrayList', '扩容', 'subList', 'Java', '集合']::text[]
    )
  ) AS t(content, answer, tips, difficulty, tags)
)
INSERT INTO questions (bank_id, category_id, content, answer, tips, difficulty, source, tags, stats, created_by, remark)
SELECT
  ch.bank_id,
  ch.category_id,
  s.content,
  s.answer,
  s.tips,
  s.difficulty,
  'official'::question_source,
  s.tags,
  '{}'::jsonb,
  NULL,
  'insert_weakpoint_java_collection_questions'
FROM spec s
CROSS JOIN chosen ch
WHERE EXISTS (SELECT 1 FROM chosen)
  AND NOT EXISTS (
    SELECT 1
    FROM questions q
    WHERE q.deleted_at IS NULL
      AND q.bank_id = ch.bank_id
      AND q.content = s.content
  );

COMMIT;
