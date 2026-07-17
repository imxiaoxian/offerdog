#!/usr/bin/env python3
"""
向数据库中初始化面试题库数据
包括题库和题目
"""

import psycopg2
import json
from datetime import datetime

# 数据库连接配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'interview',
    'user': 'postgres',
    'password': 'your_password'
}

# 题库数据
QUESTION_BANKS = [
    {
        'name': 'Vue前端开发',
        'description': 'Vue.js 相关面试题目,涵盖基础到高级',
        'category_id': 101
    },
    {
        'name': 'React开发',
        'description': 'React 相关面试题目,包括Hooks、性能优化等',
        'category_id': 102
    },
    {
        'name': 'JavaScript基础',
        'description': 'JavaScript 核心概念和编程题',
        'category_id': 103
    },
    {
        'name': 'TypeScript',
        'description': 'TypeScript 类型系统和高级特性',
        'category_id': 104
    },
    {
        'name': '前端工程化',
        'description': 'Webpack、Vite、CI/CD等工程化知识',
        'category_id': 105
    }
]

# 题目数据
QUESTIONS = [
    # Vue相关题目
    {
        'bank_id': 1,
        'category_id': 101,
        'content': 'Vue3的响应式原理是什么?Proxy和Object.defineProperty有什么区别?',
        'answer': 'Vue3使用Proxy实现响应式,相比Object.defineProperty有以下优势:\n1. Proxy可以监听整个对象,不需要遍历属性\n2. 支持数组的响应式\n3. 性能更好\n4. 可以监听动态添加的属性',
        'tips': '可以结合代码示例说明',
        'difficulty': 'MEDIUM',
        'tags': ['Vue', '响应式', '核心原理'],
        'remark': '高频考点'
    },
    {
        'bank_id': 1,
        'category_id': 101,
        'content': 'Vue3的Composition API相比Options API有什么优势?',
        'answer': 'Composition API的优势:\n1. 逻辑复用更方便,可以通过组合函数实现\n2. 代码组织更灵活,相关逻辑可以放在一起\n3. 类型推导更友好\n4. 更好的代码复用和抽象能力',
        'tips': '可以从代码组织和复用角度回答',
        'difficulty': 'MEDIUM',
        'tags': ['Vue', 'Composition API', 'Options API'],
        'remark': '重要概念'
    },
    {
        'bank_id': 1,
        'category_id': 101,
        'content': 'Vue的虚拟DOM原理是什么?diff算法是如何工作的?',
        'answer': 'Vue的虚拟DOM是用JavaScript对象模拟真实DOM结构。\ndiff算法采用深度优先、同层比较的策略:\n1. 新旧节点对比\n2. 如果类型不同,直接替换\n3. 如果类型相同,更新属性\n4. 递归比较子节点\n5. 使用key优化列表diff',
        'tips': '可以画图说明diff过程',
        'difficulty': 'HARD',
        'tags': ['Vue', '虚拟DOM', 'diff算法'],
        'remark': '难点考点'
    },
    {
        'bank_id': 1,
        'category_id': 101,
        'content': 'Vue的生命周期钩子有哪些?Vue3和Vue2有什么区别?',
        'answer': 'Vue3的生命周期钩子:\nbeforeMount, mounted, beforeUpdate, updated, beforeUnmount, unmounted, errorCaptured, renderTracked, renderTriggered\nVue2有beforeCreate, created, beforeMount, mounted, beforeUpdate, updated, beforeDestroy, destroyed等\nVue3使用Composition API,生命周期钩子改为setup相关的函数',
        'tips': '可以画生命周期图',
        'difficulty': 'EASY',
        'tags': ['Vue', '生命周期'],
        'remark': '基础考点'
    },
    {
        'bank_id': 1,
        'category_id': 101,
        'content': 'Vue的计算属性和侦听器有什么区别?分别在什么场景下使用?',
        'answer': '计算属性:\n- 基于依赖缓存,依赖不变则不重新计算\n- 用于同步计算,必须有返回值\n- 适合用于模板渲染\n\n侦听器:\n- 无缓存,依赖变化就触发\n- 用于异步操作或副作用\n- 适合用于监听数据变化并执行操作',
        'tips': '可以结合实际场景说明',
        'difficulty': 'MEDIUM',
        'tags': ['Vue', '计算属性', '侦听器'],
        'remark': '常用API'
    },
    # React相关题目
    {
        'bank_id': 2,
        'category_id': 102,
        'content': 'React的虚拟DOM原理是什么?和Vue有什么区别?',
        'answer': 'React虚拟DOM是用JavaScript对象模拟DOM结构。\n主要区别:\n1. React使用JSX,Vue使用模板或JSX\n2. React默认全量更新,Vue有依赖追踪\n3. React Hooks和Vue Composition API实现方式不同\n4. React更灵活,Vue更约定',
        'tips': '可以从设计理念角度回答',
        'difficulty': 'MEDIUM',
        'tags': ['React', '虚拟DOM', '对比'],
        'remark': '高频考点'
    },
    {
        'bank_id': 2,
        'category_id': 102,
        'content': 'React Hooks的原理是什么?useEffect和useLayoutEffect有什么区别?',
        'answer': 'React Hooks基于函数组件和闭包实现。\nuseEffect:\n- 异步执行,在浏览器绘制后执行\n- 适合副作用操作\n\nuseLayoutEffect:\n- 同步执行,在浏览器绘制前执行\n- 适合读取布局和DOM操作',
        'tips': '可以从执行时机和使用场景回答',
        'difficulty': 'HARD',
        'tags': ['React', 'Hooks', 'useEffect'],
        'remark': '重要概念'
    },
    {
        'bank_id': 2,
        'category_id': 102,
        'content': 'React的性能优化有哪些方法?',
        'answer': 'React性能优化方法:\n1. 使用React.memo缓存组件\n2. 使用useMemo和useCallback缓存值和函数\n3. 列表使用key优化\n4. 虚拟列表处理大数据\n5. 懒加载和代码分割\n6. 避免不必要的渲染',
        'tips': '可以结合实际项目经验回答',
        'difficulty': 'MEDIUM',
        'tags': ['React', '性能优化'],
        'remark': '重要考点'
    },
    {
        'bank_id': 2,
        'category_id': 102,
        'content': 'React的Fiber架构是什么?解决了什么问题?',
        'answer': 'Fiber是React的新协调引擎。\n解决的问题:\n1. 旧版本无法中断和恢复渲染\n2. 大组件渲染卡顿\n3. 无法优先处理高优先级更新\n\nFiber通过分片渲染、优先级调度等机制解决了这些问题',
        'tips': '可以从渲染机制角度回答',
        'difficulty': 'HARD',
        'tags': ['React', 'Fiber', '架构'],
        'remark': '难点'
    },
    {
        'bank_id': 2,
        'category_id': 102,
        'content': 'React的Context原理是什么?有什么优缺点?',
        'answer': 'Context通过Provider和Consumer实现跨组件通信。\n优点:\n- 避免props逐层传递\n- 适合全局状态\n\n缺点:\n- Provider变化会导致所有Consumer重新渲染\n- 不适合高频更新的数据',
        'tips': '可以结合useContext Hook回答',
        'difficulty': 'MEDIUM',
        'tags': ['React', 'Context', '状态管理'],
        'remark': '重要概念'
    },
    # JavaScript基础题目
    {
        'bank_id': 3,
        'category_id': 103,
        'content': 'JavaScript的原型链是什么?如何实现继承?',
        'answer': 'JavaScript每个对象都有__proto__指向原型对象,形成原型链。\n继承方式:\n1. 原型链继承\n2. 借用构造函数继承\n3. 组合继承\n4. 原型式继承\n5. ES6 class继承',
        'tips': '可以画原型链图说明',
        'difficulty': 'EASY',
        'tags': ['JavaScript', '原型链', '继承'],
        'remark': '基础考点'
    },
    {
        'bank_id': 3,
        'category_id': 103,
        'content': 'JavaScript的闭包是什么?有什么用途和注意事项?',
        'answer': '闭包是函数记住其词法作用域的能力。\n用途:\n1. 数据封装和私有变量\n2. 柯里化\n3. 防抖节流\n\n注意事项:\n1. 可能导致内存泄漏\n2. 注意循环中的闭包',
        'tips': '可以结合代码示例',
        'difficulty': 'MEDIUM',
        'tags': ['JavaScript', '闭包'],
        'remark': '重要概念'
    },
    {
        'bank_id': 3,
        'category_id': 103,
        'content': 'JavaScript的事件循环机制是什么?宏任务和微任务有什么区别?',
        'answer': '事件循环机制:\n1. 执行栈清空后执行微任务队列\n2. 然后执行宏任务队列的一个任务\n3. 重复这个过程\n\n宏任务: setTimeout, setInterval, I/O\n微任务: Promise, MutationObserver, queueMicrotask',
        'tips': '可以画图说明执行顺序',
        'difficulty': 'HARD',
        'tags': ['JavaScript', '事件循环', '异步'],
        'remark': '难点考点'
    },
    {
        'bank_id': 3,
        'category_id': 103,
        'content': 'JavaScript的this指向规则是什么?如何改变this指向?',
        'answer': 'this指向规则:\n1. 全局上下文指向window\n2. 对象方法指向调用对象\n3. 构造函数指向新对象\n4. 箭头函数继承外层this\n\n改变this方法:\n1. call/apply\n2. bind\n3. 箭头函数',
        'tips': '可以结合代码示例',
        'difficulty': 'MEDIUM',
        'tags': ['JavaScript', 'this', '核心概念'],
        'remark': '重要考点'
    },
    {
        'bank_id': 3,
        'category_id': 103,
        'content': 'JavaScript的深拷贝和浅拷贝有什么区别?如何实现深拷贝?',
        'answer': '浅拷贝只复制引用,深拷贝创建新对象。\n深拷贝实现:\n1. JSON.parse(JSON.stringify())\n2. 递归实现\n3. 使用lodash\n4. 结构化克隆',
        'tips': '可以写出代码实现',
        'difficulty': 'MEDIUM',
        'tags': ['JavaScript', '拷贝', '数据结构'],
        'remark': '常用考点'
    },
    # TypeScript题目
    {
        'bank_id': 4,
        'category_id': 104,
        'content': 'TypeScript的泛型是什么?有什么用途?',
        'answer': '泛型是创建可重用组件的一种方式。\n用途:\n1. 函数重用\n2. 数组类型安全\n3. 类的重用\n4. 接口定义',
        'tips': '可以结合代码示例',
        'difficulty': 'MEDIUM',
        'tags': ['TypeScript', '泛型'],
        'remark': '重要概念'
    },
    {
        'bank_id': 4,
        'category_id': 104,
        'content': 'TypeScript的类型守卫有哪些方式?',
        'answer': '类型守卫方式:\n1. typeof\n2. instanceof\n3. in操作符\n4. 自定义类型守卫\n5. 空值合并运算符\n6. 可选链运算符',
        'tips': '可以结合代码说明',
        'difficulty': 'EASY',
        'tags': ['TypeScript', '类型', '类型守卫'],
        'remark': '基础考点'
    },
    {
        'bank_id': 4,
        'category_id': 104,
        'content': 'TypeScript的装饰器是什么?有哪些类型的装饰器?',
        'answer': '装饰器是特殊类型的声明,可以附加到类声明、方法、访问器、属性或参数。\n类型:\n1. 类装饰器\n2. 方法装饰器\n3. 访问器装饰器\n4. 属性装饰器\n5. 参数装饰器',
        'tips': '可以结合装饰器使用场景',
        'difficulty': 'HARD',
        'tags': ['TypeScript', '装饰器'],
        'remark': '高级特性'
    },
    {
        'bank_id': 4,
        'category_id': 104,
        'content': 'TypeScript的交叉类型和联合类型有什么区别?',
        'answer': '交叉类型(&): 合并多个类型的属性\n联合类型(|): 表示可以是多种类型之一\n\n区别:\n1. 交叉类型要求具备所有类型的特点\n2. 联合类型只需要满足其中一种类型',
        'tips': '可以结合代码示例',
        'difficulty': 'EASY',
        'tags': ['TypeScript', '类型', '交叉类型', '联合类型'],
        'remark': '基础概念'
    },
    {
        'bank_id': 4,
        'category_id': 104,
        'content': 'TypeScript的类型推断和类型断言有什么区别?',
        'answer': '类型推断: TypeScript自动推断变量类型\n类型断言: 开发者手动指定变量类型\n\n区别:\n1. 类型推断是自动的\n2. 类型断言是手动的\n3. 类型断言不进行类型检查',
        'tips': '可以结合代码说明',
        'difficulty': 'EASY',
        'tags': ['TypeScript', '类型', '推断', '断言'],
        'remark': '基础概念'
    },
    # 工程化题目
    {
        'bank_id': 5,
        'category_id': 105,
        'content': 'Webpack的打包原理是什么?如何优化打包体积?',
        'answer': 'Webpack打包原理:\n1. 依赖分析\n2. 模块打包\n3. 代码优化\n\n优化方法:\n1. Tree Shaking\n2. 代码分割\n3. 压缩混淆\n4. 图片压缩\n5. 懒加载',
        'tips': '可以结合配置说明',
        'difficulty': 'MEDIUM',
        'tags': ['Webpack', '工程化', '优化'],
        'remark': '重要考点'
    },
    {
        'bank_id': 5,
        'category_id': 105,
        'content': 'Vite和Webpack有什么区别?为什么Vite更快?',
        'answer': 'Vite更快的原因:\n1. 开发环境使用ESM,不需要打包\n2. 预构建依赖\n3. 按需编译\n4. 基于Rollup生产构建',
        'tips': '可以从开发体验角度回答',
        'difficulty': 'MEDIUM',
        'tags': ['Vite', 'Webpack', '对比'],
        'remark': '热点考点'
    },
    {
        'bank_id': 5,
        'category_id': 105,
        'content': 'CI/CD是什么?如何配置前端的CI/CD流程?',
        'answer': 'CI/CD是持续集成和持续部署。\n前端CI/CD:\n1. 代码提交触发\n2. 运行测试\n3. 构建打包\n4. 部署发布',
        'tips': '可以结合具体工具说明',
        'difficulty': 'HARD',
        'tags': ['CI/CD', '工程化'],
        'remark': '进阶考点'
    },
    {
        'bank_id': 5,
        'category_id': 105,
        'content': '前端性能优化有哪些方法?',
        'answer': '前端性能优化:\n1. 减少HTTP请求\n2. 资源压缩\n3. CDN加速\n4. 懒加载\n5. 预加载\n6. 代码分割\n7. 缓存策略',
        'tips': '可以从页面加载和运行时两个角度回答',
        'difficulty': 'MEDIUM',
        'tags': ['性能优化', '工程化'],
        'remark': '重要考点'
    },
    {
        'bank_id': 5,
        'category_id': 105,
        'content': 'Monorepo和Multirepo有什么区别?什么时候使用Monorepo?',
        'answer': 'Monorepo: 单仓库管理多个包\nMultirepo: 每个包独立仓库\n\nMonorepo适用场景:\n1. 多个项目共享代码\n2. 需要跨项目协作\n3. 统一版本管理\n\n优点:\n1. 代码共享方便\n2. 统一构建\n3. 依赖管理简单',
        'tips': '可以结合Lerna、Turborepo等工具',
        'difficulty': 'HARD',
        'tags': ['Monorepo', '工程化'],
        'remark': '进阶概念'
    }
]


