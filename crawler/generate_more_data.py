import psycopg
import random
from datetime import datetime, timedelta
import re

COMPANIES = [
    ("阿里", "阿里云", "后端开发"),
    ("腾讯", "TEG", "后台开发"),
    ("字节", "抖音", "后端开发"),
    ("美团", "到店", "Java开发"),
    ("京东", "零售", "后端开发"),
    ("百度", "搜索", "C++开发"),
    ("快手", "基础架构", "Go开发"),
    ("滴滴", "出行", "后端开发"),
    ("拼多多", "平台", "Java开发"),
    ("网易", "游戏", "服务端开发"),
    ("小红书", "搜索", "后端开发"),
    ("米哈游", "技术中台", "Go开发"),
    ("蔚来", "数字化", "后端开发"),
    ("理想汽车", "智能语音", "C++开发"),
    ("比亚迪", "软件开发", "嵌入式开发"),
    ("华为", "云服务", "后端开发"),
    ("中兴", "研发", "C++开发"),
    ("大疆", "服务端", "Go开发"),
    ("商汤科技", "平台", "后端开发"),
    ("旷视科技", "Face++", "C++开发"),
]

EIGHT_BAG_QUESTIONS = {
    "Java": [
        "HashMap的底层实现原理是什么？JDK1.8之后有哪些优化？",
        "JVM内存模型详解，每个区域的作用是什么？",
        "什么是GCroot？哪些对象可以作为GCroot？",
        "ConcurrentHashMap如何实现线程安全？",
        "Spring Bean的生命周期是怎样的？",
        "Spring事务的传播行为有哪些？",
        "MySQL索引的数据结构是什么？B+树有什么优势？",
        "什么是慢查询？如何优化慢查询？",
        "Redis的数据类型有哪些？",
        "Redis的持久化机制RDB和AOF的区别？",
        "什么是缓存穿透、缓存击穿、缓存雪崩？",
        "分布式锁的实现方式有哪些？",
        "RPC和HTTP的区别是什么？",
        "Dubbo的工作原理是什么？",
        "消息队列如何保证消息不丢失？",
    ],
    "Go": [
        "Go语言的GMP模型是什么？",
        "Go中make和new的区别？",
        "Go的GC机制是怎样的？",
        "Go的map是线程安全的吗？如何实现并发安全？",
        "Go的defer执行时机和顺序？",
        "Context的作用是什么？",
        "Go的select和switch的区别？",
        "Channel的关闭原则？",
        "Goroutine泄漏的原因和排查方法？",
    ],
    "C++": [
        "C++内存布局是怎样的？",
        "虚函数的实现原理是什么？",
        "智能指针有哪些？shared_ptr的引用计数在哪里管理？",
        "C++11的新特性有哪些？",
        "STL容器的底层实现？vector扩容机制？",
        "什么是RAII？",
        "C++多态是如何实现的？",
        "static和const的作用？",
    ],
    "通用": [
        "HTTP和HTTPS的区别？HTTPS的加密过程？",
        "TCP三次握手和四次挥手？",
        "TCP和UDP的区别？",
        "TIME_WAIT状态产生的原因？",
        "什么是滑动窗口？",
        "MySQL事务的隔离级别？",
        "InnoDB和MyISAM的区别？",
        "什么是MVCC？",
        "Redis为什么这么快？",
        "Kafka和RocketMQ的区别？",
    ]
}

PROJECT_EXPERIENCE = [
    "负责用户登录注册模块，采用JWT实现分布式登录",
    "设计并实现了商品秒杀系统，使用Redis缓存库存，消息队列削峰填谷",
    "开发了实时消息推送系统，使用WebSocket长连接",
    "优化了查询接口性能，将响应时间从500ms降低到50ms",
    "设计了数据同步方案，实现多端数据实时同步",
    "负责支付模块开发，对接第三方支付渠道",
    "开发了分布式任务调度平台，支持定时任务和延迟任务",
    "实现了灰度发布功能，支持流量切分",
    "设计了统一日志平台，收集全链路日志",
    "开发了配置中心，支持动态配置更新",
    "实现了服务限流和熔断，保护系统稳定性",
    "设计了分库分表中间件，支持水平扩展",
    "开发了API网关，统一处理鉴权和路由",
    "负责数据迁移项目，实现零 downtime 迁移",
    "优化了数据库索引，将查询性能提升10倍",
]

CODING_QUESTIONS = [
    "手撕LRU缓存实现",
    "手撕并发安全的单例模式",
    "反转链表",
    "二叉树的层序遍历",
    "两数之和",
    "无重复字符的最长子串",
    "合并两个有序链表",
    "二叉树的中序遍历",
    "对称二叉树判断",
    "删除链表的倒数第N个节点",
    "全排列",
    "组合总和",
    "岛屿数量",
    "最大子数组和",
    "二叉树的最大深度",
]

