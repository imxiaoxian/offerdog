-- ========================================
-- 优化后的PostgreSQL数据库设计
-- 充分利用JSONB、数组、枚举等特性
-- ========================================

-- 枚举类型定义
CREATE TYPE user_role AS ENUM ('user', 'admin', 'vip');
CREATE TYPE gender_type AS ENUM ('male', 'female', 'other');
CREATE TYPE question_difficulty AS ENUM ('easy', 'medium', 'hard');
CREATE TYPE question_source AS ENUM ('official', 'user');

-- ========================================
-- 1. 用户与简历模块（合并）
-- ========================================
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'user',

    -- 基本信息（使用JSONB存储灵活字段）
    profile       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    -- profile结构示例: {
    --   "email": "user@example.com",
    --   "phone": "13800138000",
    --   "real_name": "张三",
    --   "avatar_url": "https://...",
    --   "gender": "male"
    -- }

    -- 简历信息（使用JSONB存储，避免单独建表）
    resume        JSONB,
    -- resume结构示例: {
    --   "name": "张三",
    --   "birth": "1995-01-01",
    --   "gender": "male",
    --   "location": {"province": "广东省", "city": "深圳市"},
    --   "education": {
    --     "level": "本科",
    --     "school": "清华大学",
    --     "major": "计算机科学",
    --     "graduation_date": "2017-06"
    --   },
    --   "contact": {
    --     "phone": "13800138000",
    --     "email": "zhangsan@example.com",
    --     "homepage": "https://github.com/zhangsan"
    --   },
    --   "work": {
    --     "first_employment": "2017-07",
    --     "experience": [...],
    --     "projects": [...]
    --   },
    --   "skills": ["Java", "Python", "PostgreSQL"],
    --   "job_intention": "后端开发工程师",
    --   "self_evaluation": "...",
    --   "resume_file_url": "https://..."
    -- }

    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ -- 软删除使用时间戳，NULL表示未删除
);

-- 索引优化
CREATE INDEX idx_users_username ON users (username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users (role) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_profile ON users USING GIN (profile); -- JSONB索引
CREATE INDEX idx_users_resume ON users USING GIN (resume); -- JSONB索引

COMMENT ON TABLE users IS '用户表（包含基本信息和简历）';
COMMENT ON COLUMN users.profile IS '用户基本信息（JSONB格式）';
COMMENT ON COLUMN users.resume IS '简历信息（JSONB格式，可为空）';
COMMENT ON COLUMN users.deleted_at IS '删除时间，NULL表示未删除';

-- ========================================
-- 2. 行业与岗位模块（合并为层级结构）
-- ========================================
CREATE TABLE categories
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    parent_id  INT REFERENCES categories (id),
    level      SMALLINT     NOT NULL,                     -- 1=行业, 2=岗位
    path       INTEGER[]    NOT NULL,                     -- 使用数组存储路径，便于查询层级
    metadata   JSONB                 DEFAULT '{}'::jsonb, -- 额外信息
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT chk_level CHECK (level IN (1, 2))          -- 限制层级，1表示行业，2表示岗位
);

