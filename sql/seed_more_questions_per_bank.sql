-- Add a few questions for EVERY question bank (idempotent).
-- Requirements:
-- - categories (level=1/2) and question_banks exist
-- - users table has at least one row (questions.created_by is NOT NULL)
-- - questions table exists

BEGIN;

WITH first_user AS (
  SELECT id AS user_id
  FROM users
  WHERE deleted_at IS NULL
  ORDER BY id ASC
  LIMIT 1
),
bank_ctx AS (
  SELECT
    b.id AS bank_id,
    b.category_id,
    c.name AS job_name,
    p.name AS bucket_name
  FROM question_banks b
  JOIN categories c ON c.id = b.category_id AND c.deleted_at IS NULL
  JOIN categories p ON p.id = c.parent_id AND p.deleted_at IS NULL
  WHERE b.deleted_at IS NULL
),
tpl AS (
  SELECT * FROM (VALUES
    (1, 'easy'::question_difficulty,  ARRAY['基础','概念']),
    (2, 'medium'::question_difficulty,ARRAY['原理','实践']),
    (3, 'hard'::question_difficulty,  ARRAY['场景','排障'])
  ) AS t(seq, difficulty, extra_tags)
),
rows AS (
  SELECT
    bc.bank_id,
    bc.category_id,
    bc.bucket_name,
    bc.job_name,
    t.seq,
    t.difficulty,
    (ARRAY[bc.bucket_name, bc.job_name]::text[] || t.extra_tags) AS tags,

    -- content
    CASE bc.bucket_name
      WHEN '后端' THEN
        CASE t.seq
          WHEN 1 THEN bc.job_name || '：解释一次 HTTP 请求从进入网关到返回响应的完整链路（可结合你熟悉的框架）。'
          WHEN 2 THEN bc.job_name || '：如何设计一个高并发接口的限流与熔断方案？说清楚指标、算法与落地细节。'
          ELSE bc.job_name || '：线上接口 RT 飙升，你会如何排查（从入口到依赖、从应用到数据库）？'
        END
      WHEN '前端' THEN
        CASE t.seq
          WHEN 1 THEN bc.job_name || '：解释浏览器渲染流程（从 HTML/CSS 到布局/绘制/合成）以及关键性能指标。'
          WHEN 2 THEN bc.job_name || '：大型前端项目如何做性能优化（首屏、交互、包体、缓存）？给出可验证的指标。'
          ELSE bc.job_name || '：线上出现白屏/卡顿/内存暴涨，你如何定位（日志、监控、埋点、性能面板）？'
        END
      WHEN '算法' THEN
        CASE t.seq
          WHEN 1 THEN bc.job_name || '：请给出该模块一个典型题的思路（状态定义/不变量/边界），并写出复杂度分析。'
          WHEN 2 THEN bc.job_name || '：如何从暴力解法优化到可接受复杂度？说明剪枝/DP/贪心/数据结构选择依据。'
          ELSE bc.job_name || '：如果输入规模扩大 100 倍，你会如何改造（优化复杂度、常数、内存、并行）？'
        END
      WHEN 'DevOps' THEN
        CASE t.seq
          WHEN 1 THEN bc.job_name || '：解释你在该模块里最常用的 3 个命令/对象，以及它们分别解决什么问题。'
          WHEN 2 THEN bc.job_name || '：如何设计一条可回滚的发布流水线（含灰度/监控/告警/回滚策略）？'
          ELSE bc.job_name || '：生产事故复盘：你会如何做“预防同类问题再次发生”的工程化改进？'
        END
      WHEN '技术管理' THEN
        CASE t.seq
          WHEN 1 THEN bc.job_name || '：你如何在保证交付的同时控制范围蔓延（scope creep）？'
          WHEN 2 THEN bc.job_name || '：如何做技术决策（评审、取舍、风险）并推动团队达成一致？'
          ELSE bc.job_name || '：跨团队协作冲突时你如何处理（目标对齐、沟通机制、升级路径）？'
        END
      ELSE
        bc.job_name || '：请给出该方向一个核心概念的解释，并结合真实项目说明你如何应用。'
    END AS content,

    -- answer
    CASE bc.bucket_name
      WHEN '后端' THEN
        CASE t.seq
          WHEN 1 THEN '覆盖入口（LB/网关/鉴权）→ 应用（线程池/过滤器/中间件/日志）→ 业务（事务/幂等/缓存）→ 依赖（DB/Redis/MQ）→ 返回（序列化/压缩/缓存头）。重点是能说清楚每层的关键点与瓶颈。'
          WHEN 2 THEN '明确目标（QPS、延迟、错误率）与保护对象；限流可用令牌桶/漏桶/滑窗，分层（网关/应用/接口）；熔断与隔离（线程池/舱壁）、降级策略、监控与压测验证。'
          ELSE '从监控确定范围→ 看网关与应用指标→ 采样链路追踪→ 查慢 SQL/依赖超时→ 排查 GC/线程池/锁竞争→ 回滚或降级止血→ 根因修复与回归。'
        END
      WHEN '前端' THEN
        CASE t.seq
          WHEN 1 THEN 'HTML 解析→ DOM，CSS 解析→ CSSOM，合成渲染树→ Layout→ Paint→ Composite；关注 FCP/LCP/CLS/INP，理解重排/重绘/合成层与 GPU。'
          WHEN 2 THEN '拆分：按路由/组件分包、预加载；缓存：HTTP cache、Service Worker；渲染：虚拟列表、减少不必要渲染；资源：图片/字体优化；用 Lighthouse/Performance API 验证改动。'
          ELSE '复现与采集：Sentry/自研埋点；Performance 面板定位长任务；检查资源加载与错误；排查内存快照与泄漏；逐步二分发布定位；修复后加监控与报警。'
        END
      WHEN '算法' THEN
        CASE t.seq
          WHEN 1 THEN '给出清晰的状态/不变量与边界；说明为什么这样定义能覆盖所有情况；写出时间/空间复杂度。'
          WHEN 2 THEN '识别重复子问题/最优子结构或贪心性质；选择合适数据结构（堆/单调队列/并查集/线段树等）降低复杂度。'
          ELSE '如果不能再降复杂度，则考虑并行、分治、预处理、压缩存储、外存/流式处理，并解释 trade-off。'
        END
      WHEN 'DevOps' THEN
        CASE t.seq
          WHEN 1 THEN '回答要包含“对象/命令是什么、什么时候用、如何验证结果、常见坑”。'
          WHEN 2 THEN '流水线：构建→测试→制品→部署→健康检查→流量切换；灰度策略（按用户/比例/区域）；回滚条件与自动化；以指标驱动放量。'
          ELSE '工程化：SLO/错误预算、演练、Runbook、自动化修复、变更治理、监控告警收敛。'
        END
      WHEN '技术管理' THEN
        CASE t.seq
          WHEN 1 THEN '用“目标-范围-里程碑-变更流程”框住：定义 Done、评审变更、量化影响并对齐优先级。'
          WHEN 2 THEN '收集方案与数据→评审与决策记录→明确 owner/时间表→同步机制→复盘沉淀为规范。'
          ELSE '先对齐目标与事实，再对齐方案；建立沟通节奏（例会/同步文档）；必要时升级到更高层做裁决。'
        END
      ELSE '回答包含：概念解释、应用场景、风险与边界、以及一次真实案例。'
    END AS answer,

    -- tips
    CASE t.seq
      WHEN 1 THEN '尽量用 1 个真实例子解释概念。'
      WHEN 2 THEN '把“指标、方案、验证”说完整。'
      ELSE '要包含“止血、定位、修复、复盘”。'
    END AS tips
  FROM bank_ctx bc
  CROSS JOIN tpl t
)
INSERT INTO questions(bank_id, category_id, content, answer, tips, difficulty, source, tags, stats, created_by, remark)
SELECT
  r.bank_id,
  r.category_id,
  r.content,
  r.answer,
  r.tips,
  r.difficulty,
  'official'::question_source,
  r.tags,
  '{}'::jsonb,
  (SELECT user_id FROM first_user),
  'offerdog_seed_more'
FROM rows r
WHERE (SELECT user_id FROM first_user) IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM questions q
    WHERE q.deleted_at IS NULL
      AND q.bank_id = r.bank_id
      AND q.content = r.content
  );

COMMIT;

