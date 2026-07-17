# CLAUDE.md — OfferDog AI Interview Platform

## Quick Start
```bash
cd offerDog-main
cp .env.example .env  # Add DEEPSEEK_API_KEY + SILICONFLOW_API_KEY
docker compose up -d  # PostgreSQL + Redis + MinIO + Whisper
.\run-local.ps1        # Windows
```

## Architecture
- **Backend**: Spring Boot 3.x + MyBatis-Plus (Java 21)
- **Frontend**: Vue.js 3 + TypeScript + Vite (`OfferDog-frontend/`)
- **AI**: DeepSeek V4 via langchain4j, pgvector RAG with recursive-semantic chunking
- **Speech**: Whisper (local ASR) + Doubao (cloud TTS)
- **Storage**: MinIO (S3-compatible) for resumes/reports
- **Cache**: Redis 7 + Caffeine L1

## Key Files
- `pom.xml` — Maven build (Java 21, Spring Boot 3.5.7)
- `docker-compose.yml` — 5 services (postgres, redis, minio, whisper, app)
- `src/main/resources/config/application-prod.yml` — All config via env vars
- `OfferDog-frontend/` — Vue.js frontend (pnpm)
- `postgres-init/` — Database migration SQL
- `src/main/java/com/hanserdev/interview/` — Application source

## CI
- `.github/workflows/ci.yml` — Maven compile (JDK 21 only, pom.xml requires 21)

## Notes
- Tests require PostgreSQL/Redis — CI skips tests (compile only)
- `.env` is gitignored, `.env.example` is committed
- Password defaults in `application-prod.yml` were replaced with placeholders
