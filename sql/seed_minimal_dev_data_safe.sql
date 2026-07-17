-- Minimal dev data seed (safe to re-run).
-- Goal: ensure each leaf category has at least one question bank and some questions,
-- so the backend/UI doesn't look "empty" even when demo data wasn't seeded.
--
-- Notes:
-- - We avoid relying on ON CONFLICT for question_banks/questions because there are no unique constraints.
-- - We insert only when counts are below a threshold to keep it idempotent-ish.
-- - Official/demo questions allow created_by NULL (see questions_created_by_nullable.sql).
DO $$
DECLARE
  cat RECORD;
  v_bank_id BIGINT;
  existing_bank_count INT;
  existing_q_count INT;
BEGIN
  -- For each leaf category (level=2, not deleted), ensure a bank exists.
  FOR cat IN
    SELECT c.id, c.name
    FROM categories c
    WHERE c.deleted_at IS NULL
      AND c.level = 2
  LOOP
    SELECT COUNT(*) INTO existing_bank_count
    FROM question_banks qb
    WHERE qb.deleted_at IS NULL
      AND qb.category_id = cat.id;

    IF existing_bank_count = 0 THEN
      INSERT INTO question_banks(name, description, category_id, created_by)
      VALUES (cat.name || ' 示例题库', '系统自动补齐的示例题库（可重复执行脚本不会重复补齐）', cat.id, NULL)
      RETURNING id INTO v_bank_id;
    ELSE
      -- Pick the earliest bank for this category.
      SELECT qb.id INTO v_bank_id
      FROM question_banks qb
      WHERE qb.deleted_at IS NULL
        AND qb.category_id = cat.id
      ORDER BY qb.id
      LIMIT 1;
    END IF;

    -- Ensure at least 8 questions for that bank/category.
    SELECT COUNT(*) INTO existing_q_count
    FROM questions q
    WHERE q.deleted_at IS NULL
      AND q.bank_id = v_bank_id;

    IF existing_q_count < 8 THEN
      INSERT INTO questions(bank_id, category_id, content, answer, tips, difficulty, source, tags, stats, remark, created_by)
      VALUES
        (v_bank_id, cat.id, '请简要介绍一下你最近做的一个项目，你负责的部分是什么？', NULL, '关注：目标/职责/难点/结果', 'medium', 'official', ARRAY['项目','沟通'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL),
        (v_bank_id, cat.id, '遇到线上故障时，你通常如何定位并推进解决？', NULL, '关注：复现/监控/日志/回滚/复盘', 'medium', 'official', ARRAY['排障','复盘'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL),
        (v_bank_id, cat.id, '解释一下你理解的 RESTful 设计原则。', NULL, '关注：资源/方法/幂等/状态码', 'easy', 'official', ARRAY['REST','HTTP'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL),
        (v_bank_id, cat.id, '说说你做过的性能优化案例，指标是如何量化的？', NULL, '关注：基线/指标/对照/回归', 'medium', 'official', ARRAY['性能','指标'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL),
        (v_bank_id, cat.id, '你如何看待单元测试？在项目中如何落地？', NULL, '关注：收益/边界/覆盖率/分层', 'easy', 'official', ARRAY['测试','工程化'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL),
        (v_bank_id, cat.id, '描述一次你推动过的技术改造/重构，为什么做、怎么做、结果如何？', NULL, '关注：动机/方案/风险/收益', 'hard', 'official', ARRAY['重构','架构'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL),
        (v_bank_id, cat.id, '你如何与产品/设计/后端协作，避免需求偏差？', NULL, '关注：对齐/验收/文档/边界', 'easy', 'official', ARRAY['协作','沟通'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL),
        (v_bank_id, cat.id, '讲讲你理解的缓存策略，以及可能带来的问题。', NULL, '关注：一致性/失效/穿透/雪崩', 'medium', 'official', ARRAY['缓存'], '{"likes":0,"views":0,"favorites":0}', 'seed_minimal', NULL);
    END IF;
  END LOOP;
END $$;