OTHER_QUESTIONS = [
    "你为什么选择我们公司？",
    "你的职业规划是什么？",
    "你觉得自己最大的优点和缺点是什么？",
    "工作中遇到的最大挑战是什么？如何解决的？",
    "你对加班怎么看？",
    "你期望的薪资是多少？",
    "你还有什么问题想问我吗？",
    "介绍一下你最满意的项目？",
    "你在团队中担任什么角色？",
    "如果给你一个很紧的工期，你会怎么做？",
]

def generate_content(company, position, language):
    questions = []
    questions.extend(random.sample(EIGHT_BAG_QUESTIONS.get(language, EIGHT_BAG_QUESTIONS["通用"]), 5))
    questions.extend(random.sample(PROJECT_EXPERIENCE, 4))
    questions.extend(random.sample(CODING_QUESTIONS, 4))
    questions.extend(random.sample(OTHER_QUESTIONS, 3))
    
    return "\n".join(questions)

def format_content(content):
    lines = content.split("\n")
    formatted_lines = []
    eight_part = []
    project_part = []
    coding_part = []
    other_part = []
    
    current_section = None
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        line = re.sub(r'[\x00-\x1f\x7f-\x9f]', '', line)
        
        if any(kw in line for kw in ["底层", "原理", "机制", "区别", "是什么", "如何"]):
            current_section = "eight"
        elif any(kw in line for kw in ["项目", "负责", "设计", "开发", "优化"]):
            current_section = "project"
        elif any(kw in line for kw in ["手撕", "算法", "实现", "编程"]):
            current_section = "coding"
        else:
            current_section = "other"
        
        if current_section == "eight" and len(eight_part) < 5:
            eight_part.append(f"{len(eight_part) + 1}. {line}")
        elif current_section == "project" and len(project_part) < 4:
            project_part.append(f"{len(project_part) + 1}. {line}")
        elif current_section == "coding" and len(coding_part) < 4:
            coding_part.append(f"{len(coding_part) + 1}. {line}")
        elif current_section == "other" and len(other_part) < 3:
            other_part.append(f"{len(other_part) + 1}. {line}")
    
    lang = random.choice(["Java", "Go", "C++", "Python"])
    formatted = f"## {lang}开发面经\n\n"
    
    if eight_part:
        formatted += "### 八股文\n" + "\n".join(eight_part) + "\n\n"
    
    if project_part:
        formatted += "### 项目/实习\n" + "\n".join(project_part) + "\n\n"
    
    if coding_part:
        formatted += "### 手撕\n" + "\n".join(coding_part) + "\n\n"
    
    if other_part:
        formatted += "### 其他\n" + "\n".join(other_part)
    
    return formatted

def main():
    print("Connecting to database...")
    
    conn = psycopg.connect(
        host="172.31.0.3",
        port=5432,
        dbname="interview",
        user="postgres",
        password="postgres"
    )
    cur = conn.cursor()
    
    cur.execute("SELECT COUNT(*) FROM interview_experience")
    current_count = cur.fetchone()[0]
    print(f"Current count: {current_count}")
    
    # 获取数据库中已有的公司
    cur.execute("SELECT DISTINCT company_name FROM interview_experience")
    existing_companies = set(row[0] for row in cur.fetchall())
    print(f"Existing companies: {existing_companies}")
    
    # 检查目标公司
    target_companies = [c[0] for c in COMPANIES]
    companies_to_add = [c for c in target_companies if c not in existing_companies]
    print(f"Companies to add: {companies_to_add}")
    
    if not companies_to_add:
        print("No new companies to add")
        cur.close()
        conn.close()
        return
    
    # 为每个新公司生成多条数据
    added = 0
    for company in companies_to_add:
        # 找到对应的部门和职位
        dept_pos = next(((d, p) for c, d, p in COMPANIES if c == company), ("技术部", "后端开发"))
        dept, position = dept_pos
        
        # 每家公司生成多条
        for i in range(3):
            lang = random.choice(["Java", "Go", "C++", "Python"])
            
            content = generate_content(company, position, lang)
            formatted_content = format_content(content)
            
            # 检查是否已存在
            cur.execute("""
                SELECT id FROM interview_experience 
                WHERE company_name = %s AND position = %s
            """, (company, position))
            
            if cur.fetchone():
                continue
            
            cur.execute("""
                INSERT INTO interview_experience 
                (company_name, position, experience_type, content, formatted_content, 
                 source_url, source, author, views, likes, created_by, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (
                company,
                position,
                "full_time",
                content,
                formatted_content,
                "",
                "Generated",
                "匿名",
                random.randint(100, 10000),
                random.randint(10, 1000),
                1,
                datetime.now() - timedelta(days=random.randint(1, 30)),
                datetime.now()
            ))
            
            added += 1
            print(f"Added: {company} - {position}")
    
    conn.commit()
    
    cur.execute("SELECT COUNT(*) FROM interview_experience")
    new_count = cur.fetchone()[0]
    print(f"\nTotal added: {added}")
    print(f"New total count: {new_count}")
    
    cur.close()
    conn.close()

if __name__ == "__main__":
    main()
