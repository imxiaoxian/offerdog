-- Insert sample interview experiences
INSERT INTO interview_experience (company_name, position, experience_type, content, formatted_content, source, author, views, likes, created_by, created_at, updated_at) VALUES
('阿里巴巴', 'Java开发', 'full_time', 
 '一面：1.自我介绍 2.HashMap底层原理 3.concurrentHashMap如何保证线程安全 4.MySQL索引底层原理 5.事务隔离级别 6.项目中遇到的难点如何解决 项目：1.项目介绍 2.为什么要选用这个技术栈 3.如何优化接口性能 4.手撕：反转链表',
 '## 阿里巴巴面经

### 八股文
1. HashMap底层原理
2. concurrentHashMap如何保证线程安全
3. MySQL索引底层原理
4. 事务隔离级别
5. 项目中遇到的难点如何解决

### 项目/实习
1. 项目介绍
2. 为什么要选用这个技术栈
3. 如何优化接口性能

### 手撕
反转链表',
 'manual', '匿名', 0, 0, 1, NOW(), NOW()),

('腾讯', '后端开发', 'full_time',
 '一面：1.自我介绍 2.介绍项目 3.Redis数据类型 4.Redis持久化机制 5.分布式锁原理 6.CAP理论 7.手撕：两数之和 二面：1.项目中遇到的最大挑战 2.如何进行性能优化 3.数据库优化经验 4.手撕：二叉树层序遍历',
 '## 腾讯面经

### 八股文
1. Redis数据类型
2. Redis持久化机制
3. 分布式锁原理
4. CAP理论

### 项目/实习
1. 项目中遇到的最大挑战
2. 如何进行性能优化
3. 数据库优化经验

### 手撕
两数之和
二叉树层序遍历',
 'manual', '匿名', 0, 0, 1, NOW(), NOW()),

('字节跳动', 'Go后端', 'full_time',
 '一面：1.自我介绍 2.Golang GMP模型 3.Golang GC机制 4.K8s了解多少 5.MySQL事务隔离级别 6.手撕：LRU缓存 二面：1.项目架构设计 2.如何保证高可用 3.熔断限流实现 4.手撕：合并K个有序链表 三面：1.职业规划 2.为什么离职 3.反问环节',
 '## 字节跳动面经

### 八股文
1. Golang GMP模型
2. Golang GC机制
3. K8s了解多少
4. MySQL事务隔离级别

### 项目/实习
1. 项目架构设计
2. 如何保证高可用
3. 熔断限流实现

### 手撕
LRU缓存
合并K个有序链表',
 'manual', '匿名', 0, 0, 1, NOW(), NOW()),

('美团', 'Java后端', 'full_time',
 '一面：1.自我介绍 2.ArrayList和LinkedList区别 3.HashMap扩容机制 4.Synchronized和ReentrantLock区别 5.MySQL慢查询优化 6.手撕：二分查找 二面：1.项目介绍 2.秒杀系统设计 3.如何防止超卖 4.Redis缓存问题 5.手撕：排序算法',
 '## 美团面经

### 八股文
1. ArrayList和LinkedList区别
2. HashMap扩容机制
3. Synchronized和ReentrantLock区别
4. MySQL慢查询优化

### 项目/实习
1. 项目介绍
2. 秒杀系统设计
3. 如何防止超卖
4. Redis缓存问题

### 手撕
二分查找
排序算法',
 'manual', '匿名', 0, 0, 1, NOW(), NOW()),

('京东', '后端开发', 'full_time',
 '一面：1.自我介绍 2.Spring Boot启动流程 3.Spring Bean生命周期 4.MyBatis缓存 5.Redis数据结构 6.手撕：链表反转 二面：1.项目经验 2.分布式事务 3.Seata原理 4.如何保证数据一致性',
 '## 京东面经

### 八股文
1. Spring Boot启动流程
2. Spring Bean生命周期
3. MyBatis缓存
4. Redis数据结构

### 项目/实习
1. 项目经验
2. 分布式事务
3. Seata原理
4. 如何保证数据一致性

### 手撕
链表反转',
 'manual', '匿名', 0, 0, 1, NOW(), NOW()),

('百度', 'Java开发', 'full_time',
 '一面：1.自我介绍 2.JVM内存模型 3.垃圾回收算法 4.类加载机制 5.手撕：快速排序 二面：1.项目架构 2.微服务拆分 3.服务注册发现 4.配置中心',
 '## 百度面经

### 八股文
1. JVM内存模型
2. 垃圾回收算法
3. 类加载机制

### 项目/实习
1. 项目架构
2. 微服务拆分
3. 服务注册发现
4. 配置中心

### 手撕
快速排序',
 'manual', '匿名', 0, 0, 1, NOW(), NOW()),

('拼多多', '后端开发', 'full_time',
 '一面：1.自我介绍 2.HashMap底层 3.ConcurrentHashMap 4.volatile原理 5.JMM模型 6.手撕：判断链表有环 二面：1.项目难点 2.高并发优化 3.缓存雪崩 4.缓存击穿',
 '## 拼多多面经

### 八股文
1. HashMap底层
2. ConcurrentHashMap
3. volatile原理
4. JMM模型

### 项目/实习
1. 项目难点
2. 高并发优化
3. 缓存雪崩
4. 缓存击穿

### 手撕
判断链表有环',
 'manual', '匿名', 0, 0, 1, NOW(), NOW()),

('网易', 'Java开发', 'full_time',
 '一面：1.自我介绍 2.Spring源码看过吗 3.AOP原理 4.事务传播行为 5.手撕：堆排序 二面：1.项目介绍 2.消息队列使用 3.如何保证消息不丢失 4.系统设计',
 '## 网易面经

### 八股文
1. Spring源码看过吗
2. AOP原理
3. 事务传播行为

### 项目/实习
1. 项目介绍
2. 消息队列使用
3. 如何保证消息不丢失
4. 系统设计

### 手撕
堆排序',
 'manual', '匿名', 0, 0, 1, NOW(), NOW());
