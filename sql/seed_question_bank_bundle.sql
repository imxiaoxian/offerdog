-- 题库与岗位分类一次性导入（各子脚本均为幂等，可重复执行）。
-- 依赖：categories / question_banks / questions 等表已存在（见 new_table.sql、questions_created_by_nullable）。
-- 调用方式：
--   - Docker 首次建卷：由 postgres-init/06_seed_question_bank_bundle.sql 引入；
--   - 已有库：在项目根执行 .\init-postgres-schema.ps1（已包含本文件），或容器内 psql -f /schema/seed_question_bank_bundle.sql

\echo '>>> seed_categories_offerdog'
\i /schema/seed_categories_offerdog.sql
\echo '>>> seed_offerdog_question_bank_and_questions'
\i /schema/seed_offerdog_question_bank_and_questions.sql
\echo '>>> seed_interview_roles_java_web_algo'
\i /schema/seed_interview_roles_java_web_algo.sql
\echo '>>> seed_questions_extra_batch'
\i /schema/seed_questions_extra_batch.sql
\echo '>>> seed_questions_extra_batch2'
\i /schema/seed_questions_extra_batch2.sql
\echo '>>> seed_questions_per_job_category'
\i /schema/seed_questions_per_job_category.sql
\echo '>>> seed_java_collection_arraylist'
\i /schema/seed_java_collection_arraylist.sql
\echo '>>> ensure_java_bank_questions（按名称补全 Java 题库，避免仅有空壳）'
\i /schema/ensure_java_bank_questions.sql
\echo '>>> insert_weakpoint_java_collection_questions（报告薄弱点「集合/ArrayList/LinkedList」高命中题）'
\i /schema/insert_weakpoint_java_collection_questions.sql

\echo '>>> seed_question_bank_bundle done'
