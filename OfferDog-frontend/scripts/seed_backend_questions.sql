-- 后端开发题目 (bank_id=41-45)
INSERT INTO questions (bank_id, category_id, content, answer, tips, difficulty, tags, created_by, stats, remark, created_at, updated_at) VALUES
-- Java基础 (bank_id=41)
(41, 5, 'Java的面向对象特性有哪些?封装、继承、多态的实现原理?', 
'Java面向对象特性:
1. 封装:通过private修饰符和getter/setter方法实现
2. 继承:通过extends关键字实现代码复用
3. 多态:通过方法重写和动态绑定实现

实现原理:
- 封装:访问控制和方法封装
- 继承:继承机制和super关键字
- 多态:动态绑定和虚方法表', 
'可以结合代码示例说明', 'easy', '{"Java", "面向对象", "核心概念"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

(41, 5, 'Java的集合框架有哪些?ArrayList和LinkedList有什么区别?', 
'Java集合框架:
1. Collection: List、Set
2. Map: HashMap、TreeMap、HashTable

ArrayList和LinkedList区别:
- ArrayList:基于数组,查询快,增删慢
- LinkedList:基于链表,查询慢,增删快
- 线程安全:ArrayList非线程安全,Vector线程安全', 
'可以从底层实现和使用场景回答', 'medium', '{"Java", "集合", "数据结构"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(41, 5, 'Java的线程池有哪些类型?如何合理使用线程池?', 
'Java线程池类型:
1. FixedThreadPool:固定大小线程池
2. CachedThreadPool:可缓存线程池
3. ScheduledThreadPool:定时任务线程池
4. SingleThreadExecutor:单线程池

使用原则:
1. 根据任务类型选择线程池
2. 合理设置线程数量
3. 设置合理的队列大小
4. 处理拒绝策略', 
'可以结合实际场景说明', 'medium', '{"Java", "线程池", "并发"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(41, 5, 'Java的垃圾回收机制是什么?GC算法有哪些?', 
'垃圾回收机制:
1. 分代收集:新生代、老年代、永久代
2. GC算法:标记-清除、复制、标记-整理
3. GC收集器:Serial、ParNew、Parallel Scavenge、CMS、G1

G1特点:
- 区域化、分区管理
- 可预测的停顿时间
- 并行与并发', 
'可以从内存模型和GC流程回答', 'hard', '{"Java", "GC", "内存管理"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(41, 5, 'Java的SPI机制是什么?如何实现?', 
'SPI(Service Provider Interface)是Java的服务发现机制。
实现步骤:
1. 定义服务接口
2. 创建实现类
3. 在META-INF/services下创建配置文件
4. 使用ServiceLoader加载服务

应用场景:
- JDBC驱动加载
- 日志框架实现
- 插件化开发', 
'可以结合代码示例', 'medium', '{"Java", "SPI", "设计模式"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '进阶考点', NOW(), NOW()),

-- Spring框架 (bank_id=42)
(42, 5, 'Spring的IoC容器原理是什么?Bean的生命周期?', 
'IoC容器原理:
1. 读取配置元数据
2. 注册Bean定义
3. 创建Bean实例
4. 依赖注入
5. 初始化和销毁

Bean生命周期:
1. 实例化
2. 属性赋值
3. 设置BeanName
4. 设置BeanFactory
5. 初始化前处理
6. 初始化
7. 初始化后处理
8. 使用
9. 销毁前处理
10. 销毁', 
'可以画生命周期图', 'medium', '{"Spring", "IoC", "Bean生命周期"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(42, 5, 'Spring的AOP原理是什么?有哪些通知类型?', 
'AOP(面向切面编程)原理:
1. 动态代理:JDK代理和CGLIB代理
2. 切面织入:在运行时将切面应用到目标对象

通知类型:
1. 前置通知:Before
2. 后置通知:AfterReturning
3. 异常通知:AfterThrowing
4. 最终通知:After
5. 环绕通知:Around', 
'可以结合代理模式说明', 'medium', '{"Spring", "AOP", "代理"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(42, 5, 'Spring事务传播机制有哪些?@Transactional注解的使用?', 
'事务传播机制:
1. REQUIRED:支持当前事务,不存在则创建
2. REQUIRES_NEW:创建新事务,挂起当前事务
3. NESTED:在当前事务中创建嵌套事务
4. SUPPORTS:支持当前事务,不存在则非事务执行
5. NOT_SUPPORTED:非事务执行,挂起当前事务
6. NEVER:非事务执行,存在事务则抛出异常
7. MANDATORY:支持当前事务,不存在则抛出异常

@Transactional使用:
1. 指定传播行为
2. 设置隔离级别
3. 指定回滚规则
4. 超时设置', 
'可以结合实际场景说明', 'hard', '{"Spring", "事务", "传播机制"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(42, 5, 'Spring Boot自动装配原理是什么?如何自定义Starter?', 
'自动装配原理:
1. @SpringBootApplication启用自动配置
2. @EnableAutoConfiguration导入自动配置
3. 读取META-INF/spring.factories
4. 条件装配:ConditionalOnClass、ConditionalOnMissingBean等

自定义Starter:
1. 创建自动配置类
2. 配置属性类
3. 创建spring.factories
4. 提供默认实现', 
'可以结合代码示例', 'hard', '{"Spring Boot", "自动装配", "Starter"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(42, 5, 'Spring Cloud微服务组件有哪些?各组件的作用?', 
'Spring Cloud组件:
1. Eureka:服务注册与发现
2. Ribbon:客户端负载均衡
3. Feign:声明式HTTP客户端
4. Hystrix:熔断器
5. Zuul/Gateway:API网关
6. Config:配置中心
7. Bus:消息总线
8. Sleuth:链路追踪', 
'可以结合架构图说明', 'medium', '{"Spring Cloud", "微服务", "组件"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

-- 数据库 (bank_id=43)
(43, 5, 'MySQL的索引类型有哪些?聚簇索引和非聚簇索引的区别?', 
'MySQL索引类型:
1. 主键索引
2. 唯一索引
3. 普通索引
4. 全文索引
5. 组合索引

聚簇索引和非聚簇索引区别:
- 聚簇索引:数据和索引存储在一起,InnoDB主键索引
- 非聚簇索引:索引和数据分离,InnoDB二级索引
- MyISAM:所有索引都是非聚簇索引', 
'可以从B+树结构回答', 'medium', '{"MySQL", "索引", "存储"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(43, 5, 'MySQL的事务隔离级别有哪些?各隔离级别解决什么问题?', 
'事务隔离级别:
1. READ UNCOMMITTED:读未提交,存在脏读
2. READ COMMITTED:读已提交,解决脏读,存在不可重复读
3. REPEATABLE READ:可重复读,解决脏读和不可重复读,存在幻读
4. SERIALIZABLE:串行化,解决所有问题

InnoDB默认:REPEATABLE READ
通过MVCC和间隙锁解决幻读', 
'可以从并发问题回答', 'medium', '{"MySQL", "事务", "隔离级别"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(43, 5, 'Redis的数据类型有哪些?应用场景?', 
'Redis数据类型:
1. String:字符串,计数器、缓存
2. Hash:哈希,对象存储
3. List:列表,消息队列、朋友圈
4. Set:集合,点赞、关注
5. Sorted Set:有序集合,排行榜

应用场景:
1. 缓存:热点数据缓存
2. 计数器:限流、统计
3. 消息队列:发布订阅
4. 排行榜:有序集合
5. 位图:用户签到', 
'可以结合实际场景说明', 'medium', '{"Redis", "数据类型", "应用"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(43, 5, 'Redis的持久化机制有哪些?RDB和AOF的区别?', 
'持久化机制:
1. RDB:快照模式,定期保存数据
2. AOF:追加日志,记录每个写操作

RDB和AOF区别:
- RDB:文件小,恢复快,可能丢失数据
- AOF:文件大,恢复慢,数据完整性好

混合持久化:RDB+AOF增量', 
'可以从数据安全性和性能平衡回答', 'medium', '{"Redis", "持久化", "RDB", "AOF"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(43, 5, '数据库优化有哪些方法?SQL优化实践?', 
'数据库优化方法:
1. 索引优化:合理创建索引
2. 查询优化:避免SELECT *,使用EXPLAIN分析
3. 分表分库:水平分表、垂直分库
4. 读写分离:主从复制
5. 缓存:Redis缓存热点数据

SQL优化实践:
1. 避免全表扫描
2. 使用覆盖索引
3. 避免NULL值判断
4. 使用批量操作
5. 合理使用JOIN', 
'可以结合实际项目经验回答', 'hard', '{"数据库", "优化", "SQL"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

-- 微服务 (bank_id=44)
(44, 5, '微服务架构有哪些优缺点?如何解决微服务的问题?', 
'微服务优缺点:
优点:
1. 技术栈灵活
2. 独立部署
3. 故障隔离
4. 扩展性强
5. 团队自治

缺点:
1. 分布式复杂性
2. 数据一致性
3. 运维成本
4. 测试复杂

解决方案:
1. 服务注册与发现
2. 配置中心
3. 熔断降级
4. 链路追踪
5. 分布式事务', 
'可以结合实际项目经验回答', 'medium', '{"微服务", "架构", "问题"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(44, 5, '分布式事务有哪些解决方案?Seata的使用?', 
'分布式事务解决方案:
1. 两阶段提交(2PC)
2. 三阶段提交(3PC)
3. TCC(Try-Confirm-Cancel)
4. 本地消息表
5. MQ事务消息
6. Saga模式

Seata使用:
1. AT模式:自动事务
2. TCC模式:手动控制
3. SAGA模式:长事务
4. XA模式:强一致性', 
'可以结合实际场景说明', 'hard', '{"微服务", "分布式事务", "Seata"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(44, 5, '服务注册与发现原理是什么?Eureka和Consul的区别?', 
'服务注册与发现原理:
1. 服务启动时注册到注册中心
2. 客户端从注册中心获取服务列表
3. 定期心跳续约
4. 服务下线时注销

Eureka和Consul区别:
- Eureka:AP原则,自我保护机制
- Consul:CP原则,强一致性
- Eureka:Netflix开源
- Consul:HashiCorp开源', 
'可以从CAP理论回答', 'medium', '{"微服务", "注册中心", "Eureka"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(44, 5, 'API网关的作用有哪些?常用的网关有哪些?', 
'API网关作用:
1. 请求路由
2. 负载均衡
3. 认证授权
4. 限流熔断
5. 日志监控
6. 协议转换

常用网关:
1. Kong:基于Nginx
2. API Gateway:AWS服务
3. Zuul:Netflix开源
4. Gateway:Spring Cloud
5. Nginx:反向代理', 
'可以结合架构图说明', 'medium', '{"微服务", "网关", "架构"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(44, 5, '服务熔断和降级原理是什么?Hystrix的使用?', 
'熔断原理:
1. 请求失败达到阈值
2. 熔断器打开
3. 直接返回降级结果
4. 半开状态尝试恢复

降级原理:
1. 服务不可用时返回默认值
2. 保证核心功能可用
3. 降级策略配置

Hystrix使用:
1. @HystrixCommand注解
2. fallbackMethod指定降级方法
3. 配置熔断参数', 
'可以结合代码示例', 'medium', '{"微服务", "熔断", "Hystrix"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

-- 后端工程化 (bank_id=45)
(45, 5, 'Docker的原理是什么?常用命令有哪些?', 
'Docker原理:
1. Namespace:隔离机制
2. Cgroups:资源限制
3. Union FS:联合文件系统

常用命令:
1. docker run:启动容器
2. docker ps:查看容器
3. docker build:构建镜像
4. docker pull/push:镜像管理
5. docker-compose:编排服务', 
'可以从容器化技术角度回答', 'easy', '{"Docker", "工程化", "容器"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

(45, 5, 'Kubernetes的核心概念有哪些?Pod的生命周期?', 
'Kubernetes核心概念:
1. Pod:最小调度单位
2. Service:服务发现
3. Deployment:部署管理
4. Namespace:命名空间
5. ConfigMap:配置管理

Pod生命周期:
1. Pending:正在创建
2. Running:运行中
3. Succeeded:成功完成
4. Failed:失败
5. Unknown:未知状态', 
'可以从容器编排角度回答', 'medium', '{"Kubernetes", "工程化", "容器"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(45, 5, 'CI/CD流程有哪些阶段?如何配置Jenkins?', 
'CI/CD流程:
1. 代码提交
2. 拉取代码
3. 编译构建
4. 运行测试
5. 部署发布

Jenkins配置:
1. 创建任务
2. 配置源码管理
3. 配置构建触发器
4. 配置构建步骤
5. 配置构建后操作', 
'可以结合实际项目经验回答', 'medium', '{"CI/CD", "Jenkins", "工程化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(45, 5, '日志管理有哪些最佳实践?ELK的使用?', 
'日志管理最佳实践:
1. 统一日志格式
2. 分级日志输出
3. 结构化日志
4. 日志采样
5. 敏感信息脱敏

ELK使用:
1. Elasticsearch:存储和搜索
2. Logstash:日志收集
3. Kibana:可视化
4. Filebeat:轻量级日志收集', 
'可以结合实际项目经验回答', 'medium', '{"日志", "ELK", "工程化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(45, 5, '服务监控有哪些指标?Prometheus的使用?', 
'服务监控指标:
1. CPU使用率
2. 内存使用
3. 磁盘IO
4. 网络IO
5. 请求延迟
6. 错误率
7. 并发数

Prometheus使用:
1. Metrics暴露
2. Pull方式采集
3. PromQL查询
4. Alertmanager告警
5. Grafana可视化', 
'可以结合监控体系回答', 'medium', '{"监控", "Prometheus", "工程化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW());
