-- ============================================
-- AI模拟面试平台数据库设计
-- 数据库: PostgreSQL 12+
-- 特性: UUID、JSONB、触发器、分区表、全文搜索
-- ============================================

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
-- 用于模糊搜索

-- ============================================
-- 1. 面试模板表
-- 定义不同类型的面试(前端、后端、产品等)
-- ============================================
CREATE TABLE interview_templates
(
    template_id                UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    template_name              VARCHAR(200) NOT NULL,
    description                TEXT,
    category                   VARCHAR(100) NOT NULL,                  -- '前端工程师', '后端工程师', 'HR面试'等
    difficulty_level           VARCHAR(50)  NOT NULL    DEFAULT 'mid', -- 'junior', 'mid', 'senior'
    estimated_duration_minutes INT                      DEFAULT 30,    -- 预计时长

    -- AI面试官的系统提示词模板
    system_prompt              TEXT         NOT NULL,

    -- 默认的面试配置
    default_config             JSONB                    DEFAULT '{
      "enable_follow_up": true,
      "max_depth_per_question": 3,
      "time_warning_enabled": true
    }'::jsonb,

    -- 状态管理
    is_active                  BOOLEAN                  DEFAULT true,
    version                    INT                      DEFAULT 1,

    created_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_difficulty CHECK (difficulty_level IN ('junior', 'mid', 'senior'))
);

-- 索引
CREATE INDEX idx_templates_category ON interview_templates (category) WHERE is_active = true;
CREATE INDEX idx_templates_difficulty ON interview_templates (difficulty_level) WHERE is_active = true;

-- 注释
COMMENT ON TABLE interview_templates IS '面试模板表,定义不同类型和难度的面试';
COMMENT ON COLUMN interview_templates.system_prompt IS 'AI面试官的系统提示词,包含角色设定和行为规则';

-- ============================================
-- 2. 面试计划模板表
-- 预定义的面试问题流程和计划
-- ============================================
CREATE TABLE interview_plan_templates
(
    plan_template_id    UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    template_id         UUID         NOT NULL REFERENCES interview_templates (template_id) ON DELETE CASCADE,
    plan_name           VARCHAR(200) NOT NULL,

    -- 预定义的面试计划结构
    plan_structure      JSONB        NOT NULL,
    /*
    结构示例:
    {
        "total_questions": 5,
        "estimated_duration": 30,
        "questions": [
            {
                "question_id": "q1",
                "topic": "JavaScript基础",
                "category": "technical",
                "initial_question": "请解释JavaScript中的闭包",
                "max_depth": 3,
                "time_allocation": 5,
                "evaluation_criteria": {
                    "key_points": ["作用域", "变量捕获", "实际应用"],
                    "difficulty": "mid"
                }
            }
        ],
        "transition_rules": {
            "min_score_to_proceed": 6.0,
            "auto_end_conditions": ["all_questions_completed", "time_exceeded"]
        }
    }
    */

    -- 使用统计
    usage_count         INT                      DEFAULT 0,
    avg_completion_rate DECIMAL(5, 2),

    is_active           BOOLEAN                  DEFAULT true,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT plan_structure_check CHECK (
        jsonb_typeof(plan_structure) = 'object' AND
        plan_structure ? 'questions' AND
        jsonb_typeof(plan_structure -> 'questions') = 'array'
        )
);

-- 索引
CREATE INDEX idx_plan_templates_template ON interview_plan_templates (template_id) WHERE is_active = true;
CREATE INDEX idx_plan_templates_usage ON interview_plan_templates (usage_count DESC) WHERE is_active = true;

COMMENT ON TABLE interview_plan_templates IS '面试计划模板,包含预定义的问题流程';

