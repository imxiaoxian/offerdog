-- 初始化面试题库数据
-- 题库数据
INSERT INTO question_banks (name, description, category_id, created_by, created_at, updated_at) VALUES
('Vue前端开发', 'Vue.js 相关面试题目,涵盖基础到高级', 101, 1, NOW(), NOW()),
('React开发', 'React 相关面试题目,包括Hooks、性能优化等', 102, 1, NOW(), NOW()),
('JavaScript基础', 'JavaScript 核心概念和编程题', 103, 1, NOW(), NOW()),
('TypeScript', 'TypeScript 类型系统和高级特性', 104, 1, NOW(), NOW()),
('前端工程化', 'Webpack、Vite、CI/CD等工程化知识', 105, 1, NOW(), NOW());
