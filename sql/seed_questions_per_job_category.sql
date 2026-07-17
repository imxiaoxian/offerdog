-- 全岗位知识点覆盖：每个二级分类（与 seed_categories_offerdog 一致）至少 3 道题。
-- 幂等：同 bank_id + 题干 content 已存在则跳过。
-- 依赖：sql/seed_categories_offerdog.sql、各「{岗位名} 题库」已存在。

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
    -- ========== 后端 / Java ==========
    ('后端','Java','【Java｜IO】BIO、NIO、AIO 的主要区别？NIO 中 Buffer、Channel、Selector 各起什么作用？',
     'BIO 一连接一线程阻塞读写；NIO 多路复用非阻塞+Selector 监听就绪事件；AIO 异步回调（平台支持差异）。Buffer 读写缓冲，Channel 双向传输，Selector 单线程管理多 Channel。',
     '结合 Netty 或 Tomcat NIO 连接器谈工程实践。', 'hard'::question_difficulty, ARRAY['技术知识','Java','NIO','IO']::text[]),
    ('后端','Java','【Java｜类加载】双亲委派模型是什么？什么场景会打破委派？',
     '类加载器先交给父加载器，避免核心类被篡改；Tomcat 隔离、SPI、OSGi、热部署等可能打破。',
     '说明 findClass 与 loadClass 差异。', 'medium'::question_difficulty, ARRAY['技术知识','Java','类加载','JVM']::text[]),
    ('后端','Java','【Java｜语法与基础】String 为什么不可变？字符串常量池与 intern() 需要注意什么？',
     'final char 数组/字节数组与安全、哈希缓存；常量池复用；intern 可能导致池膨胀与 GC 压力。',
     'JDK9+ 内部存储从 char[] 到 byte[] 的变化可提一句。', 'medium'::question_difficulty, ARRAY['技术知识','Java','String']::text[]),

    -- ========== 后端 / Golang ==========
    ('后端','Golang','【Go｜defer】defer 的执行顺序与参数求值时机？常见踩坑？',
     'LIFO 执行；defer 注册时求值（传参已固定）；循环内 defer 可能泄漏资源应包一层函数。',
     '与 recover 配合时 defer 要放在可能发生 panic 的函数内。', 'medium'::question_difficulty, ARRAY['技术知识','Go','defer']::text[]),
    ('后端','Golang','【Go｜并发】Context 的取消与超时如何向下传递？WithValue 滥用问题？',
     '派生 context 链式传递；Done/Err/Deadline；WithValue 仅传请求域元数据，避免塞大对象或业务状态。',
     '说明 server 端超时与 client 超时的协调。', 'medium'::question_difficulty, ARRAY['技术知识','Go','Context']::text[]),
    ('后端','Golang','【Go｜接口】Go 的隐式接口（structural typing）与 Java 显式实现有何不同？',
     '实现方无需声明 implements，只要方法集匹配即可；利于解耦与测试替身；注意接口为 nil 的坑（类型+值均为 nil）。',
     '举例 io.Reader 组合。', 'easy'::question_difficulty, ARRAY['技术知识','Go','接口']::text[]),

    -- ========== 后端 / Python ==========
    ('后端','Python','【Python｜语言】装饰器（decorator）本质是什么？如何保留被装饰函数的元信息？',
     '高阶函数包装；用 functools.wraps 复制 __name__、docstring 等；带参装饰器需多一层工厂。',
     '类作为装饰器与 __call__。', 'medium'::question_difficulty, ARRAY['技术知识','Python','装饰器']::text[]),
    ('后端','Python','【Python｜并发】asyncio 中 gather 与 wait、create_task 的区别？',
     'gather 返回结果列表且可取消策略；wait 更底层返回 done/pending；create_task 立即调度协程。',
     '说明与线程池 asyncio.to_thread 的边界。', 'medium'::question_difficulty, ARRAY['技术知识','Python','asyncio']::text[]),
    ('后端','Python','【Python｜内存】CPython 引用计数与循环引用如何被回收？',
     '主要引用计数；循环引用由分代 GC 检测；可通过 weakref 打破循环。',
     '简述 gc 模块与调试泄漏。', 'hard'::question_difficulty, ARRAY['技术知识','Python','GC']::text[]),

    -- ========== 后端 / C++ ==========
    ('后端','C++','【C++｜移动语义】std::move 做了什么？移动构造与拷贝构造何时自动生成？',
     'move 转右值引用；资源转移置空源对象；三五法则/零法则；Rule of Five。',
     '说明移动后源对象处于有效但未指定状态。', 'hard'::question_difficulty, ARRAY['技术知识','C++','移动语义']::text[]),
    ('后端','C++','【C++｜OOP】虚函数表与动态绑定的开销？析构函数为何常声明为 virtual？',
     'vptr/vtable 间接调用与缓存不友好；多态基类虚析构保证派生资源释放。',
     'final 与 override 关键字。', 'medium'::question_difficulty, ARRAY['技术知识','C++','虚函数']::text[]),
    ('后端','C++','【C++｜模板】typename 与 class 在模板参数中的区别？SFINAE 大致指什么？',
     '在模板上下文中 typename 可标明依赖名为类型；SFINAE：替换失败非错误，用于重载决议与 enable_if。',
     '可提 concepts（C++20）。', 'hard'::question_difficulty, ARRAY['技术知识','C++','模板']::text[]),

    -- ========== 后端 / 数据库 ==========
    ('后端','数据库','【数据库｜锁】InnoDB 行锁、间隙锁、next-key 锁分别在什么语句下出现？',
     'RC 多快照读；RR 当前读可能加记录锁+间隙锁形成 next-key 防幻读；插入意向锁等。',
     '结合 SELECT ... FOR UPDATE 与范围条件举例。', 'hard'::question_difficulty, ARRAY['技术知识','MySQL','锁']::text[]),
    ('后端','数据库','【数据库｜架构】读写分离下主从延迟导致读不到新数据，业务侧常见对策？',
     '关键读走主库、会话粘主、延迟探测后重试、缓存旁路配合版本号、半同步复制降低延迟。',
     '说明「写完立刻读」场景的权衡。', 'medium'::question_difficulty, ARRAY['技术知识','MySQL','主从']::text[]),
    ('后端','数据库','【数据库｜扩展】分库分表后全局唯一 ID 与跨分片查询的常见方案？',
     '雪花算法、号段、Redis；ES/宽表/联邦查询；订单按用户+订单双路由；避免跨分片 join。',
     '提到冷热归档与二次拆分成本。', 'hard'::question_difficulty, ARRAY['技术知识','MySQL','分库分表']::text[]),

    -- ========== 后端 / 中间件 ==========
    ('后端','中间件','【中间件｜消息】RabbitMQ 的 exchange、routing key、queue 绑定模型？',
     'direct/topic/fanout/headers；消息经 exchange 路由到 queue；消费者从 queue 拉取或推送。',
     '与 Kafka topic-partition 模型对比。', 'medium'::question_difficulty, ARRAY['技术知识','RabbitMQ','消息']::text[]),
    ('后端','中间件','【中间件｜搜索】Elasticsearch 倒排索引大致结构？分词与分析器的作用？',
     'term -> posting list；analyzer 将文本切词并可能小写/去停用词；mapping 决定是否 keyword/text。',
     '说明精确匹配与全文检索字段选型。', 'medium'::question_difficulty, ARRAY['技术知识','ES','搜索']::text[]),
    ('后端','中间件','【中间件｜缓存】Redis 常见数据结构及其典型使用场景？',
     'string 计数缓存；hash 对象字段；list 队列；set 去重/集合运算；zset 排行榜/延迟队列；bitmap/hyperloglog 统计。',
     '提到 big key 与热 key 治理。', 'easy'::question_difficulty, ARRAY['技术知识','Redis','数据结构']::text[]),

    -- ========== 后端 / 分布式 ==========
    ('后端','分布式','【分布式｜锁】基于 Redis 的分布式锁要注意什么？Redisson 或 RedLock 争议点？',
     'SET NX PX、值+Lua 释放防误删、续期看门狗；时钟漂移与主从切换下锁丢失风险；业务幂等仍需要。',
     '说明与 etcd/ZooKeeper 锁的差异。', 'hard'::question_difficulty, ARRAY['技术知识','分布式锁','Redis']::text[]),
    ('后端','分布式','【分布式｜共识】Raft 选主与日志复制大致流程？脑裂如何避免？',
     '任期 term、投票、日志匹配；多数派提交；旧主未获多数则降级；日志压缩与快照。',
     '对比 Paxos 可理解成本。', 'hard'::question_difficulty, ARRAY['技术知识','Raft','共识']::text[]),
    ('后端','分布式','【分布式｜路由】分片键如何选择？热点分片如何缓解？',
     '按业务访问模式选高基数、均匀字段；避免单调递增主键做唯一分片键；随机后缀、分段、二级映射、异步搬迁。',
     '提到一致性哈希与虚拟节点。', 'medium'::question_difficulty, ARRAY['技术知识','分片','路由']::text[]),

    -- ========== 前端 / JavaScript/TypeScript ==========
    ('前端','JavaScript/TypeScript','【JS｜异步】Promise.then 链与 async/await 错误传播差异？未捕获 rejection 会怎样？',
     'await 同步 try/catch；then 第二参数或末尾 catch；未处理 rejection 可能 unhandledrejection。',
     '对比 Promise.all 与 allSettled。', 'medium'::question_difficulty, ARRAY['技术知识','JS','Promise']::text[]),
    ('前端','JavaScript/TypeScript','【JS｜语言】原型链是什么？ES class 语法糖与原型关系？',
     '对象 __proto__ 指向构造函数的 prototype；class 方法仍在 prototype；extends 设置原型链与 super。',
     '箭头函数不能做构造函数。', 'medium'::question_difficulty, ARRAY['技术知识','JS','原型']::text[]),
    ('前端','JavaScript/TypeScript','【TS｜类型】泛型约束 extends keyof 常见用法？条件类型 infer 解决什么问题？',
     '约束泛型可访问属性；条件类型分发；infer 从结构中提取类型（如 ReturnType 实现思路）。',
     '说明 any 与 unknown 在泛型中的安全差异。', 'hard'::question_difficulty, ARRAY['技术知识','TS','泛型']::text[]),

    -- ========== 前端 / Vue ==========
    ('前端','Vue','【Vue｜组件】scoped CSS 原理？深度选择器 :deep 适用场景？',
     '编译为带 data 属性的选择器；:deep 穿透子组件根；注意与 CSS Modules、原子类混用。',
     'Vue3 组合式 API 与样式隔离。', 'medium'::question_difficulty, ARRAY['技术知识','Vue','样式']::text[]),
    ('前端','Vue','【Vue｜内置】Teleport 解决什么问题？与 modal 层级、SSR 注意点？',
     '将 DOM 挂到 body 等容器避免 z-index/overflow 裁剪；SSR 需保证目标节点存在。',
     '对比 React Portal。', 'easy'::question_difficulty, ARRAY['技术知识','Vue','Teleport']::text[]),
    ('前端','Vue','【Vue｜状态】Pinia 与 Vuex4 相比模块化的改进点？store 之间如何组合？',
     '更轻、无 mutations 可选项、TS 友好；useStore 组合；可拆分 store 互相 import。',
     '持久化插件思路。', 'medium'::question_difficulty, ARRAY['技术知识','Vue','Pinia']::text[]),

    -- ========== 前端 / React ==========
    ('前端','React','【React｜Hooks】useEffect 闭包陈旧（stale closure）经典例子与修复？',
     '依赖数组遗漏导致读到旧 state/props；修复：补依赖、函数式 setState、useRef 存最新值、useEvent 模式。',
     'eslint exhaustive-deps 的意义。', 'medium'::question_difficulty, ARRAY['技术知识','React','Hooks']::text[]),
    ('前端','React','【React｜原理】Fiber 架构要解决什么问题？可中断更新与优先级大致如何工作？',
     '解决同步递归渲染阻塞；工作单元可暂停恢复；lane/priority 调度；并发特性基础。',
     '不必手写调度算法，讲清目标即可。', 'hard'::question_difficulty, ARRAY['技术知识','React','Fiber']::text[]),
    ('前端','React','【React｜性能】memo、useMemo、useCallback 各自缓存什么？滥用后果？',
     'memo 缓存组件渲染；useMemo 缓存计算；useCallback 缓存函数引用；过度使用增加比较成本与心智负担。',
     '说明 props 引用稳定与列表 key。', 'medium'::question_difficulty, ARRAY['技术知识','React','性能']::text[]),

    -- ========== 前端 / 工程化 ==========
    ('前端','工程化','【工程化｜构建】Webpack 与 Vite 在开发态热更新路径上的核心差异？',
     'Webpack bundle 后 HMR；Vite 利用 ESM 原生模块与 esbuild 预构建，按需编译更快。',
     '生产构建 Vite 仍用 Rollup。', 'medium'::question_difficulty, ARRAY['技术知识','构建','Vite']::text[]),
    ('前端','工程化','【工程化｜质量】如何在前端 CI 中做增量 lint/test？',
     'lint-staged、jest --onlyChanged、turbo/affected、缓存 node_modules 与构建产物；PR 门禁与主分支全量。',
     '提到 typecheck 与 bundle analyze 分阶段。', 'medium'::question_difficulty, ARRAY['技术知识','CI','前端']::text[]),
    ('前端','工程化','【工程化｜模块化】动态 import() 与路由懒加载如何影响首包体积？',
     '代码分割 chunk；预加载 prefetch/preload 策略；避免瀑布式串行加载。',
     '与 SSR/流式渲染的关系可简提。', 'medium'::question_difficulty, ARRAY['技术知识','代码分割']::text[]),

    -- ========== 前端 / 性能优化 ==========
    ('前端','性能优化','【性能｜资源】图片现代格式 AVIF/WebP 与响应式图片 srcset 如何选型？',
     '按浏览器支持降级；srcset/sizes 适配 DPR 与布局；CDN 裁剪参数与懒加载 decoding=async。',
     '说明 LCP 图片优先 fetchpriority。', 'medium'::question_difficulty, ARRAY['技术知识','图片','性能']::text[]),
    ('前端','性能优化','【性能｜运行时】如何减少长任务（Long Task）？Scheduler API 与分片渲染思路？',
     '拆分为 requestIdleCallback/scheduler.postTask；虚拟列表；Web Worker  offload；React concurrent 模式。',
     '用 Performance 面板验证。', 'hard'::question_difficulty, ARRAY['技术知识','长任务','性能']::text[]),
    ('前端','性能优化','【性能｜指标】如何定位与修复 CLS（累积布局偏移）？',
     '图片/广告预留宽高、字体 fallback、避免动态插入顶栏；CLS 公式与会话窗口。',
     '与 skeleton 占位配合。', 'medium'::question_difficulty, ARRAY['技术知识','CLS','Web Vitals']::text[]),

    -- ========== 前端 / Node.js ==========
    ('前端','Node.js','【Node｜原理】Node 事件循环各阶段顺序？process.nextTick 与微任务队列关系？',
     'timers -> pending -> poll -> check -> close；nextTick 优先于其他微任务；注意阻塞 poll。',
     '与浏览器微任务差异。', 'hard'::question_difficulty, ARRAY['技术知识','Node','事件循环']::text[]),
    ('前端','Node.js','【Node｜IO】Stream 的 pipe 与 backpressure 机制？',
     '可读流 push 返回 false 时应暂停读；pipe 自动处理背压；highWaterMark 调优。',
     '对比一次性 readFile 大文件风险。', 'medium'::question_difficulty, ARRAY['技术知识','Node','Stream']::text[]),
    ('前端','Node.js','【Node｜部署】cluster 模块与 worker_threads 分别适合什么场景？',
     'cluster 多进程利用多核处理 IO；worker_threads 适合 CPU 密集且要共享 ArrayBuffer；注意进程间通信成本。',
     '现代可用 PM2 或容器水平扩展替代部分场景。', 'medium'::question_difficulty, ARRAY['技术知识','Node','多进程']::text[]),

    -- ========== 算法 / 数据结构 ==========
    ('算法','数据结构','【数据结构｜字典树】Trie 的插入与查询复杂度？相比哈希表优缺点？',
     'O(L) 长度相关；前缀搜索、自动补全强；空间可能稀疏；哈希无前缀能力。',
     '可提压缩 Trie。', 'medium'::question_difficulty, ARRAY['技术知识','Trie','字符串']::text[]),
    ('算法','数据结构','【数据结构｜并查集】路径压缩与按秩合并的目的？均摊复杂度量级？',
     '降低树高；近似 O(α(n)) 反阿克曼；用于 Kruskal、连通分量。',
     '说明 union-find 接口。', 'medium'::question_difficulty, ARRAY['技术知识','并查集']::text[]),
    ('算法','数据结构','【数据结构｜跳表】跳表如何实现有序 Map？与平衡树相比工程上优势？',
     '多层索引随机层高；期望 O(log n) 查找插入；实现较红黑树简单；Redis zset 底层之一。',
     '说明随机层与最坏情况。', 'hard'::question_difficulty, ARRAY['技术知识','跳表']::text[]),

    -- ========== 算法 / 动态规划 ==========
    ('算法','动态规划','【DP｜区间】区间 DP 的状态一般如何定义？石子合并或括号串类题思路？',
     'dp[i][j] 表示区间 [i,j] 最优；枚举分割点 k；注意长度从小到大填表。',
     '与矩阵链乘类比。', 'hard'::question_difficulty, ARRAY['技术知识','动态规划','区间DP']::text[]),
    ('算法','动态规划','【DP｜树】树形 DP 在「没有上司的舞会」类问题上的状态设计？',
     '节点选/不选最大权独立集；dfs 后序合并子树信息；注意后效性处理。',
     '推广到背包 on tree。', 'hard'::question_difficulty, ARRAY['技术知识','动态规划','树形DP']::text[]),
    ('算法','动态规划','【DP｜计数】网格路径计数（无障碍/有障碍）状态转移与边界？',
     'dp[i][j] 从左上到 (i,j) 路径数；障碍为 0；第一行第一列初始化。',
     '取模防溢出。', 'easy'::question_difficulty, ARRAY['技术知识','动态规划','计数']::text[]),

    -- ========== 算法 / 图与搜索 ==========
    ('算法','图与搜索','【图｜最小生成树】Kruskal 与 Prim 思想及适用图稠密/稀疏？',
     'Kruskal 边排序+并查集 O(E log E)；Prim 堆优化 O(E log V)；稀疏图 Kruskal 常友好。',
     '说明环检测。', 'medium'::question_difficulty, ARRAY['技术知识','图','MST']::text[]),
    ('算法','图与搜索','【图｜拓扑】拓扑排序检测有向环的方法？',
     'Kahn 入度为 0 入队，计数访问边；若处理节点数 < V 则有环；或 DFS 三色标记。',
     '应用场景：编译依赖、任务调度。', 'medium'::question_difficulty, ARRAY['技术知识','图','拓扑']::text[]),
    ('算法','图与搜索','【图｜二分图】如何判断二分图？染色法细节？',
     'BFS/DFS 二色染色，冲突则非二分；等价无奇环；匹配问题基础。',
     '时间复杂度 O(V+E)。', 'easy'::question_difficulty, ARRAY['技术知识','图','二分图']::text[]),

    -- ========== 算法 / 排序与双指针 ==========
    ('算法','排序与双指针','【排序｜逆序对】归并排序求逆序对的核心观察？',
     '合并时右半小于左半产生跨区间逆序对；O(n log n)。',
     '与树状数组方案对比。', 'medium'::question_difficulty, ARRAY['技术知识','排序','逆序对']::text[]),
    ('算法','排序与双指针','【双指针｜数组】荷兰国旗问题（三色排序）单趟指针移动规则？',
     '三指针 lt/i/gt 分区 0/1/2；i 遇到 1 前进，0 与 lt 交换，2 与 gt 交换且不急着 i++。',
     '原地 O(n)。', 'medium'::question_difficulty, ARRAY['技术知识','双指针','排序']::text[]),
    ('算法','排序与双指针','【双指针｜滑动窗口】固定窗口与可变窗口求最值/最短子串的模板差异？',
     '固定窗口右移维护窗口状态；可变窗口右扩左收满足单调条件；哈希或 deque 维护最值。',
     '注意窗口合法性判定。', 'medium'::question_difficulty, ARRAY['技术知识','滑动窗口']::text[]),

    -- ========== 算法 / 并发与锁（算法题） ==========
    ('算法','并发与锁（算法题）','【并发｜同步】三个线程交替打印 ABC… 各打印 n 轮，如何用锁或信号量建模？',
     '三把锁/三个信号量轮转；或一个互斥锁+条件变量+轮次变量；避免忙等。',
     '可扩展到打印 1-100 三线程分工。', 'hard'::question_difficulty, ARRAY['场景题','线程同步']::text[]),
    ('算法','并发与锁（算法题）','【并发｜经典】哲学家就餐问题如何避免死锁？',
     '限制同时拿叉人数、按序申请资源、奇偶哲学家不同顺序；资源分级。',
     '联系数据库死锁预防。', 'medium'::question_difficulty, ARRAY['技术知识','死锁','并发']::text[]),
    ('算法','并发与锁（算法题）','【并发｜无锁】CAS 自旋实现计数器的问题？ABA 如何解决？',
     '忙等浪费 CPU、高竞争性能差；ABA 用版本号/StampedReference；适合低竞争短临界区。',
     '对比 synchronized 与 Atomic*。', 'hard'::question_difficulty, ARRAY['技术知识','CAS','无锁']::text[]),

    -- ========== DevOps / Docker ==========
    ('DevOps','Docker','【Docker｜网络】bridge/host/none 网络模式各适用场景？',
     'bridge 默认 NAT 隔离；host 共享宿主机网络栈性能最好但端口冲突；none 无网络。',
     '自定义网络 DNS 服务发现。', 'medium'::question_difficulty, ARRAY['技术知识','Docker','网络']::text[]),
    ('DevOps','Docker','【Docker｜镜像】多阶段构建（multi-stage）解决什么问题？',
     '减小最终镜像体积、分离构建依赖与运行环境；只拷贝二进制/静态资源。',
     '与 distroless 基础镜像。', 'medium'::question_difficulty, ARRAY['技术知识','Docker','镜像']::text[]),
    ('DevOps','Docker','【Docker｜安全】容器以 root 运行的风险与缓解？',
     '逃逸面扩大；用非 root USER、read-only rootfs、cap-drop、seccomp；最小镜像。',
     '与 K8s securityContext 配合。', 'medium'::question_difficulty, ARRAY['技术知识','Docker','安全']::text[]),

    -- ========== DevOps / Kubernetes ==========
    ('DevOps','Kubernetes','【K8s｜网络】Service 的 ClusterIP、NodePort、LoadBalancer 差异？Ingress 解决什么？',
     'ClusterIP 集群内；NodePort 暴露节点端口；LoadBalancer 云 LB；Ingress L7 路由 TLS。',
     '提到 kube-proxy 模式 iptables/ipvs。', 'medium'::question_difficulty, ARRAY['技术知识','K8s','网络']::text[]),
    ('DevOps','Kubernetes','【K8s｜弹性】HPA 依据哪些指标扩缩？VPA 与 HPA 冲突时注意什么？',
     'CPU/内存/custom metrics；VPA 调资源请求可能与 HPA 同时震荡需策略分离或分环境。',
     'KEDA 事件驱动扩缩。', 'medium'::question_difficulty, ARRAY['技术知识','K8s','HPA']::text[]),
    ('DevOps','Kubernetes','【K8s｜安全】RBAC 中 Role 与 ClusterRole、Binding 关系？',
     'Role 命名空间作用域；ClusterRole 全集群；RoleBinding/ClusterRoleBinding 绑定主体。',
     '最小权限原则。', 'medium'::question_difficulty, ARRAY['技术知识','K8s','RBAC']::text[]),

    -- ========== DevOps / Linux ==========
    ('DevOps','Linux','【Linux｜系统】systemd 单元类型 service、timer、target 用途？',
     'service 长期运行；timer 替代 cron；target 组依赖与启动级别。',
     'journalctl 查日志。', 'medium'::question_difficulty, ARRAY['技术知识','Linux','systemd']::text[]),
    ('DevOps','Linux','【Linux｜资源】cgroup v2 能限制哪些资源？与容器关系？',
     'CPU、内存、IO、PID；容器运行时通过 cgroup 隔离；OOM 行为与 memory.max。',
     '对比 namespace。', 'hard'::question_difficulty, ARRAY['技术知识','Linux','cgroup']::text[]),
    ('DevOps','Linux','【Linux｜网络】如何用 ss 与 tcpdump 快速判断连接问题？',
     'ss 看状态、队列、进程；tcpdump 过滤 SYN/重传；结合 mtr 看路径。',
     'TIME_WAIT 过多处理思路。', 'medium'::question_difficulty, ARRAY['技术知识','Linux','网络']::text[]),

    -- ========== DevOps / CI/CD ==========
    ('DevOps','CI/CD','【CI/CD｜发布】蓝绿发布与金丝雀（灰度）在流量切换与回滚上的差异？',
     '蓝绿两套环境瞬时切换、资源占用高；金丝雀逐步放量、观察指标再全量；回滚速度与风险不同。',
     '结合网关/服务网格流量权重。', 'medium'::question_difficulty, ARRAY['技术知识','发布','灰度']::text[]),
    ('DevOps','CI/CD','【CI/CD｜GitOps】Git 作为单一事实来源的利弊？',
     '审计与回滚友好；声明式 desired state；需保护 main、签名与权限；漂移检测。',
     'Argo CD / Flux 思路。', 'medium'::question_difficulty, ARRAY['技术知识','GitOps']::text[]),
    ('DevOps','CI/CD','【CI/CD｜流水线】如何设计「构建一次，多处部署」的制品管理？',
     '不可变制品版本化；镜像 digest 固定；配置与密钥外置；环境差异通过配置中心而非改包。',
     'SBOM 与漏洞扫描卡点。', 'medium'::question_difficulty, ARRAY['技术知识','CI','制品']::text[]),

    -- ========== DevOps / 监控与可观测性 ==========
    ('DevOps','监控与可观测性','【可观测｜指标】RED 与 USE 方法分别关注什么？',
     'RED：Rate Errors Duration 面向服务；USE：Utilization Saturation Errors 面向资源。',
     '与四大黄金信号关系。', 'medium'::question_difficulty, ARRAY['技术知识','监控','RED']::text[]),
    ('DevOps','监控与可观测性','【可观测｜Prometheus】高基数 label 会导致什么问题？如何避免？',
     '时间序列爆炸、内存暴涨；避免 user_id 作 label；用 recording rule 或日志追踪个体。',
     'histogram vs summary 选型。', 'hard'::question_difficulty, ARRAY['技术知识','Prometheus']::text[]),
    ('DevOps','监控与可观测性','【可观测｜Tracing】分布式追踪中 trace、span、context propagation 指什么？',
     'trace 全链路；span 单次调用；通过 HTTP/gRPC header 传递 trace id；与日志关联。',
     'OpenTelemetry 统一模型。', 'medium'::question_difficulty, ARRAY['技术知识','链路追踪']::text[]),

    -- ========== 技术管理 / 项目管理 ==========
    ('技术管理','项目管理','【项管｜计划】WBS 工作分解结构的作用？到什么粒度合适？',
     '可估算、可分配、可验证的交付单元；过细管理成本高，过粗失控；与里程碑映射。',
     '结合敏捷 backlog。', 'easy'::question_difficulty, ARRAY['技术知识','项目管理','WBS']::text[]),
    ('技术管理','项目管理','【项管｜风险】风险登记册应包含哪些字段？定性分析与应对策略类型？',
     '描述、概率、影响、负责人、应对（规避/减轻/转移/接受）；优先级矩阵。',
     '定期复盘更新。', 'medium'::question_difficulty, ARRAY['技术知识','风险']::text[]),
    ('技术管理','项目管理','【项管｜敏捷】燃尽图异常平台期可能原因？如何与干系人沟通？',
     '需求蔓延、估算偏差、技术债、依赖阻塞；透明原因、调整范围或资源、重置基线需正式变更。',
     '避免隐瞒。', 'medium'::question_difficulty, ARRAY['技术知识','敏捷','燃尽图']::text[]),

    -- ========== 技术管理 / 团队管理 ==========
    ('技术管理','团队管理','【团管｜1:1】高效一对一会议应覆盖哪些主题？频率与记录建议？',
     '目标对齐、阻塞、反馈、职业发展、团队氛围；双周或按需；行动项跟进。',
     '避免只聊项目进度。', 'medium'::question_difficulty, ARRAY['技术知识','团队管理','1:1']::text[]),
    ('技术管理','团队管理','【团管｜绩效】如何给出建设性负面反馈（SBI 模型）？',
     'Situation-Behavior-Impact；对事不对人；及时；共定改进计划与检查点。',
     '与绩效结果分离日常反馈。', 'medium'::question_difficulty, ARRAY['技术知识','反馈','绩效']::text[]),
    ('技术管理','团队管理','【团管｜梯队】技术梯队建设中「骨干 vs 专家」路径如何区分培养？',
     '骨干偏交付与带小团队；专家偏深度与横向影响；双通道职级；项目历练与外部交流。',
     '避免单一晋升标准。', 'medium'::question_difficulty, ARRAY['技术知识','梯队','培养']::text[]),

    -- ========== 技术管理 / 架构评审 ==========
    ('技术管理','架构评审','【架评｜流程】架构评审应至少回答哪几类问题？',
     '业务目标与约束、方案对比、非功能需求（性能/可用/安全/成本）、风险与回滚、演进路线。',
     '输出结论与 action owner。', 'medium'::question_difficulty, ARRAY['技术知识','架构评审']::text[]),
    ('技术管理','架构评审','【架评｜ADR】Architecture Decision Record 建议记录哪些内容？',
     '背景、决策、备选方案与取舍、后果；版本化随代码库管理；便于后人理解「为何如此」。',
     '与 RFC 文档关系。', 'easy'::question_difficulty, ARRAY['技术知识','ADR']::text[]),
    ('技术管理','架构评审','【架评｜容量】如何做粗略容量估算（QPS、存储、带宽）？',
     '峰值与增长系数；读写分离与缓存命中；磁盘与副本；压测验证假设。',
     '说明数量级即可。', 'medium'::question_difficulty, ARRAY['技术知识','容量','估算']::text[]),

    -- ========== 技术管理 / 沟通协作 ==========
    ('技术管理','沟通协作','【沟通｜冲突】跨部门资源冲突时，如何推动决策？',
     '共同目标对齐、数据化影响、升级路径清晰、书面结论与优先级；避免人身攻击。',
     'RACI 明确责任。', 'medium'::question_difficulty, ARRAY['场景题','沟通','冲突']::text[]),
    ('技术管理','沟通协作','【沟通｜向上】向高层汇报技术风险时应注意什么？',
     '结论先行、影响业务翻译、备选方案与所需支持、时间线；少术语或附一页解释。',
     '诚实不粉饰。', 'medium'::question_difficulty, ARRAY['技术知识','向上管理']::text[]),
    ('技术管理','沟通协作','【沟通｜文档】技术方案文档的读者分层如何写？',
     '执行摘要给决策层；架构图与接口给实现层；运维与 SLO 给平台；附录细节。',
     '版本与变更记录。', 'easy'::question_difficulty, ARRAY['技术知识','文档']::text[]),

    -- ========== 技术管理 / 招聘与培养 ==========
    ('技术管理','招聘与培养','【招聘｜面试】行为面试 STAR 法则如何拆解？技术岗如何防「背题」？',
     'Situation Task Action Result；追问细节、数据、取舍、失败复盘；现场小设计或代码。',
     '结构化评分表减偏见。', 'medium'::question_difficulty, ARRAY['技术知识','招聘','面试']::text[]),
    ('技术管理','招聘与培养','【招聘｜渠道】校招与社招在考察重点上应有何不同？',
     '校招潜力、基础、学习曲线；社招即战力、领域经验、工程成熟度；实习转正降低错配。',
     '文化契合评估方式。', 'medium'::question_difficulty, ARRAY['技术知识','招聘']::text[]),
    ('技术管理','招聘与培养','【培养｜新人】新员工 30-60-90 计划一般包含什么？',
     '环境融入、代码规范、首个小需求交付、导师反馈；60 独立模块；90 承担更大责任；检查清单。',
     '可衡量产出与反馈节奏。', 'easy'::question_difficulty, ARRAY['技术知识','培养','入职']::text[])
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
  'seed_questions_per_job_category'
FROM resolved r
WHERE NOT EXISTS (
  SELECT 1 FROM questions q
  WHERE q.deleted_at IS NULL
    AND q.bank_id = r.bank_id
    AND q.content = r.content
);

COMMIT;