-- ============================================
-- 3. 面试会话表 (核心表)
-- 记录每次面试的主信息和动态计划
-- ============================================
CREATE TABLE interview_sessions
(
    session_id             UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    user_id                BIGINT NOT NULL REFERENCES users (id), -- 外键关联到你现有的users表
    template_id            UUID        NOT NULL REFERENCES interview_templates (template_id),
    plan_template_id       UUID REFERENCES interview_plan_templates (plan_template_id),

    -- 会话状态
    status                 VARCHAR(50) NOT NULL     DEFAULT 'in_progress',
    -- 'in_progress': 进行中
    -- 'completed': 已完成
    -- 'abandoned': 用户中途退出
    -- 'expired': 超时未完成

    -- 动态面试计划 (会在面试过程中更新)
    interview_plan         JSONB       NOT NULL,
    /*
    动态结构:
    {
        "total_questions": 5,
        "current_question_index": 0,
        "questions_completed": 0,
        "questions": [
            {
                "question_id": "q1",
                "topic": "JavaScript闭包",
                "status": "current", // pending, current, in_depth, completed, skipped
                "depth_level": 1,
                "max_depth": 3,
                "start_time": "2025-11-15T10:00:00Z",
                "end_time": null,
                "follow_up_count": 0
            }
        ],
        "adaptive_adjustments": {
            "difficulty_adjusted": false,
            "skipped_questions": [],
            "added_questions": []
        }
    }
    */

    -- 进度跟踪
    current_question_index INT                      DEFAULT 0,
    questions_completed    INT                      DEFAULT 0,
    total_messages_count   INT                      DEFAULT 0,

    -- 时间管理
    start_time             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time               TIMESTAMP WITH TIME ZONE,
    expected_end_time      TIMESTAMP WITH TIME ZONE,        -- 预计结束时间
    duration_minutes       INT GENERATED ALWAYS AS (
        CASE
            WHEN end_time IS NOT NULL
                THEN EXTRACT(EPOCH FROM (end_time - start_time)) / 60
            END
        ) STORED,

    -- 元数据
    metadata               JSONB                    DEFAULT '{}'::jsonb,
    -- 可存储: 用户设备信息、中断次数、网络质量等

    created_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_status CHECK (status IN ('in_progress', 'completed', 'abandoned', 'expired')),
    CONSTRAINT check_end_time CHECK (end_time IS NULL OR end_time >= start_time)
);

-- 索引优化
CREATE INDEX idx_sessions_user_status ON interview_sessions (user_id, status, created_at DESC);
CREATE INDEX idx_sessions_status ON interview_sessions (status) WHERE status = 'in_progress';
CREATE INDEX idx_sessions_template ON interview_sessions (template_id, created_at DESC);
CREATE INDEX idx_sessions_created ON interview_sessions (created_at DESC);

-- JSONB字段的GIN索引,加速JSON查询
CREATE INDEX idx_sessions_plan_gin ON interview_sessions USING gin (interview_plan);

-- 分区表 (按月分区,适合大量历史数据)
-- 如果未来数据量大,可以考虑启用分区
-- CREATE TABLE interview_sessions_2025_11 PARTITION OF interview_sessions
-- FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

COMMENT ON TABLE interview_sessions IS '面试会话表,存储每次面试的主信息和动态计划';
COMMENT ON COLUMN interview_sessions.interview_plan IS '动态面试计划,AI根据回答质量实时调整';

-- ============================================
-- 4. 对话记录表
-- 存储完整的问答历史,用于构建AI上下文
-- ============================================
CREATE TABLE conversation_messages
(
    message_id          UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    session_id          UUID        NOT NULL REFERENCES interview_sessions (session_id) ON DELETE CASCADE,

    -- 消息角色和内容
    role                VARCHAR(20) NOT NULL, -- 'interviewer' 或 'candidate'
    content             TEXT        NOT NULL,

    -- 关联信息
    related_question_id VARCHAR(100),         -- 关联到 interview_plan 中的 question_id

    -- 序列和顺序
    sequence_number     INT         NOT NULL,

    -- Token管理 (用于控制上下文窗口)
    token_count         INT,
    estimated_tokens    INT,                  -- 粗略估算: 字符数/4 (中文) 或 单词数*1.3 (英文)

    -- AI实时评估 (轻量级,非最终报告)
    ai_quick_eval       JSONB,
    /*
    {
        "relevance_score": 0.85,
        "clarity_score": 0.9,
        "completeness_score": 0.7,
        "should_follow_up": true,
        "suggested_direction": "请举个实际项目中的例子",
        "detected_issues": ["回答过于理论化", "缺少代码示例"]
    }
    */

    timestamp           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_role CHECK (role IN ('interviewer', 'candidate')),
    CONSTRAINT unique_sequence_per_session UNIQUE (session_id, sequence_number)
);

-- 索引优化
CREATE INDEX idx_messages_session_seq ON conversation_messages (session_id, sequence_number);
CREATE INDEX idx_messages_session_role ON conversation_messages (session_id, role);
CREATE INDEX idx_messages_timestamp ON conversation_messages (session_id, timestamp DESC);
CREATE INDEX idx_messages_question ON conversation_messages (session_id, related_question_id)
    WHERE related_question_id IS NOT NULL;

-- GIN索引用于快速JSON查询
CREATE INDEX idx_messages_eval_gin ON conversation_messages USING gin (ai_quick_eval);

-- 全文搜索索引 (用于搜索对话内容)
CREATE INDEX idx_messages_content_fts ON conversation_messages USING gin (to_tsvector('simple', content));

COMMENT ON TABLE conversation_messages IS '对话记录表,存储完整的面试问答历史';
COMMENT ON COLUMN conversation_messages.token_count IS '实际token数,用于精确控制AI上下文长度';