-- 索引优化
CREATE UNIQUE INDEX idx_categories_name_parent ON categories (name, COALESCE(parent_id, 0)) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_parent ON categories (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_path ON categories USING GIN (path); -- 数组索引，快速查询层级

COMMENT ON TABLE categories IS '行业和岗位分类表（树形结构）';
COMMENT ON COLUMN categories.level IS '层级：1-行业，2-岗位';
COMMENT ON COLUMN categories.path IS '从根到当前节点的ID路径数组';

-- ========================================
-- 3. 题库
-- ========================================
CREATE TABLE question_banks
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    category_id INT          NOT NULL REFERENCES categories (id), -- 关联岗位
    created_by  BIGINT REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_question_banks_category ON question_banks (category_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE question_banks IS '题库表';

-- ========================================
-- 4. 题目表（核心简化）
-- ========================================
CREATE TABLE questions
(
    id          BIGSERIAL PRIMARY KEY,
    bank_id     BIGINT REFERENCES question_banks (id),
    category_id INT                 NOT NULL REFERENCES categories (id), -- 关联岗位

    content     TEXT                NOT NULL,
    answer      TEXT,
    tips        TEXT,

    difficulty  question_difficulty NOT NULL DEFAULT 'medium',
    source      question_source     NOT NULL DEFAULT 'official',
    tags        TEXT[]                       DEFAULT '{}',               -- 使用数组直接存储标签

    -- 统计信息
    stats       JSONB               NOT NULL DEFAULT '{
      "views": 0,
      "likes": 0,
      "favorites": 0
    }'::jsonb,

    remark      TEXT,                                                    -- 用户备注

    created_by  BIGINT              REFERENCES users (id),               -- 官方题可为 NULL；用户题须指向 users.id
    created_at  TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- 索引优化
CREATE INDEX idx_questions_bank ON questions (bank_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_questions_category ON questions (category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_questions_difficulty ON questions (difficulty) WHERE deleted_at IS NULL;
CREATE INDEX idx_questions_source ON questions (source) WHERE deleted_at IS NULL;
CREATE INDEX idx_questions_tags ON questions USING GIN (tags); -- 数组索引，支持标签查询
CREATE INDEX idx_questions_stats ON questions USING GIN (stats); -- JSONB索引
CREATE INDEX idx_questions_created_by ON questions (created_by) WHERE deleted_at IS NULL;

COMMENT ON TABLE questions IS '题目表';
COMMENT ON COLUMN questions.tags IS '标签数组，直接存储标签名称';
COMMENT ON COLUMN questions.stats IS '统计信息（浏览、点赞、收藏等）';

-- ========================================
-- 5. 评论表（简化）
-- ========================================
CREATE TABLE comments
(
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT      NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    parent_id   BIGINT REFERENCES comments (id), -- 支持回复

    is_pinned   BOOLEAN     NOT NULL DEFAULT false,
    like_count  INT         NOT NULL DEFAULT 0,

    created_by  BIGINT      NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

-- 索引优化
CREATE INDEX idx_comments_question ON comments (question_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_parent ON comments (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_created_by ON comments (created_by) WHERE deleted_at IS NULL;

COMMENT ON TABLE comments IS '评论表';

-- ========================================
-- 6. 用户行为表（新增，用于记录学习进度）
-- ========================================
CREATE TABLE user_question_progress
(
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    question_id    BIGINT      NOT NULL REFERENCES questions (id) ON DELETE CASCADE,

    status         SMALLINT    NOT NULL DEFAULT 0, -- 0-未做, 1-已做, 2-已掌握
    is_favorite    BOOLEAN     NOT NULL DEFAULT false,
    notes          TEXT,                           -- 用户笔记

    last_viewed_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (user_id, question_id)
);

CREATE INDEX idx_progress_user ON user_question_progress (user_id, status);
CREATE INDEX idx_progress_favorite ON user_question_progress (user_id) WHERE is_favorite = true;

COMMENT ON TABLE user_question_progress IS '用户题目学习进度';

-- ========================================
-- 7. 自动更新时间戳函数
-- ========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为所有表添加自动更新触发器
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE
    ON categories
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_question_banks_updated_at
    BEFORE UPDATE
    ON question_banks
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_questions_updated_at
    BEFORE UPDATE
    ON questions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE
    ON comments
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_question_progress_updated_at
    BEFORE UPDATE
    ON user_question_progress
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 添加 questions表中 vector_at 列，如果值不为空，表示该题目已向量化
alter table public.questions
    add vector_at timestamp with time zone;

-- ========================================
-- 8. 面经表
-- ========================================
CREATE TABLE IF NOT EXISTS interview_experience (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    position VARCHAR(255),
    experience_type VARCHAR(50) DEFAULT 'full_time',
    content TEXT NOT NULL,
    formatted_content TEXT,
    source_url VARCHAR(500),
    source VARCHAR(100),
    author VARCHAR(100),
    views INT DEFAULT 0,
    likes INT DEFAULT 0,
    created_by BIGINT DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_experience_company_name ON interview_experience(company_name);
CREATE INDEX idx_experience_position ON interview_experience(position);
CREATE INDEX idx_experience_source ON interview_experience(source);
CREATE INDEX idx_experience_created_at ON interview_experience(created_at DESC);

CREATE TRIGGER update_interview_experience_updated_at
    BEFORE UPDATE
    ON interview_experience
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE interview_experience IS '面经表';