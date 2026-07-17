-- 结构化面试种子题：明确定义三类岗位 — Java 后端工程师、Web 前端工程师、Python 算法工程师。
-- 题型覆盖：技术知识、项目经历深挖、场景题、行为题。
-- 依赖：sql/seed_categories_offerdog.sql；各二级岗位「{岗位名} 题库」已存在（见 seed_offerdog_question_bank_and_questions.sql）。
-- 官方题 created_by 可为 NULL（见 sql/questions_created_by_nullable.sql）。按题干去重，可重复执行。

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
    -- ========== 岗位：Java 后端工程师（后端 / Java）==========
    ('后端','Java','【Java后端｜技术知识】结合 JDK 源码直觉，说明 HashMap 在 Java 8+ 中链表与红黑树转换的触发条件及时间复杂度考量。',
     '链表长度超过树化阈值（默认 8）且桶数组长度达到最小树化容量（默认 64）时链表转红黑树，查找从 O(n) 改善为 O(log n)；退化条件满足时会退化为链表。需提到 resize、hash 扰动与 equals。',
     '避免只背阈值数字，说明「为何需要树化」与 resize 对性能影响。', 'hard'::question_difficulty, ARRAY['技术知识','Java','集合']::text[]),
    ('后端','Java','【Java后端｜技术知识】ThreadPoolExecutor 核心参数有哪些？各自如何影响稳定性与吞吐？',
     'corePoolSize、maximumPoolSize、keepAliveTime、workQueue、threadFactory、handler；队列选型（有界/无界）影响背压与 OOM 风险；拒绝策略与监控指标（队列长度、活跃线程、完成任务数）。',
     '结合一个线上线程池调参或事故例子。', 'medium'::question_difficulty, ARRAY['技术知识','并发','线程池']::text[]),
    ('后端','Java','【Java后端｜技术知识】Spring @Transactional 在什么常见场景下会「看起来不生效」？如何避免？',
     '自调用绕过代理、异常类型不匹配（默认仅 rollback Runtime）、非 public、多数据源未走同一 TM、异步线程脱离事务边界等；可通过拆 Bean、AopContext、明确 rollbackFor、编程式事务等修复。',
     '回答时至少举 2 个真实踩坑点。', 'medium'::question_difficulty, ARRAY['技术知识','Spring','事务']::text[]),
    ('后端','Java','【Java后端｜技术知识】解释 MySQL 联合索引的最左前缀原则，并说明「索引下推」如何减少回表。',
     '最左：条件从索引最左列连续匹配才能充分用索引；跳过左列则无法用到后续列。ICP 在存储引擎层过滤索引行，减少回 Server 层与回表次数。',
     '可画一个 (a,b,c) 索引与三条查询的用索引情况。', 'medium'::question_difficulty, ARRAY['技术知识','MySQL','索引']::text[]),
    ('后端','Java','【Java后端｜技术知识】缓存旁路（Cache-Aside）下，先更 DB 再删缓存与先删缓存再更 DB 各有什么一致性与并发问题？',
     '两种顺序在并发读写下都可能出现短暂不一致；常用「先更 DB 再删缓存」+ 重试删缓存/延迟双删缓解；关键配合过期时间、版本号或读写串行化热点 key。',
     '说明「强一致」与「最终一致」期望不要混谈。', 'hard'::question_difficulty, ARRAY['技术知识','Redis','一致性']::text[]),

    ('后端','Java','【Java后端｜项目深挖】选一个你负责过的最复杂业务模块：业务目标、边界、你的职责、关键设计决策分别是什么？',
     'STAR：量化指标（QPS、数据量、延迟）；讲清个人产出与团队协作分界；设计决策要有备选方案对比与取舍理由。',
     '面试官常追问：若需求翻倍你如何扩展。', 'medium'::question_difficulty, ARRAY['项目深挖','架构','协作']::text[]),
    ('后端','Java','【Java后端｜项目深挖】描述一次线上故障：现象、定位路径、根因、临时止血与永久修复、复盘项。',
     '时间线清晰；工具（日志、监控、链路、DB、JVM）；根因到代码/SQL/配置；复盘要有可执行 action（监控、限流、单测、Runbook）。',
     '突出你在其中的角色，而非泛泛「我们」。', 'hard'::question_difficulty, ARRAY['项目深挖','稳定性','排障']::text[]),
    ('后端','Java','【Java后端｜项目深挖】你如何在迭代中偿还技术债？如何向非技术干系人证明「值得做」？',
     '识别风险（故障概率×影响）、量化收益（人天、缺陷率、性能）；切片交付与 feature flag；与业务共背 KPI（如发布频率、缺陷）。',
     '避免空谈「代码丑」，用数据说话。', 'medium'::question_difficulty, ARRAY['项目深挖','技术债','沟通']::text[]),

    ('后端','Java','【Java后端｜场景题】设计一个「下单减库存」接口，要求防超卖并尽量低延迟，你会如何选型与分层？',
     'DB 乐观锁/悲观锁、Redis 预减与异步落库、库存分段、热点 sku 隔离；幂等与订单状态机；压测与监控；说明强一致与最终一致边界。',
     '追问准备：缓存与 DB 对账怎么做。', 'hard'::question_difficulty, ARRAY['场景题','秒杀','库存']::text[]),
    ('后端','Java','【Java后端｜场景题】微服务链路中某下游突然超时飙升，你会如何快速缩小范围并恢复服务？',
     '限流/熔断/隔离止血；采样链路看慢节点；线程池与连接池；依赖版本与发布关联；降级开关与静态兜底；事后容量评估与超时与重试策略修正。',
     '强调「先恢复再查因」。', 'medium'::question_difficulty, ARRAY['场景题','微服务','SRE']::text[]),

    ('后端','Java','【Java后端｜行为题】与产品对需求优先级产生冲突时，你如何处理？',
     '对齐业务目标与数据；拆分 MVP；风险透明（工期/质量）；书面记录决策；必要时升级决策人。避免对立人格，聚焦用户价值与成本。',
     '举一次真实协调例子。', 'easy'::question_difficulty, ARRAY['行为题','沟通','优先级']::text[]),
    ('后端','Java','【Java后端｜行为题】你如何做技术方案评审？最看重哪些维度？',
     '正确性、性能与容量、可观测性、回滚与发布、安全与合规、成本；鼓励提前小 PR/设计文档；记录结论与待办。',
     '可对比一次「评审拦住问题」的案例。', 'medium'::question_difficulty, ARRAY['行为题','评审','工程文化']::text[]),

    -- ========== 岗位：Web 前端工程师（分散在 JS/TS、Vue、工程化、性能优化）==========
    ('前端','JavaScript/TypeScript','【Web前端｜技术知识】说明浏览器事件循环中宏任务与微任务的执行顺序，并解释 async/await 的微观表现。',
     '每轮：执行栈清空后先跑微任务队列再执行一个宏任务；await 后续相当于微任务。可结合打印顺序题说明。',
     '避免与 Node 事件循环细节混淆时可先声明讨论范围。', 'medium'::question_difficulty, ARRAY['技术知识','JS','异步']::text[]),
    ('前端','JavaScript/TypeScript','【Web前端｜技术知识】TypeScript 中「类型收窄」常见手段有哪些？何时需要自定义 type guard？',
     'typeof/instanceof/in、判别联合、可辨识 tag、判空；当运行时结构无法被编译器自动推断时需要自定义 predicate 函数。',
     '给一个 isFoo(x): x is Foo 示例思路即可。', 'easy'::question_difficulty, ARRAY['技术知识','TypeScript']::text[]),
    ('前端','JavaScript/TypeScript','【Web前端｜场景题】跨域是什么？CORS 预检请求在什么条件下触发？如何与后端协作排查失败？',
     '同源策略限制；简单请求与非简单请求（自定义头、非简单方法、Content-Type 等）触发 OPTIONS；检查响应头 Access-Control-*、凭证与通配符、网关是否吞头。',
     '提到 credentials 时 * 的限制。', 'medium'::question_difficulty, ARRAY['场景题','CORS','安全']::text[]),
    ('前端','JavaScript/TypeScript','【Web前端｜项目深挖】你在项目中如何管理复杂客户端状态（服务端状态 vs UI 状态）？选型依据是什么？',
     '分层：服务端缓存（如 SWR/React Query 思路）与本地 UI 状态分离；全局 store 仅放横切 concern；说明何时不需要 Redux。',
     '结合具体页面或数据依赖图。', 'medium'::question_difficulty, ARRAY['项目深挖','状态管理']::text[]),
    ('前端','JavaScript/TypeScript','【Web前端｜行为题】代码评审中你常提的几类意见是什么？遇到「风格争论」怎么办？',
     '可读性、边界与错误处理、性能隐患、安全（XSS）、可测试性；风格交给自动化工具（ESLint/Prettier）；争论用团队规范与数据（包体、性能）裁决。',
     '体现协作而非挑剔。', 'easy'::question_difficulty, ARRAY['行为题','代码评审']::text[]),

    ('前端','Vue','【Web前端｜技术知识】Vue 3 中 ref 与 reactive 的适用场景差异是什么？toRefs 解决什么问题？',
     'ref 适合原始值与单一引用；reactive 适合对象整体响应式；解构会丢响应需 toRefs；大型表单/嵌套对象选型举例。',
     '可提 script setup 心智。', 'easy'::question_difficulty, ARRAY['技术知识','Vue','响应式']::text[]),
    ('前端','Vue','【Web前端｜技术知识】v-if 与 v-show 的性能与语义差异？在首屏与切换频繁场景如何选？',
     'v-if 惰性渲染有切换成本；v-show 初始渲染成本、切换仅 display；首屏隐藏且重可用 v-if，频繁切换用 v-show。',
     '联系真实组件（弹窗、Tab）。', 'easy'::question_difficulty, ARRAY['技术知识','Vue','渲染']::text[]),
    ('前端','Vue','【Web前端｜项目深挖】描述一次你在 Vue 项目中做的性能优化：前后指标、手段、验证方式。',
     '指标：LCP/TTI/长任务；手段：懒加载、虚拟列表、拆分组件、减少 watch 触发；验证：Lighthouse、Performance、线上 RUM。',
     '数字哪怕是区间也比模糊形容好。', 'medium'::question_difficulty, ARRAY['项目深挖','性能','Vue']::text[]),
    ('前端','Vue','【Web前端｜场景题】基于 Vue Router 实现「路由级权限」：有哪些实现层次？刷新与异步路由如何兼顾安全？',
     '路由 meta + 导航守卫；菜单与路由表由后端下发时的校验；前端隐藏不等于安全，敏感能力必须服务端鉴权；异步组件与加载失败兜底。',
     '强调前后端分工。', 'medium'::question_difficulty, ARRAY['场景题','路由','权限']::text[]),

    ('前端','工程化','【Web前端｜技术知识】Vite 与 Webpack 在开发态构建路径上的核心差异是什么？对大型 Monorepo 有何影响？',
     'Vite 开发态利用原生 ESM + 预依赖打包（esbuild），冷启动快；Webpack 传统打包开发服务器；Monorepo 需注意依赖预构建、别名与 workspace 软链。',
     '不必贬损某一工具，谈 trade-off。', 'medium'::question_difficulty, ARRAY['技术知识','工程化','构建']::text[]),
    ('前端','工程化','【Web前端｜场景题】前端 CI 流水线你建议包含哪些阶段？如何保证「可回滚」？',
     'install、lint、test、build、制品上传、部署与探针；制品不可变；按 git tag 回滚；与后端协调环境变量与配置中心。',
     '提到 preview 环境。', 'medium'::question_difficulty, ARRAY['场景题','CI/CD']::text[]),
    ('前端','工程化','【Web前端｜行为题】业务催得紧时，你如何平衡「快速交付」与「工程质量」？',
     '切片交付、关键路径测试、特性开关、技术债登记与排期；用风险沟通替代空头承诺。',
     '举例说明哪些底线不能妥协（安全、合规）。', 'medium'::question_difficulty, ARRAY['行为题','交付','质量']::text[]),

    ('前端','性能优化','【Web前端｜场景题】线上用户反馈首屏慢，你会如何在前端侧系统化排查？',
     'RUM 看 LCP 元素与 TTFB；网络瀑布与缓存；JS 体积与长任务；图片与字体策略；SSR/边缘缓存是否命中；对比发布与地域。',
     '先定义「慢」的指标。', 'medium'::question_difficulty, ARRAY['场景题','性能','首屏']::text[]),
    ('前端','性能优化','【Web前端｜场景题】页面长时间停留后变卡，可能原因有哪些？如何用工具验证？',
     '内存泄漏（监听器、闭包、全局缓存）、Detached DOM、定时器未清理、无限增长的状态；Chrome Memory/Performance、复现路径与堆快照对比。',
     '说明修复后如何加回归测试或监控。', 'hard'::question_difficulty, ARRAY['场景题','内存','排障']::text[]),

    -- ========== 岗位：Python 算法工程师（算法 / 多二级类）==========
    ('算法','数据结构','【Python算法｜技术知识】用 Python 实现 LRU 缓存（可描述思路或伪代码）：应用哪些数据结构？各操作均摊复杂度？',
     'OrderedDict 或 dict+双向链表；get/put O(1)；解释为何 list 不合适；线程安全若需要可加锁。',
     '若手写链表，注意边界与移动节点细节。', 'hard'::question_difficulty, ARRAY['技术知识','数据结构','LRU']::text[]),
    ('算法','数据结构','【Python算法｜技术知识】堆（heapq）适合解决哪些典型问题？建堆与时间复杂度如何？',
     'TopK、合并 K 路有序、调度问题；heapq 为最小堆，nlargest 内部策略；heapify O(n)，单次 push/pop O(log n)。',
     '对比快排找第 K 大的适用场景。', 'medium'::question_difficulty, ARRAY['技术知识','堆','Python']::text[]),
    ('算法','数据结构','【Python算法｜项目深挖】讲一个你用 Python 做批量数据处理或算法落地的项目：数据规模、瓶颈、优化前后复杂度或耗时。',
     '用向量化（NumPy/Pandas）替代 Python for；分块读取；Profiling 定位热点；必要时 C 扩展或 Numba（了解即可）。',
     '准备具体数字或数量级。', 'medium'::question_difficulty, ARRAY['项目深挖','Python','性能']::text[]),
    ('算法','数据结构','【Python算法｜场景题】日志中统计海量 URL 出现次数 Top 100，单机内存有限，如何设计？',
     '分治/hash 分片外排；或 Count-Min Sketch/蓄水池近似（若允许误差）；精确解用磁盘归并；说明误差与资源 trade-off。',
     '追问：分布式如何做。', 'hard'::question_difficulty, ARRAY['场景题','大数据','TopK']::text[]),

    ('算法','动态规划','【Python算法｜技术知识】0/1 背包状态定义与转移方程是什么？一维滚动时为何倒序遍历容量？',
     'dp[i][w] 表示前 i 件容量 w 最大价值；一维时倒序避免同一轮重复使用第 i 件；空间 O(W)。',
     '手写一个 Python 循环骨架即可。', 'medium'::question_difficulty, ARRAY['技术知识','动态规划','背包']::text[]),
    ('算法','动态规划','【Python算法｜技术知识】最长递增子序列（LIS）的 O(n log n) 思路中，辅助数组维护的是什么不变量？',
     'tails[k] 表示长度为 k+1 的递增子序列最小末尾；二分查找第一个 ≥ x 的位置替换；不变量：tails 严格递增。',
     '对比 O(n^2) DP 何时仍够用。', 'hard'::question_difficulty, ARRAY['技术知识','动态规划','LIS']::text[]),
    ('算法','动态规划','【Python算法｜场景题】股票买卖系列题（含冷冻期/手续费）的通用建模思路是什么？',
     '定义持有/不持有/冷冻等状态，按天转移；注意边界与最后一天卖出；复杂度 O(n)。',
     '说明状态要覆盖所有合法决策。', 'medium'::question_difficulty, ARRAY['场景题','动态规划','状态机']::text[]),
    ('算法','动态规划','【Python算法｜行为题】遇到一道没见过的 DP，你在面试或工作中如何拆解？',
     '找子问题与最优子结构；定义状态维度；写转移与边界；小例子手算；验证复杂度；尝试降维或贪心判定。',
     '体现思维过程比背题重要。', 'easy'::question_difficulty, ARRAY['行为题','方法论','算法']::text[]),

    ('算法','图与搜索','【Python算法｜技术知识】有向无环图中如何求最长路径（权值在边上）？用什么算法框架？',
     '拓扑排序后松弛；或 DAG 上 DP；检测环；复杂度 O(V+E)。',
     '与 Dijkstra 适用条件对比。', 'medium'::question_difficulty, ARRAY['技术知识','图','拓扑']::text[]),
    ('算法','图与搜索','【Python算法｜场景题】无限网格中从起点到终点的最短步数（四连通）用 BFS，如何避免重复访问？',
     '队列分层 BFS；visited 集合或标记；注意 Python 中 tuple 坐标作 key；边界与障碍物处理。',
     '说明为何 DFS 不适合最短路（无权）。', 'easy'::question_difficulty, ARRAY['场景题','BFS','图']::text[]),

    ('算法','排序与双指针','【Python算法｜技术知识】归并排序与快速排序的稳定性、平均与最坏时间、空间复杂度对比。',
     '归并稳定 O(n log n) 稳定，空间 O(n)；快排平均 O(n log n) 最坏 O(n^2)，原地大致 O(log n) 栈，不稳定。',
     '说明 Python sort 为 Timsort，大多情况好用。', 'medium'::question_difficulty, ARRAY['技术知识','排序','复杂度']::text[]),
    ('算法','排序与双指针','【Python算法｜场景题】有序数组找两数之和为 target，O(n) 做法？若需所有不重复二元组呢？',
     '双指针向中间收拢；全量解需在移动时跳过重复元素；注意索引与越界。',
     '与哈希 O(n) 空间方案对比。', 'medium'::question_difficulty, ARRAY['场景题','双指针','数组']::text[])
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
  'seed_interview_roles_java_web_algo'
FROM resolved r
WHERE NOT EXISTS (
  SELECT 1 FROM questions q
  WHERE q.deleted_at IS NULL
    AND q.bank_id = r.bank_id
    AND q.content = r.content
);

COMMIT;