-- ============================================
-- 5. 面试报告表
-- AI生成的最终评估报告
-- ============================================
CREATE TABLE interview_reports
(
    report_id              UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    session_id             UUID         NOT NULL UNIQUE REFERENCES interview_sessions (session_id) ON DELETE CASCADE,

    -- 总体评分
    overall_score          DECIMAL(3, 1) CHECK (overall_score >= 0 AND overall_score <= 10),
    pass_status            BOOLEAN,               -- 是否通过
    confidence_level       VARCHAR(20),           -- 'high', 'medium', 'low' - AI对评分的信心水平

    -- 详细报告内容 (结构化存储)
    report_content         JSONB        NOT NULL,
    /*
    {
        "summary": "候选人展现出扎实的JavaScript基础...",
        "strengths": [
            "逻辑思维清晰",
            "能够举一反三",
            "代码规范性好"
        ],
        "weaknesses": [
            "对异步编程理解不够深入",
            "缺少大型项目经验",
            "回答时间较长,思考不够迅速"
        ],
        "dimension_scores": {
            "technical_knowledge": 8.5,
            "problem_solving": 7.0,
            "communication": 8.0,
            "code_quality": 7.5,
            "learning_ability": 8.5,
            "practical_experience": 6.5
        },
        "question_analysis": [
            {
                "question_id": "q1",
                "question": "请解释JavaScript闭包",
                "answer_summary": "候选人准确解释了闭包的概念...",
                "score": 8.0,
                "time_spent_minutes": 4.5,
                "follow_up_count": 2,
                "feedback": "回答准确且有深度,但缺少实际应用案例",
                "key_points_covered": ["作用域链", "变量捕获"],
                "key_points_missed": ["性能考虑", "内存泄漏"]
            }
        ],
        "skill_assessment": {
            "JavaScript": 8.0,
            "ES6+": 7.5,
            "异步编程": 6.5,
            "算法": 7.0
        },
        "recommendations": [
            "建议系统学习Promise和async/await的实现原理",
            "多参与大型项目,积累实战经验",
            "可以通过LeetCode等平台提升算法能力"
        ],
        "interview_level_match": {
            "applied_level": "mid",
            "evaluated_level": "mid",
            "match": true,
            "suggested_level": "mid"
        }
    }
    */

    -- 纯文本版本 (用于展示和导出)
    report_text            TEXT,
    report_html            TEXT,

    -- AI生成信息
    ai_model               VARCHAR(100) NOT NULL, -- 'claude-sonnet-4', 'gpt-4'等
    generation_prompt      TEXT,                  -- 用于生成报告的提示词(可选,用于调试)
    generation_tokens_used INT,
    generation_duration_ms INT,

    -- 报告版本 (如果用户要求重新生成)
    version                INT                      DEFAULT 1,

    -- 审核状态 (可选,如果需要人工审核)
    review_status          VARCHAR(50)              DEFAULT 'pending',
    reviewed_by            UUID,                  -- 审核人ID
    reviewed_at            TIMESTAMP WITH TIME ZONE,
    review_notes           TEXT,

    created_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_confidence CHECK (confidence_level IN ('high', 'medium', 'low')),
    CONSTRAINT check_review_status CHECK (review_status IN ('pending', 'approved', 'rejected', 'revised'))
);

-- 索引
CREATE INDEX idx_reports_session ON interview_reports (session_id);
CREATE INDEX idx_reports_score ON interview_reports (overall_score DESC);
CREATE INDEX idx_reports_created ON interview_reports (created_at DESC);
CREATE INDEX idx_reports_review ON interview_reports (review_status) WHERE review_status = 'pending';

-- GIN索引用于报告内容搜索
CREATE INDEX idx_reports_content_gin ON interview_reports USING gin (report_content);

COMMENT ON TABLE interview_reports IS 'AI生成的面试评估报告';
COMMENT ON COLUMN interview_reports.report_content IS '结构化的报告内容,包含多维度评分和详细反馈';

