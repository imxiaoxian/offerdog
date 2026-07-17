## OfferDog（Spring Boot + Vue3 + Whisper）

后端：Spring Boot  
前端：Vue 3 + Vite  
依赖：Postgres / Redis / MinIO（Docker）  
语音识别：Whisper（支持“伪实时字幕”）

### 快速开始（推荐）

在仓库根目录执行（需要已准备好 `.env`）：

```powershell
docker compose up -d --build
```

如果你希望“一键拉起基础设施 + 本机后端 + 前端”：

```powershell
.\run-dev-full.ps1
```

### 1) 环境要求

- Docker Desktop（含 compose v2）
- JDK 21
- Maven 3.9+（本机启动后端需要）
- Node.js（见 `OfferDog-frontend/package.json#engines`）

### 2) 配置 `.env`

从模板复制：

```powershell
Copy-Item .env.example .env
notepad .env
```

关键项（不填会影响 AI/向量能力）：

- `SILICONFLOW_API_KEY`
- `DEEPSEEK_API_KEY`

### 3) 启动方式

#### A. 全部用 Docker（最省事）

```powershell
docker compose up -d --build
```

- 后端（宿主机）：`http://127.0.0.1:${APP_PORT}`（默认 9081）
- Whisper（宿主机）：`http://127.0.0.1:${WHISPER_PORT}/health`（默认 5002）

#### B. Docker 起依赖 + 本机跑后端

```powershell
docker compose up -d postgres redis minio whisper
.\run-local.ps1
```

后端默认：`http://127.0.0.1:9080`

#### C. 启动前端

```powershell
cd OfferDog-frontend
npm install
npm run dev
```

Vite 代理（见 `OfferDog-frontend/vite.config.ts`）：
- `/api` → 后端 HTTP（默认 `http://127.0.0.1:9080`）
- `/ws` → 后端 WebSocket（例如 `/ws/asr`）

### 4) 数据库初始化/修复

- Postgres **首次新建数据卷**时，会自动执行 `postgres-init/` 下脚本（通过 `\i /schema/*.sql` 引用 `sql/`）。其中 `06_seed_question_bank_bundle.sql` 会依次导入岗位分类、各「岗位题库」及多批官方面试题（见 `sql/seed_question_bank_bundle.sql`，幂等可重复执行）。末尾会执行 `sql/ensure_java_bank_questions.sql`：按名称 **`Java 题库`** 写入题目，避免仅有空壳、历史种子 JOIN 未落题的情况。
- 若界面里 **Java 题库** 仍无题，在 Postgres 容器内执行：`psql -U postgres -d interview -v ON_ERROR_STOP=1 -f /schema/ensure_java_bank_questions.sql`（需已挂载 `./sql` → `/schema`）。
- 如果数据卷已存在，修改 `sql/` 不会自动重跑，可手动执行（含题库种子）：

```powershell
.\init-postgres-schema.ps1
```

彻底重置（会清空数据）：

```powershell
docker compose down -v
docker compose up -d postgres
```

### 5) Whisper 语音识别说明

#### 5.1 端口与访问

- 容器内服务互访用：`http://whisper:5000`
- 宿主机访问用：`http://127.0.0.1:${WHISPER_PORT}`（默认 5002）

#### 5.2 Whisper “伪实时字幕”

Whisper 原生是“整段音频→一次性转写”。项目里做了折中方案：
- 录音过程中周期性触发转写，推送 `isFinal=false` 的增量片段
- 停止录音后推送 `isFinal=true` 的最终结果

建议设置（提升兼容性）：
- `ASR_WHISPER_DEFAULT_PCM_SAMPLE_RATE=48000`
- `ASR_WHISPER_LANGUAGE=auto`（或 `zh`）

### 6) 常见问题

- 改了 `.env` 但容器没生效：重新 `docker compose up -d`（必要时 `down` 后再 `up`）。
- 端口被占用：换端口或关闭占用进程（Vite 会自动尝试 5174/5175…）。

