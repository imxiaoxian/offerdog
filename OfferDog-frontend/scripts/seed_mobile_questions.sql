-- 移动开发题目 (bank_id=46-50)
INSERT INTO questions (bank_id, category_id, content, answer, tips, difficulty, tags, created_by, stats, remark, created_at, updated_at) VALUES
-- Android开发 (bank_id=46)
(46, 6, 'Android的四大组件是什么?各组件的作用?', 
'Android四大组件:
1. Activity:界面展示
2. Service:后台服务
3. BroadcastReceiver:广播接收
4. ContentProvider:数据共享

生命周期:
- Activity: onCreate, onStart, onResume, onPause, onStop, onDestroy
- Service: onCreate, onStartCommand, onDestroy
- BroadcastReceiver: onReceive', 
'可以结合生命周期图说明', 'easy', '{"Android", "四大组件", "基础"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

(46, 6, 'Android的布局有哪些?ConstraintLayout的优势?', 
'Android布局:
1. LinearLayout:线性布局
2. RelativeLayout:相对布局
3. FrameLayout:帧布局
4. GridLayout:网格布局
5. ConstraintLayout:约束布局

ConstraintLayout优势:
1. 性能优化,扁平化视图
2. 灵活的约束关系
3. 支持链式布局
4. 支持百分比布局', 
'可以从布局性能角度回答', 'medium', '{"Android", "布局", "ConstraintLayout"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(46, 6, 'Android的事件分发机制是什么?如何处理点击事件?', 
'事件分发机制:
1. MotionEvent:事件类型
2. 分发流程:Activity -> ViewGroup -> View
3. 处理流程:onInterceptTouchEvent -> onTouchEvent

点击事件处理:
1. onClickListener
2. onTouchListener
3. onTouchEvent
4. 自定义View处理', 
'可以结合事件分发流程图说明', 'hard', '{"Android", "事件分发", "触摸事件"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(46, 6, 'Android的内存泄漏有哪些原因?如何避免?', 
'内存泄漏原因:
1. 静态变量引用Context
2. 匿名内部类引用外部类
3. Handler引用Activity
4. 未取消异步任务
5. 未注销广播接收器

避免方法:
1. 使用Application Context
2. 使用弱引用
3. 及时取消异步任务
4. 注销广播接收器
5. 使用LeakCanary检测', 
'可以结合代码示例', 'medium', '{"Android", "内存泄漏", "优化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(46, 6, 'Android的性能优化有哪些方法?', 
'性能优化方法:
1. 布局优化:减少层级,使用ConstraintLayout
2. 内存优化:避免内存泄漏,合理使用缓存
3. 绘制优化:减少过度绘制
4. 网络优化:数据压缩,缓存策略
5. 电量优化:减少耗电操作', 
'可以从启动速度和运行时性能回答', 'medium', '{"Android", "性能优化", "优化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

-- iOS开发 (bank_id=47)
(47, 6, 'iOS的生命周期有哪些?AppDelegate和SceneDelegate的区别?', 
'iOS生命周期:
1. 应用生命周期:didFinishLaunching, willFinishLaunching
2. 状态保存:applicationWillResignActive, applicationDidEnterBackground
3. 状态恢复:applicationWillEnterForeground, applicationDidBecomeActive

AppDelegate和SceneDelegate区别:
- AppDelegate:iOS 12及之前
- SceneDelegate:iOS 13+支持多窗口
- SceneDelegate处理场景相关生命周期', 
'可以从iOS版本差异回答', 'easy', '{"iOS", "生命周期", "基础"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

(47, 6, 'iOS的内存管理机制是什么?ARC和MRC的区别?', 
'iOS内存管理机制:
1. 引用计数:retainCount
2. ARC:自动引用计数
3. MRC:手动引用计数

ARC和MRC区别:
- ARC:编译器自动插入retain/release
- MRC:手动管理内存
- ARC禁止调用retain/release

内存管理规则:
1. 自己生成的对象自己释放
2. 引用的对象需要保留
3. 不再需要时释放', 
'可以从内存管理规则回答', 'medium', '{"iOS", "内存管理", "ARC"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(47, 6, 'iOS的Runloop是什么?应用场景?', 
'Runloop是事件处理循环。
应用场景:
1. Timer定时器
2. PerformSelector
3. Input Source
4. AutoreleasePool

Runloop模式:
1. DefaultMode:默认模式
2. TrackingMode:滚动模式
3. CommonModes:公共模式', 
'可以从事件处理角度回答', 'hard', '{"iOS", "Runloop", "事件循环"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(47, 6, 'iOS的动画有哪些类型?CAAnimation的使用?', 
'iOS动画类型:
1. CABasicAnimation:基础动画
2. CAKeyframeAnimation:关键帧动画
3. CATransition:过渡动画
4. CAAnimationGroup:动画组

CAAnimation使用:
1. 创建动画对象
2. 设置动画属性
3. 添加到Layer
4. 处理动画完成', 
'可以结合代码示例', 'medium', '{"iOS", "动画", "CAAnimation"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(47, 6, 'iOS的性能优化有哪些方法?', 
'性能优化方法:
1. 减少视图层级
2. 异步绘制
3. 图片优化
4. 内存优化
5. 网络优化
6. 启动优化', 
'可以从启动速度和运行时性能回答', 'medium', '{"iOS", "性能优化", "优化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

-- Flutter开发 (bank_id=48)
(48, 6, 'Flutter的渲染原理是什么?Widget、Element、RenderObject的关系?', 
'Flutter渲染原理:
1. Widget:描述UI
2. Element:Widget实例,管理生命周期
3. RenderObject:渲染对象,负责布局和绘制

关系:
- Widget -> Element -> RenderObject
- Widget是不可变的配置
- Element是Widget的实例
- RenderObject负责渲染', 
'可以结合渲染流程图说明', 'hard', '{"Flutter", "渲染原理", "核心概念"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(48, 6, 'Flutter的状态管理有哪些方案?Provider和Riverpod的区别?', 
'Flutter状态管理方案:
1. setState:简单状态
2. Provider:依赖注入
3. Riverpod:Provider升级版
4. Bloc:响应式状态管理
5. Redux:集中式状态管理

Provider和Riverpod区别:
- Riverpod:更灵活,不依赖Widget树
- Riverpod:支持异步状态
- Riverpod:更好的测试性', 
'可以结合实际项目经验回答', 'medium', '{"Flutter", "状态管理", "Provider"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(48, 6, 'Flutter的性能优化有哪些方法?', 
'Flutter性能优化:
1. 避免不必要的重建
2. 使用RepaintBoundary
3. 优化图片加载
4. 减少Widget层级
5. 使用ListView.builder
6. 避免过度绘制', 
'可以从渲染性能回答', 'medium', '{"Flutter", "性能优化", "优化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(48, 6, 'Flutter和原生通信有哪些方式?Platform Channel的使用?', 
'Flutter和原生通信方式:
1. MethodChannel:方法调用
2. EventChannel:事件流
3. Platform Channel:基础通信

MethodChannel使用:
1. 定义Channel
2. 调用方法
3. 处理回调
4. 错误处理', 
'可以结合代码示例', 'medium', '{"Flutter", "原生通信", "Platform Channel"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(48, 6, 'Flutter的热重载原理是什么?和热启动的区别?', 
'热重载原理:
1. 注入更新
2. 重建Widget树
3. 保持状态

热重载和热启动区别:
- 热重载:快速重建,保持状态
- 热启动:重新启动应用,不保持状态', 
'可以从开发体验角度回答', 'easy', '{"Flutter", "热重载", "原理"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

-- 移动性能优化 (bank_id=49)
(49, 6, '移动应用启动优化有哪些方法?', 
'启动优化方法:
1. 延迟初始化
2. 异步加载
3. 预加载
4. 启动页优化
5. 减少首屏数据请求

启动流程优化:
1. Application初始化优化
2. 首屏页面加载优化
3. 资源预加载', 
'可以从冷启动和热启动角度回答', 'medium', '{"移动", "启动优化", "性能"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(49, 6, '移动应用内存优化有哪些方法?', 
'内存优化方法:
1. 避免内存泄漏
2. 图片优化
3. 缓存策略
4. 对象池
5. 减少对象创建', 
'可以从内存分析和优化工具回答', 'medium', '{"移动", "内存优化", "性能"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(49, 6, '移动应用网络优化有哪些方法?', 
'网络优化方法:
1. 数据压缩
2. 缓存策略
3. 请求合并
4. 断点续传
5. 网络状态检测

优化策略:
1. 静态资源CDN
2. 动态数据缓存
3. 接口聚合
4. 请求优先级', 
'可以从HTTP和网络协议角度回答', 'medium', '{"移动", "网络优化", "性能"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(49, 6, '移动应用电量优化有哪些方法?', 
'电量优化方法:
1. 减少CPU使用
2. 优化网络请求
3. 减少GPS使用
4. 优化WakeLock
5. 批量处理任务', 
'可以从电量分析工具回答', 'medium', '{"移动", "电量优化", "性能"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(49, 6, '移动应用卡顿优化有哪些方法?', 
'卡顿优化方法:
1. 减少主线程负担
2. 异步处理
3. 布局优化
4. 绘制优化
5. 使用RecyclerView', 
'可以从FPS和帧率角度回答', 'medium', '{"移动", "卡顿优化", "性能"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

-- 移动安全 (bank_id=50)
(50, 6, '移动应用安全有哪些常见问题?', 
'移动安全问题:
1. 数据存储安全
2. 网络传输安全
3. 代码安全
4. 权限控制
5. 环境检测

安全措施:
1. 数据加密
2. HTTPS
3. 混淆
4. 签名
5. 反调试', 
'可以从安全防护角度回答', 'medium', '{"移动", "安全", "防护"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(50, 6, '移动应用反调试有哪些方法?', 
'反调试方法:
1. 检测调试器
2. 检测ptrace
3. 检测文件描述符
4. 检测异常
5. 检测性能', 
'可以从Android和iOS不同平台回答', 'hard', '{"移动", "反调试", "安全"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(50, 6, '移动应用数据加密有哪些方法?', 
'数据加密方法:
1. 对称加密:AES
2. 非对称加密:RSA
3. 哈希算法:SHA
4. 证书绑定:SSL Pinning

加密策略:
1. 敏感数据加密存储
2. 网络传输加密
3. 密钥管理', 
'可以从加密算法和实现回答', 'hard', '{"移动", "加密", "安全"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(50, 6, '移动应用防抓包有哪些方法?', 
'防抓包方法:
1. SSL Pinning
2. 证书校验
3. 环境检测
4. 代码混淆
5. 反调试', 
'可以从HTTPS和网络层回答', 'hard', '{"移动", "防抓包", "安全"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(50, 6, '移动应用代码混淆有哪些方法?', 
'代码混淆方法:
1. 名称混淆
2. 控制流混淆
3. 数据混淆
4. 移除调试信息
5. 移除无用代码', 
'可以从Android和iOS不同工具回答', 'medium', '{"移动", "混淆", "安全"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW());