-- ============================================
-- 6. 辅助表: 用户反馈表 (可选)
-- 收集用户对面试体验的反馈
-- ============================================
CREATE TABLE interview_feedback
(
    feedback_id       UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    session_id        UUID NOT NULL REFERENCES interview_sessions (session_id) ON DELETE CASCADE,
    user_id           BIGINT NOT NULL REFERENCES users (id),

    -- 评分
    experience_rating INT CHECK (experience_rating BETWEEN 1 AND 5),
    ai_quality_rating INT CHECK (ai_quality_rating BETWEEN 1 AND 5),
    difficulty_rating INT CHECK (difficulty_rating BETWEEN 1 AND 5), -- 1=太简单, 5=太难

    -- 反馈内容
    comments          TEXT,
    issues_reported   TEXT[],

    -- 是否愿意推荐
    would_recommend   BOOLEAN,

    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_session ON interview_feedback (session_id);
CREATE INDEX idx_feedback_rating ON interview_feedback (experience_rating, created_at DESC);

-- ============================================
-- 触发器: 自动更新 updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_interview_templates_updated_at
    BEFORE UPDATE
    ON interview_templates
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_interview_sessions_updated_at
    BEFORE UPDATE
    ON interview_sessions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_plan_templates_updated_at
    BEFORE UPDATE
    ON interview_plan_templates
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 触发器: 自动更新消息计数
-- ============================================
CREATE OR REPLACE FUNCTION update_session_message_count()
    RETURNS TRIGGER AS
$$
BEGIN
    UPDATE interview_sessions
    SET total_messages_count = total_messages_count + 1
    WHERE session_id = NEW.session_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER increment_message_count
    AFTER INSERT
    ON conversation_messages
    FOR EACH ROW
EXECUTE FUNCTION update_session_message_count();

-- ============================================
-- 视图: 面试会话概览
-- 方便查询会话的关键信息
-- ============================================
CREATE VIEW v_interview_sessions_overview AS
SELECT s.session_id,
       s.user_id,
       s.status,
       s.start_time,
       s.end_time,
       s.duration_minutes,
       t.template_name,
       t.category,
       t.difficulty_level,
       s.questions_completed,
       s.total_messages_count,
       (s.interview_plan ->> 'total_questions')::int as total_questions,
       CASE
           WHEN s.questions_completed > 0 AND (s.interview_plan ->> 'total_questions')::int > 0
               THEN ROUND(s.questions_completed::numeric / (s.interview_plan ->> 'total_questions')::numeric * 100, 2)
           ELSE 0
           END                                       as completion_percentage,
       r.overall_score,
       r.pass_status,
       s.created_at
FROM interview_sessions s
         LEFT JOIN interview_templates t ON s.template_id = t.template_id
         LEFT JOIN interview_reports r ON s.session_id = r.session_id;

COMMENT ON VIEW v_interview_sessions_overview IS '面试会话概览视图,汇总关键信息';

-- ============================================
-- 视图: 用户面试历史统计
-- ============================================
CREATE VIEW v_user_interview_stats AS
SELECT user_id,
       COUNT(*)                                                       as total_interviews,
       COUNT(*) FILTER (WHERE status = 'completed')                   as completed_interviews,
       COUNT(*) FILTER (WHERE status = 'abandoned')                   as abandoned_interviews,
       ROUND(AVG(duration_minutes), 2)                                as avg_duration_minutes,
       ROUND(AVG((interview_plan ->> 'total_questions')::numeric), 2) as avg_questions_per_interview,
       MAX(created_at)                                                as last_interview_date
FROM interview_sessions
GROUP BY user_id;

COMMENT ON VIEW v_user_interview_stats IS '用户面试统计视图';

-- ============================================
-- 函数: 获取会话的完整上下文 (用于AI调用)
-- ============================================
CREATE OR REPLACE FUNCTION get_session_context(p_session_id UUID, p_max_messages INT DEFAULT 50)
    RETURNS TABLE
            (
                session_info JSONB,
                messages     JSONB,
                current_plan JSONB
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT jsonb_build_object(
                       'session_id', s.session_id,
                       'template_name', t.template_name,
                       'status', s.status,
                       'current_question_index', s.current_question_index,
                       'questions_completed', s.questions_completed,
                       'start_time', s.start_time
               )                               as session_info,

               (SELECT jsonb_agg(
                               jsonb_build_object(
                                       'role', m.role,
                                       'content', m.content,
                                       'sequence', m.sequence_number,
                                       'timestamp', m.timestamp,
                                       'related_question', m.related_question_id
                               ) ORDER BY m.sequence_number
                       )
                FROM (SELECT *
                      FROM conversation_messages
                      WHERE session_id = p_session_id
                      ORDER BY sequence_number DESC
                      LIMIT p_max_messages) m) as messages,

               s.interview_plan                as current_plan
        FROM interview_sessions s
                 LEFT JOIN interview_templates t ON s.template_id = t.template_id
        WHERE s.session_id = p_session_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_session_context IS '获取面试会话的完整上下文,用于构建AI提示';

-- ============================================
-- 初始化数据示例 (可选)
-- ============================================
\i /schema/seed_interview_templates_data.sql

-- 表字段修改（历史库可能曾含 metadata 列）
ALTER TABLE conversation_messages
    DROP COLUMN IF EXISTS metadata;
