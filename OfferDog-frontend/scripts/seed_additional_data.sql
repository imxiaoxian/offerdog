-- 为后端开发、移动开发、人工智能、运维/测试添加题库和题目

-- 后端开发题库
INSERT INTO question_banks (name, description, category_id, created_by, created_at, updated_at) VALUES
('Java基础', 'Java 核心概念和编程题', 5, 1, NOW(), NOW()),
('Spring框架', 'Spring Boot、Spring Cloud等框架知识', 5, 1, NOW(), NOW()),
('数据库', 'MySQL、Redis等数据库相关知识', 5, 1, NOW(), NOW()),
('微服务', '微服务架构和设计模式', 5, 1, NOW(), NOW()),
('后端工程化', '后端开发工具和流程', 5, 1, NOW(), NOW());

-- 移动开发题库
INSERT INTO question_banks (name, description, category_id, created_by, created_at, updated_at) VALUES
('Android开发', 'Android 应用开发相关题目', 6, 1, NOW(), NOW()),
('iOS开发', 'iOS 应用开发相关题目', 6, 1, NOW(), NOW()),
('Flutter开发', 'Flutter 跨平台开发相关题目', 6, 1, NOW(), NOW()),
('移动性能优化', '移动应用性能优化方法', 6, 1, NOW(), NOW()),
('移动安全', '移动应用安全相关知识', 6, 1, NOW(), NOW());

-- 人工智能题库
INSERT INTO question_banks (name, description, category_id, created_by, created_at, updated_at) VALUES
('机器学习基础', '机器学习核心概念和算法', 7, 1, NOW(), NOW()),
('深度学习', '深度学习框架和模型', 7, 1, NOW(), NOW()),
('自然语言处理', 'NLP 相关技术和应用', 7, 1, NOW(), NOW()),
('计算机视觉', 'CV 相关技术和应用', 7, 1, NOW(), NOW()),
('AI工程化', 'AI 模型部署和工程实践', 7, 1, NOW(), NOW());

-- 运维/测试题库
INSERT INTO question_banks (name, description, category_id, created_by, created_at, updated_at) VALUES
('测试基础', '软件测试核心概念和方法', 8, 1, NOW(), NOW()),
('自动化测试', '自动化测试框架和工具', 8, 1, NOW(), NOW()),
('DevOps', 'DevOps 理念和实践', 8, 1, NOW(), NOW()),
('容器化技术', 'Docker、Kubernetes等容器技术', 8, 1, NOW(), NOW()),
('监控运维', '系统监控和运维知识', 8, 1, NOW(), NOW());
