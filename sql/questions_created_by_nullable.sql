-- 官方题目允许 created_by 为空（与 question_banks.created_by 一致），便于启动时无用户也能写入示例题。
-- 可重复执行（已有库升级时运行一次）。
ALTER TABLE questions
    ALTER COLUMN created_by DROP NOT NULL;
