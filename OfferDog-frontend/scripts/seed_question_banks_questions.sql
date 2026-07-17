-- 初始化面试题库数据
-- 题库数据
INSERT INTO question_banks (name, description, category_id, created_by, created_at, updated_at) VALUES
('Vue前端开发', 'Vue.js 相关面试题目,涵盖基础到高级', 4, 1, NOW(), NOW()),
('React开发', 'React 相关面试题目,包括Hooks、性能优化等', 4, 1, NOW(), NOW()),
('JavaScript基础', 'JavaScript 核心概念和编程题', 4, 1, NOW(), NOW()),
('TypeScript', 'TypeScript 类型系统和高级特性', 4, 1, NOW(), NOW()),
('前端工程化', 'Webpack、Vite、CI/CD等工程化知识', 4, 1, NOW(), NOW());

-- 题目数据
INSERT INTO questions (bank_id, category_id, content, answer, tips, difficulty, tags, created_by, stats, remark, created_at, updated_at) VALUES
-- Vue相关题目
(1, 4, 'Vue3的响应式原理是什么?Proxy和Object.defineProperty有什么区别?', 
'Vue3使用Proxy实现响应式,相比Object.defineProperty有以下优势:
1. Proxy可以监听整个对象,不需要遍历属性
2. 支持数组的响应式
3. 性能更好
4. 可以监听动态添加的属性', 
'可以结合代码示例说明', 'medium', '{"Vue", "响应式", "核心原理"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '高频考点', NOW(), NOW()),

(1, 4, 'Vue3的Composition API相比Options API有什么优势?', 
'Composition API的优势:
1. 逻辑复用更方便,可以通过组合函数实现
2. 代码组织更灵活,相关逻辑可以放在一起
3. 类型推导更友好
4. 更好的代码复用和抽象能力', 
'可以从代码组织和复用角度回答', 'medium', '{"Vue", "Composition API", "Options API"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要概念', NOW(), NOW()),

(1, 4, 'Vue的虚拟DOM原理是什么?diff算法是如何工作的?', 
'Vue的虚拟DOM是用JavaScript对象模拟真实DOM结构。
diff算法采用深度优先、同层比较的策略:
1. 新旧节点对比
2. 如果类型不同,直接替换
3. 如果类型相同,更新属性
4. 递归比较子节点
5. 使用key优化列表diff', 
'可以画图说明diff过程', 'hard', '{"Vue", "虚拟DOM", "diff算法"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点考点', NOW(), NOW()),

(1, 4, 'Vue的生命周期钩子有哪些?Vue3和Vue2有什么区别?', 
'Vue3的生命周期钩子:
beforeMount, mounted, beforeUpdate, updated, beforeUnmount, unmounted, errorCaptured, renderTracked, renderTriggered
Vue2有beforeCreate, created, beforeMount, mounted, beforeUpdate, updated, beforeDestroy, destroyed等
Vue3使用Composition API,生命周期钩子改为setup相关的函数', 
'可以画生命周期图', 'easy', '{"Vue", "生命周期"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

(1, 4, 'Vue的计算属性和侦听器有什么区别?分别在什么场景下使用?', 
'计算属性:
- 基于依赖缓存,依赖不变则不重新计算
- 用于同步计算,必须有返回值
- 适合用于模板渲染

侦听器:
- 无缓存,依赖变化就触发
- 用于异步操作或副作用
- 适合用于监听数据变化并执行操作', 
'可以结合实际场景说明', 'medium', '{"Vue", "计算属性", "侦听器"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '常用API', NOW(), NOW()),

-- React相关题目
(2, 4, 'React的虚拟DOM原理是什么?和Vue有什么区别?', 
'React虚拟DOM是用JavaScript对象模拟DOM结构。
主要区别:
1. React使用JSX,Vue使用模板或JSX
2. React默认全量更新,Vue有依赖追踪
3. React Hooks和Vue Composition API实现方式不同
4. React更灵活,Vue更约定', 
'可以从设计理念角度回答', 'medium', '{"React", "虚拟DOM", "对比"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '高频考点', NOW(), NOW()),

(2, 4, 'React Hooks的原理是什么?useEffect和useLayoutEffect有什么区别?', 
'React Hooks基于函数组件和闭包实现。
useEffect:
- 异步执行,在浏览器绘制后执行
- 适合副作用操作

useLayoutEffect:
- 同步执行,在浏览器绘制前执行
- 适合读取布局和DOM操作', 
'可以从执行时机和使用场景回答', 'hard', '{"React", "Hooks", "useEffect"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要概念', NOW(), NOW()),

(2, 4, 'React的性能优化有哪些方法?', 
'React性能优化方法:
1. 使用React.memo缓存组件
2. 使用useMemo和useCallback缓存值和函数
3. 列表使用key优化
4. 虚拟列表处理大数据
5. 懒加载和代码分割
6. 避免不必要的渲染', 
'可以结合实际项目经验回答', 'medium', '{"React", "性能优化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(2, 4, 'React的Fiber架构是什么?解决了什么问题?', 
'Fiber是React的新协调引擎。
解决的问题:
1. 旧版本无法中断和恢复渲染
2. 大组件渲染卡顿
3. 无法优先处理高优先级更新

Fiber通过分片渲染、优先级调度等机制解决了这些问题', 
'可以从渲染机制角度回答', 'hard', '{"React", "Fiber", "架构"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点', NOW(), NOW()),

(2, 4, 'React的Context原理是什么?有什么优缺点?', 
'Context通过Provider和Consumer实现跨组件通信。
优点:
- 避免props逐层传递
- 适合全局状态

缺点:
- Provider变化会导致所有Consumer重新渲染
- 不适合高频更新的数据', 
'可以结合useContext Hook回答', 'medium', '{"React", "Context", "状态管理"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要概念', NOW(), NOW()),

-- JavaScript基础题目
(3, 4, 'JavaScript的原型链是什么?如何实现继承?', 
'JavaScript每个对象都有__proto__指向原型对象,形成原型链。
继承方式:
1. 原型链继承
2. 借用构造函数继承
3. 组合继承
4. 原型式继承
5. ES6 class继承', 
'可以画原型链图说明', 'easy', '{"JavaScript", "原型链", "继承"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

(3, 4, 'JavaScript的闭包是什么?有什么用途和注意事项?', 
'闭包是函数记住其词法作用域的能力。
用途:
1. 数据封装和私有变量
2. 柯里化
3. 防抖节流

注意事项:
1. 可能导致内存泄漏
2. 注意循环中的闭包', 
'可以结合代码示例', 'medium', '{"JavaScript", "闭包"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要概念', NOW(), NOW()),

(3, 4, 'JavaScript的事件循环机制是什么?宏任务和微任务有什么区别?', 
'事件循环机制:
1. 执行栈清空后执行微任务队列
2. 然后执行宏任务队列的一个任务
3. 重复这个过程

宏任务: setTimeout, setInterval, I/O
微任务: Promise, MutationObserver, queueMicrotask', 
'可以画图说明执行顺序', 'hard', '{"JavaScript", "事件循环", "异步"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '难点考点', NOW(), NOW()),

(3, 4, 'JavaScript的this指向规则是什么?如何改变this指向?', 
'this指向规则:
1. 全局上下文指向window
2. 对象方法指向调用对象
3. 构造函数指向新对象
4. 箭头函数继承外层this

改变this方法:
1. call/apply
2. bind
3. 箭头函数', 
'可以结合代码示例', 'medium', '{"JavaScript", "this", "核心概念"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(3, 4, 'JavaScript的深拷贝和浅拷贝有什么区别?如何实现深拷贝?', 
'浅拷贝只复制引用,深拷贝创建新对象。
深拷贝实现:
1. JSON.parse(JSON.stringify())
2. 递归实现
3. 使用lodash
4. 结构化克隆', 
'可以写出代码实现', 'medium', '{"JavaScript", "拷贝", "数据结构"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '常用考点', NOW(), NOW()),

-- TypeScript题目
(4, 4, 'TypeScript的泛型是什么?有什么用途?', 
'泛型是创建可重用组件的一种方式。
用途:
1. 函数重用
2. 数组类型安全
3. 类的重用
4. 接口定义', 
'可以结合代码示例', 'medium', '{"TypeScript", "泛型"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要概念', NOW(), NOW()),

(4, 4, 'TypeScript的类型守卫有哪些方式?', 
'类型守卫方式:
1. typeof
2. instanceof
3. in操作符
4. 自定义类型守卫
5. 空值合并运算符
6. 可选链运算符', 
'可以结合代码说明', 'easy', '{"TypeScript", "类型", "类型守卫"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础考点', NOW(), NOW()),

(4, 4, 'TypeScript的装饰器是什么?有哪些类型的装饰器?', 
'装饰器是特殊类型的声明,可以附加到类声明、方法、访问器、属性或参数。
类型:
1. 类装饰器
2. 方法装饰器
3. 访问器装饰器
4. 属性装饰器
5. 参数装饰器', 
'可以结合装饰器使用场景', 'hard', '{"TypeScript", "装饰器"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '高级特性', NOW(), NOW()),

(4, 4, 'TypeScript的交叉类型和联合类型有什么区别?', 
'交叉类型(&): 合并多个类型的属性
联合类型(|): 表示可以是多种类型之一

区别:
1. 交叉类型要求具备所有类型的特点
2. 联合类型只需要满足其中一种类型', 
'可以结合代码示例', 'easy', '{"TypeScript", "类型", "交叉类型", "联合类型"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础概念', NOW(), NOW()),

(4, 4, 'TypeScript的类型推断和类型断言有什么区别?', 
'类型推断: TypeScript自动推断变量类型
类型断言: 开发者手动指定变量类型

区别:
1. 类型推断是自动的
2. 类型断言是手动的
3. 类型断言不进行类型检查', 
'可以结合代码说明', 'easy', '{"TypeScript", "类型", "推断", "断言"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '基础概念', NOW(), NOW()),

-- 工程化题目
(5, 4, 'Webpack的打包原理是什么?如何优化打包体积?', 
'Webpack打包原理:
1. 依赖分析
2. 模块打包
3. 代码优化

优化方法:
1. Tree Shaking
2. 代码分割
3. 压缩混淆
4. 图片压缩
5. 懒加载', 
'可以结合配置说明', 'medium', '{"Webpack", "工程化", "优化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(5, 4, 'Vite和Webpack有什么区别?为什么Vite更快?', 
'Vite更快的原因:
1. 开发环境使用ESM,不需要打包
2. 预构建依赖
3. 按需编译
4. 基于Rollup生产构建', 
'可以从开发体验角度回答', 'medium', '{"Vite", "Webpack", "对比"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '热点考点', NOW(), NOW()),

(5, 4, 'CI/CD是什么?如何配置前端的CI/CD流程?', 
'CI/CD是持续集成和持续部署。
前端CI/CD:
1. 代码提交触发
2. 运行测试
3. 构建打包
4. 部署发布', 
'可以结合具体工具说明', 'hard', '{"CI/CD", "工程化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '进阶考点', NOW(), NOW()),

(5, 4, '前端性能优化有哪些方法?', 
'前端性能优化:
1. 减少HTTP请求
2. 资源压缩
3. CDN加速
4. 懒加载
5. 预加载
6. 代码分割
7. 缓存策略', 
'可以从页面加载和运行时两个角度回答', 'medium', '{"性能优化", "工程化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '重要考点', NOW(), NOW()),

(5, 4, 'Monorepo和Multirepo有什么区别?什么时候使用Monorepo?', 
'Monorepo: 单仓库管理多个包
Multirepo: 每个包独立仓库

Monorepo适用场景:
1. 多个项目共享代码
2. 需要跨项目协作
3. 统一版本管理

优点:
1. 代码共享方便
2. 统一构建
3. 依赖管理简单', 
'可以结合Lerna、Turborepo等工具', 'hard', '{"Monorepo", "工程化"}', 1, '{"views": 0, "likes": 0, "favorites": 0}', '进阶概念', NOW(), NOW());
