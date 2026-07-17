-- Java 岗位：集合 / ArrayList 专项题（幂等：同 bank + 题干已存在则跳过）。
-- 依赖：后端 / Java 二级分类与「Java 题库」已存在。

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
    ('后端','Java','【Java｜集合｜ArrayList】ArrayList 底层用什么结构存储元素？默认初始容量与扩容规则（JDK8+）大致如何？',
     'Object 数组 elementData；默认空数组，首次 add 常扩容为 10；之后容量不足时约按 1.5 倍增长（grow 计算 newCapacity）；使用 Arrays.copyOf 拷贝。',
     '可追问：private 数组与 transient、序列化如何单独写 size。', 'medium'::question_difficulty, ARRAY['技术知识','Java','集合','ArrayList']::text[]),
    ('后端','Java','【Java｜集合｜ArrayList】ArrayList 的 add(int index, E e) 时间复杂度？为什么？',
     '平均/最坏 O(n)：需将 index 及之后元素后移一位，可能触发扩容；尾部 add 均摊 O(1)。',
     '与 LinkedList 按索引插入对比。', 'easy'::question_difficulty, ARRAY['技术知识','Java','ArrayList','复杂度']::text[]),
    ('后端','Java','【Java｜集合｜ArrayList】modCount 与 expectedModCount 在 ArrayList 迭代中起什么作用？subList 返回的子列表与母列表关系？',
     '迭代器记录 expectedModCount，结构修改使 modCount++，不一致抛 ConcurrentModificationException。subList 是母列表视图，一方结构性修改可能影响另一方（文档要求通过子列表操作）。',
     '强调 subList 非独立拷贝。', 'hard'::question_difficulty, ARRAY['技术知识','Java','ArrayList','fail-fast']::text[]),
    ('后端','Java','【Java｜集合｜ArrayList】ArrayList 是线程安全的吗？多线程下读多写少有哪些替代？',
     '非线程安全；可用 Collections.synchronizedList、CopyOnWriteArrayList（读多写少复制数组）、或外部锁；Vector 虽同步但一般不推荐新业务。',
     '说明 COW 写时复制成本。', 'medium'::question_difficulty, ARRAY['技术知识','Java','ArrayList','并发']::text[]),
    ('后端','Java','【Java｜集合｜ArrayList】ensureCapacity(int minCapacity) 与 trimToSize() 适用场景？',
     '批量添加前预扩容减少多次 grow；trimToSize 在元素数远小于容量时释放多余数组空间节省内存。',
     '与 new ArrayList<>(expectedSize) 构造配合。', 'easy'::question_difficulty, ARRAY['技术知识','Java','ArrayList','性能']::text[]),
    ('后端','Java','【Java｜集合】List、Set、Queue 与 Map 在 Java 集合框架中的角色？ArrayList 实现了哪些接口？',
     'List 有序可重复；Set 不重复；Queue 排队；Map 键值对。ArrayList 实现 List、RandomAccess、Cloneable、Serializable 等；不实现 Set/Map。',
     'RandomAccess 标记对 Collections.binarySearch 等算法的意义。', 'easy'::question_difficulty, ARRAY['技术知识','Java','集合','Collection','List']::text[])
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
  'seed_java_collection_arraylist'
FROM resolved r
WHERE NOT EXISTS (
  SELECT 1 FROM questions q
  WHERE q.deleted_at IS NULL
    AND q.bank_id = r.bank_id
    AND q.content = r.content
);

COMMIT;
