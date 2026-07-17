-- 面试计划模板数据
INSERT INTO interview_plan_templates (plan_template_id, template_id, plan_name, plan_structure, usage_count, avg_completion_rate, is_active)
VALUES 
(
    gen_random_uuid(),
    '82cda905-efe1-4a94-9de2-ac31e01c7986',
    '前端工程师标准计划',
    '{
        "total_questions": 5,
        "estimated_duration": 45,
        "targetLevel": "mid",
        "questions": [
            {
                "question_id": "q1",
                "topic": "HTML/CSS基础",
                "category": "technical",
                "initial_question": "请解释一下CSS盒模型，以及如何解决margin塌陷问题？",
                "max_depth": 3,
                "time_allocation": 8,
                "evaluation_criteria": {
                    "key_points": ["盒模型组成", "IE盒模型", "BFC原理", "margin塌陷解决方案"],
                    "difficulty": "easy"
                }
            },
            {
                "question_id": "q2",
                "topic": "JavaScript核心",
                "category": "technical",
                "initial_question": "请解释JavaScript中的闭包是什么？它有什么优缺点？",
                "max_depth": 3,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["闭包定义", "作用域链", "内存泄漏", "实际应用场景"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q3",
                "topic": "Vue/React框架",
                "category": "technical",
                "initial_question": "请解释Vue3的响应式原理，以及Composition API的优势？",
                "max_depth": 3,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["Proxy vs Object.defineProperty", "依赖收集", "Composition API", "性能优化"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q4",
                "topic": "性能优化",
                "category": "technical",
                "initial_question": "前端性能优化有哪些常用手段？请从加载、渲染、缓存三个方面说明。",
                "max_depth": 2,
                "time_allocation": 8,
                "evaluation_criteria": {
                    "key_points": ["CDN", "代码分割", "懒加载", "回流重绘", "缓存策略"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q5",
                "topic": "项目经验",
                "category": "experience",
                "initial_question": "请介绍一个你最引以为豪的前端项目，你在其中承担了什么角色？遇到了哪些技术挑战？",
                "max_depth": 2,
                "time_allocation": 9,
                "evaluation_criteria": {
                    "key_points": ["项目复杂度", "技术选型", "问题解决能力", "团队协作"],
                    "difficulty": "mid"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 6.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }'::jsonb,
    0, NULL, true
),
(
    gen_random_uuid(),
    'a7f22f28-3e69-4e3f-a258-76f6163ea790',
    '后端工程师标准计划',
    '{
        "total_questions": 5,
        "estimated_duration": 50,
        "targetLevel": "mid",
        "questions": [
            {
                "question_id": "q1",
                "topic": "Java基础",
                "category": "technical",
                "initial_question": "请解释一下Java中的集合类框架，以及ArrayList和LinkedList的区别？",
                "max_depth": 3,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["Collection接口", "List/Set/Map", "ArrayList vs LinkedList", "时间复杂度"],
                    "difficulty": "easy"
                }
            },
            {
                "question_id": "q2",
                "topic": "Spring框架",
                "category": "technical",
                "initial_question": "请解释Spring的IoC和AOP原理，以及Bean的生命周期？",
                "max_depth": 3,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["IoC/DI", "AOP原理", "Bean生命周期", "代理模式"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q3",
                "topic": "数据库",
                "category": "technical",
                "initial_question": "请解释数据库事务的ACID特性，以及MySQL的隔离级别？",
                "max_depth": 3,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["ACID", "隔离级别", "脏读/不可重复读/幻读", "锁机制"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q4",
                "topic": "分布式系统",
                "category": "technical",
                "initial_question": "请解释什么是CAP定理，以及分布式系统中如何做负载均衡？",
                "max_depth": 2,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["CAP定理", "BASE理论", "负载均衡算法", "一致性哈希"],
                    "difficulty": "senior"
                }
            },
            {
                "question_id": "q5",
                "topic": "项目经验",
                "category": "experience",
                "initial_question": "请介绍一个你参与的后端系统架构设计，你是如何保证系统高可用性的？",
                "max_depth": 2,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["架构设计", "高可用方案", "性能瓶颈", "解决方案"],
                    "difficulty": "mid"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 6.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }'::jsonb,
    0, NULL, true
),
(
    gen_random_uuid(),
    '66588078-6abd-4121-8379-b02301ffbd7e',
    '全栈工程师标准计划',
    '{
        "total_questions": 5,
        "estimated_duration": 60,
        "targetLevel": "mid",
        "questions": [
            {
                "question_id": "q1",
                "topic": "前端基础",
                "category": "technical",
                "initial_question": "请解释一下浏览器的事件循环机制，以及async/await和Promise的关系？",
                "max_depth": 3,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["Event Loop", "宏任务/微任务", "Promise", "async/await"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q2",
                "topic": "后端基础",
                "category": "technical",
                "initial_question": "请解释RESTful API设计原则，以及GraphQL的优缺点？",
                "max_depth": 2,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["REST原则", "HTTP方法", "GraphQL", "API设计最佳实践"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q3",
                "topic": "数据库",
                "category": "technical",
                "initial_question": "请解释NoSQL和关系型数据库的区别，以及何时应该选择哪种类型？",
                "max_depth": 2,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["SQL vs NoSQL", "事务支持", "扩展性", "使用场景"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q4",
                "topic": "DevOps",
                "category": "technical",
                "initial_question": "请解释CI/CD的概念，以及Docker和Kubernetes在项目中的应用？",
                "max_depth": 2,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["CI/CD流程", "Dockerfile", "K8s架构", "容器编排"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q5",
                "topic": "系统设计",
                "category": "experience",
                "initial_question": "如果你要设计一个支持万人同时在线的聊天系统，你会如何架构？",
                "max_depth": 2,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["高并发", "WebSocket", "消息队列", "缓存策略"],
                    "difficulty": "senior"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 6.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }'::jsonb,
    0, NULL, true
),
(
    gen_random_uuid(),
    '63fe8d80-46e5-40ef-bcb1-caffc088ea5f',
    '算法工程师标准计划',
    '{
        "total_questions": 5,
        "estimated_duration": 60,
        "targetLevel": "mid",
        "questions": [
            {
                "question_id": "q1",
                "topic": "数据结构基础",
                "category": "technical",
                "initial_question": "请手写一个二分查找算法，并分析其时间复杂度和适用场景？",
                "max_depth": 3,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["二分查找", "边界条件", "时间复杂度", "变体问题"],
                    "difficulty": "easy"
                }
            },
            {
                "question_id": "q2",
                "topic": "动态规划",
                "category": "technical",
                "initial_question": "请解释什么是动态规划，以及如何使用动态规划解决最长公共子序列问题？",
                "max_depth": 3,
                "time_allocation": 15,
                "evaluation_criteria": {
                    "key_points": ["DP思想", "状态定义", "状态转移", "空间优化"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q3",
                "topic": "图算法",
                "category": "technical",
                "initial_question": "请解释BFS和DFS的区别，以及如何用BFS解决最短路径问题？",
                "max_depth": 2,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["BFS vs DFS", "队列/栈", "最短路径", "适用范围"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q4",
                "topic": "机器学习基础",
                "category": "technical",
                "initial_question": "请解释监督学习和无监督学习的区别，以及常见的分类和聚类算法？",
                "max_depth": 2,
                "time_allocation": 12,
                "evaluation_criteria": {
                    "key_points": ["监督vs无监督", "分类算法", "聚类算法", "评估指标"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q5",
                "topic": "项目经验",
                "category": "experience",
                "initial_question": "请介绍一个你用机器学习解决的实际问题，你的方案是什么？效果如何？",
                "max_depth": 2,
                "time_allocation": 9,
                "evaluation_criteria": {
                    "key_points": ["问题定义", "特征工程", "模型选择", "效果评估"],
                    "difficulty": "mid"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 6.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }'::jsonb,
    0, NULL, true
),
(
    gen_random_uuid(),
    'fb37ea67-a7e9-49bb-b904-79b0c970706e',
    '初级前端工程师计划',
    '{
        "total_questions": 4,
        "estimated_duration": 30,
        "targetLevel": "junior",
        "questions": [
            {
                "question_id": "q1",
                "topic": "HTML/CSS",
                "category": "technical",
                "initial_question": "请解释一下什么是CSS选择器优先级，以及如何清除浮动？",
                "max_depth": 2,
                "time_allocation": 8,
                "evaluation_criteria": {
                    "key_points": ["优先级规则", "!important", "clear:both", "BFC"],
                    "difficulty": "easy"
                }
            },
            {
                "question_id": "q2",
                "topic": "JavaScript基础",
                "category": "technical",
                "initial_question": "请解释JavaScript的数据类型，以及typeof和instanceof的区别？",
                "max_depth": 2,
                "time_allocation": 8,
                "evaluation_criteria": {
                    "key_points": ["基本类型", "引用类型", "typeof", "instanceof"],
                    "difficulty": "easy"
                }
            },
            {
                "question_id": "q3",
                "topic": "Vue基础",
                "category": "technical",
                "initial_question": "请解释Vue的双向绑定原理，以及v-if和v-show的区别？",
                "max_depth": 2,
                "time_allocation": 7,
                "evaluation_criteria": {
                    "key_points": ["数据绑定", "v-if vs v-show", "DOM操作", "生命周期"],
                    "difficulty": "easy"
                }
            },
            {
                "question_id": "q4",
                "topic": "项目介绍",
                "category": "experience",
                "initial_question": "请介绍一下你学习前端的过程，以及做过的练习项目？",
                "max_depth": 1,
                "time_allocation": 7,
                "evaluation_criteria": {
                    "key_points": ["学习路径", "项目经验", "技术栈", "成长规划"],
                    "difficulty": "easy"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 5.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }'::jsonb,
    0, NULL, true
),
(
    gen_random_uuid(),
    '45ab10f4-3046-4ed8-809a-fec21998260f',
    '技术团队负责人计划',
    '{
        "total_questions": 5,
        "estimated_duration": 90,
        "targetLevel": "senior",
        "questions": [
            {
                "question_id": "q1",
                "topic": "技术决策",
                "category": "technical",
                "initial_question": "请介绍一下你在技术选型方面的经验，你通常如何评估和选择技术栈？",
                "max_depth": 3,
                "time_allocation": 18,
                "evaluation_criteria": {
                    "key_points": ["技术评估", "团队能力", "业务需求", "长期维护"],
                    "difficulty": "senior"
                }
            },
            {
                "question_id": "q2",
                "topic": "团队管理",
                "category": "management",
                "initial_question": "请介绍一下你的团队管理经验，如何进行技术人员的招聘和培养？",
                "max_depth": 3,
                "time_allocation": 18,
                "evaluation_criteria": {
                    "key_points": ["招聘策略", "人才培养", "绩效管理", "团队建设"],
                    "difficulty": "senior"
                }
            },
            {
                "question_id": "q3",
                "topic": "项目管理",
                "category": "management",
                "initial_question": "请解释敏捷开发流程，以及如何在团队中实施Scrum或Kanban？",
                "max_depth": 2,
                "time_allocation": 18,
                "evaluation_criteria": {
                    "key_points": ["敏捷原则", "Scrum/Kanban", "迭代管理", "风险控制"],
                    "difficulty": "senior"
                }
            },
            {
                "question_id": "q4",
                "topic": "系统架构",
                "category": "technical",
                "initial_question": "请介绍一个你主导的系统架构升级案例，你是如何规划和执行的？",
                "max_depth": 3,
                "time_allocation": 18,
                "evaluation_criteria": {
                    "key_points": ["架构设计", "迁移方案", "风险控制", "效果评估"],
                    "difficulty": "senior"
                }
            },
            {
                "question_id": "q5",
                "topic": "职业规划",
                "category": "experience",
                "initial_question": "请谈谈你对技术团队负责人这个角色的理解，以及你的职业规划？",
                "max_depth": 2,
                "time_allocation": 18,
                "evaluation_criteria": {
                    "key_points": ["角色认知", "能力模型", "职业目标", "成长路径"],
                    "difficulty": "senior"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 7.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }'::jsonb,
    0, NULL, true
),
(
    gen_random_uuid(),
    '39de68b1-363c-4cf6-b6c0-345d047fee79',
    'DevOps工程师标准计划',
    '{
        "total_questions": 5,
        "estimated_duration": 50,
        "targetLevel": "mid",
        "questions": [
            {
                "question_id": "q1",
                "topic": "容器技术",
                "category": "technical",
                "initial_question": "请解释Docker的原理，以及Dockerfile的最佳实践？",
                "max_depth": 3,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["容器原理", "镜像分层", "Dockerfile优化", "多阶段构建"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q2",
                "topic": "Kubernetes",
                "category": "technical",
                "initial_question": "请解释Kubernetes的核心概念，包括Pod、Service、Deployment的区别？",
                "max_depth": 3,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["K8s架构", "Pod生命周期", "Service类型", "Deployment策略"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q3",
                "topic": "CI/CD",
                "category": "technical",
                "initial_question": "请解释CI/CD的概念，以及如何在GitLab或Jenkins中配置自动化部署？",
                "max_depth": 2,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["CI/CD流程", "流水线配置", "自动化测试", "部署策略"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q4",
                "topic": "监控与日志",
                "category": "technical",
                "initial_question": "请介绍常用的监控和日志方案，以及如何构建可观测性系统？",
                "max_depth": 2,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["Prometheus", "Grafana", "ELK", "链路追踪"],
                    "difficulty": "mid"
                }
            },
            {
                "question_id": "q5",
                "topic": "项目经验",
                "category": "experience",
                "initial_question": "请介绍一个你完成的DevOps项目，你遇到了哪些挑战？如何解决的？",
                "max_depth": 2,
                "time_allocation": 10,
                "evaluation_criteria": {
                    "key_points": ["项目背景", "技术方案", "问题解决", "效果评估"],
                    "difficulty": "mid"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 6.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }'::jsonb,
    0, NULL, true
);
