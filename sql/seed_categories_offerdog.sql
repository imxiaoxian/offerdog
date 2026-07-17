-- Seed OfferDog default categories tree (level=1 root buckets + level=2 job categories)
-- Idempotent: uses unique (name, parent_id) constraint to avoid duplicates.
-- Requires: categories table exists (see sql/new_table.sql).

BEGIN;

-- Top-level buckets (level=1)
INSERT INTO categories(name, parent_id, level, path, metadata)
VALUES
  ('后端', NULL, 1, ARRAY[0]::integer[], '{}'::jsonb),
  ('前端', NULL, 1, ARRAY[0]::integer[], '{}'::jsonb),
  ('算法', NULL, 1, ARRAY[0]::integer[], '{}'::jsonb),
  ('DevOps', NULL, 1, ARRAY[0]::integer[], '{}'::jsonb),
  ('技术管理', NULL, 1, ARRAY[0]::integer[], '{}'::jsonb)
ON CONFLICT DO NOTHING;

-- Fix path for level=1 roots (path should be [id])
UPDATE categories c
SET path = ARRAY[c.id]::integer[]
WHERE c.deleted_at IS NULL
  AND c.parent_id IS NULL
  AND c.level = 1
  AND (c.path IS NULL OR array_length(c.path, 1) IS DISTINCT FROM 1 OR c.path[1] <> c.id);

-- Level=2 job categories under each bucket
WITH roots AS (
  SELECT id, name FROM categories WHERE deleted_at IS NULL AND parent_id IS NULL AND level=1
),
to_insert AS (
  SELECT r.id AS parent_id, v.name AS child_name
  FROM roots r
  JOIN (VALUES
    ('后端','Java'),
    ('后端','Golang'),
    ('后端','Python'),
    ('后端','C++'),
    ('后端','数据库'),
    ('后端','中间件'),
    ('后端','分布式'),

    ('前端','JavaScript/TypeScript'),
    ('前端','Vue'),
    ('前端','React'),
    ('前端','工程化'),
    ('前端','性能优化'),
    ('前端','Node.js'),

    ('算法','数据结构'),
    ('算法','动态规划'),
    ('算法','图与搜索'),
    ('算法','排序与双指针'),
    ('算法','并发与锁（算法题）'),

    ('DevOps','Docker'),
    ('DevOps','Kubernetes'),
    ('DevOps','Linux'),
    ('DevOps','CI/CD'),
    ('DevOps','监控与可观测性'),

    ('技术管理','项目管理'),
    ('技术管理','团队管理'),
    ('技术管理','架构评审'),
    ('技术管理','沟通协作'),
    ('技术管理','招聘与培养')
  ) AS v(bucket, name) ON v.bucket = r.name
)
INSERT INTO categories(name, parent_id, level, path, metadata)
SELECT child_name, parent_id, 2, ARRAY[parent_id, 0]::integer[], '{}'::jsonb
FROM to_insert
ON CONFLICT DO NOTHING;

-- Fix path for level=2 nodes (path should be [parent_id, id])
UPDATE categories c
SET path = ARRAY[c.parent_id, c.id]::integer[]
WHERE c.deleted_at IS NULL
  AND c.level = 2
  AND c.parent_id IS NOT NULL
  AND (c.path IS NULL OR array_length(c.path, 1) <> 2 OR c.path[1] <> c.parent_id OR c.path[2] <> c.id);

COMMIT;