def get_connection():
    """获取数据库连接"""
    return psycopg2.connect(**DB_CONFIG)


def insert_question_banks(conn):
    """插入题库数据"""
    cursor = conn.cursor()
    
    for bank in QUESTION_BANKS:
        try:
            cursor.execute("""
                INSERT INTO question_banks (name, description, category_id, created_at, updated_at)
                VALUES (%s, %s, %s, NOW(), NOW())
                ON CONFLICT (name) DO NOTHING
                RETURNING id
            """, (bank['name'], bank['description'], bank['category_id']))
            
            result = cursor.fetchone()
            if result:
                print(f"✓ 插入题库: {bank['name']} (ID: {result[0]})")
            else:
                print(f"○ 题库已存在: {bank['name']}")
                
        except Exception as e:
            print(f"✗ 插入题库失败 {bank['name']}: {e}")
            conn.rollback()
    
    conn.commit()
    cursor.close()


def insert_questions(conn):
    """插入题目数据"""
    cursor = conn.cursor()
    
    for question in QUESTIONS:
        try:
            cursor.execute("""
                INSERT INTO questions 
                (bank_id, category_id, content, answer, tips, difficulty, tags, remark, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
                ON CONFLICT DO NOTHING
                RETURNING id
            """, (
                question['bank_id'],
                question['category_id'],
                question['content'],
                question['answer'],
                question['tips'],
                question['difficulty'].lower(),
                question['tags'],
                question['remark']
            ))
            
            result = cursor.fetchone()
            if result:
                print(f"✓ 插入题目: {question['content'][:50]}... (ID: {result[0]})")
            else:
                print(f"○ 题目已存在: {question['content'][:50]}...")
                
        except Exception as e:
            print(f"✗ 插入题目失败: {e}")
            print(f"  题目内容: {question['content'][:100]}...")
            conn.rollback()
    
    conn.commit()
    cursor.close()


def main():
    """主函数"""
    print("=" * 60)
    print("开始初始化面试题库数据...")
    print("=" * 60)
    
    try:
        conn = get_connection()
        print("✓ 数据库连接成功\n")
        
        # 插入题库
        print("正在插入题库数据...")
        insert_question_banks(conn)
        print()
        
        # 插入题目
        print("正在插入题目数据...")
        insert_questions(conn)
        print()
        
        # 统计数据
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM question_banks WHERE deleted_at IS NULL")
        bank_count = cursor.fetchone()[0]
        print(f"题库总数: {bank_count}")
        
        cursor.execute("SELECT COUNT(*) FROM questions WHERE deleted_at IS NULL")
        question_count = cursor.fetchone()[0]
        print(f"题目总数: {question_count}")
        cursor.close()
        
        conn.close()
        
        print("\n" + "=" * 60)
        print("数据初始化完成!")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n✗ 数据库连接失败: {e}")
        print("请检查数据库配置和连接状态")


if __name__ == '__main__':
    main()
