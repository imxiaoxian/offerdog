-- 演示用：在至少存在一条二级岗位分类时，插入一个官方题库（created_by 为空），便于面试题库页有数据。
-- 在 psql 中执行：\i seed_demo_question_bank.sql   或 由 DBA 按需运行。
-- 若已存在同名未删除题库则跳过。

INSERT INTO question_banks (name, description, category_id, created_by, created_at, updated_at)
SELECT '演示题库',
       '手动种子：可在管理端继续添加题目',
       (SELECT id FROM categories WHERE level = 2 AND deleted_at IS NULL ORDER BY id LIMIT 1),
       NULL,
       NOW(),
       NOW()
WHERE EXISTS (SELECT 1 FROM categories WHERE level = 2 AND deleted_at IS NULL)
  AND NOT EXISTS (
    SELECT 1 FROM question_banks WHERE name = '演示题库' AND deleted_at IS NULL
  );
