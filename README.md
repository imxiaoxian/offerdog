<div align="center">

<img src="https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk" alt="Java 17">
<img src="https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?style=flat-square&logo=springboot" alt="Spring Boot">
<img src="https://img.shields.io/badge/LangGraph-0.1+-blue?style=flat-square" alt="LangGraph">
<img src="https://img.shields.io/badge/DeepSeek-V4-purple?style=flat-square" alt="DeepSeek">
<img src="https://img.shields.io/badge/PostgreSQL-16+pgvector-blue?style=flat-square&logo=postgresql" alt="PostgreSQL">
<img src="https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis" alt="Redis">
<img src="https://img.shields.io/badge/Vue.js-3.x-brightgreen?style=flat-square&logo=vuedotjs" alt="Vue.js">
<img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License">

<h1>🐕 OfferDog</h1>
<h3>AI-Powered Technical Interview Platform</h3>

<p>
  <strong>Simulate. Practice. Succeed.</strong><br>
  Spring Boot · LangGraph Agent · pgvector RAG · Real-time Speech · Multi-modal Assessment
</p>

</div>

---

## 📖 Overview

**OfferDog** is a full-stack AI interview simulation platform that helps job seekers prepare for technical interviews through realistic, multi-turn conversations powered by **DeepSeek V4** and **pgvector-based RAG**. It goes beyond simple Q&A — with real-time speech recognition, structured learning reports, and adaptive practice plans.

> 🎯 **Core Value**: Transform interview preparation from passive reading to active, measurable practice with AI-generated personalized feedback.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Vue.js 3 Frontend                         │
│  Interview Chat · Practice Plans · Learning Reports          │
│  Real-time Audio (WebSocket) · Progress Dashboard            │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP / WebSocket / SSE
┌───────────────────────────▼─────────────────────────────────┐
│                 Spring Boot Application                       │
│                                                               │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │  Auth/User   │  │  Interview   │  │  Knowledge Base  │    │
│  │  Management  │  │  Engine      │  │  (RAG Pipeline)  │    │
│  └─────────────┘  └──────────────┘  └──────────────────┘    │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │  Speech      │  │  Report      │  │  Practice Plan   │    │
│  │  (ASR/TTS)   │  │  Generator   │  │  Generator       │    │
│  └─────────────┘  └──────────────┘  └──────────────────┘    │
└─────────────────────────────────────────────────────────────┘
        │              │              │              │
        ▼              ▼              ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐
│PostgreSQL│  │  Redis   │  │  MinIO   │  │DeepSeek API │
│(pgvector)│  │ (Session)│  │ (Files)  │  │(LLM + Emb)  │
└──────────┘  └──────────┘  └──────────┘  └─────────────┘
```

---

## ✨ Key Features

### 🎙️ Multi-modal Interview Simulation
- **Text + Voice**: Real-time speech recognition (Whisper / Volcengine ASR) + AI voice response (Doubao TTS)
- **60+ Question Banks**: Frontend, Backend, DevOps, QA, Mobile, with company-tagged questions (ByteDance, Alibaba, Tencent...)
- **Adaptive Difficulty**: Dynamic question selection based on performance
- **Resume-aware**: Upload resume PDF for personalized interview sessions

### 🧠 RAG-Enhanced Knowledge Base
- **pgvector HNSW Index**: Vector search across 1,000+ technical knowledge snippets
- **Recursive Semantic Chunking**: Intelligent Markdown splitting with similarity-based merging
- **Real-time Context Injection**: Relevant knowledge injected into interview context for deeper Q&A

### 📊 Intelligent Reporting
- **Structured Learning Reports**: Weakness identification, strength analysis, growth suggestions
- **Practice Plans**: AI-generated day-by-day study roadmap with curated resources
- **Progress Tracking**: Historical session comparison and skill radar charts

### 🎨 Modern Developer Experience
- **Dual Backend**: Local JAR + Docker Compose with unified `.env` config
- **Cloudflare Tunnel**: Instant public access for mobile testing
- **pnpm Monorepo**: Vue.js 3 + TypeScript + Vite with clean modular structure

---

## 🛠️ Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| **Backend Framework** | Spring Boot 3.x + MyBatis-Plus | Mature Java ecosystem, excellent ORM |
| **AI/LLM** | DeepSeek V4 (OpenAI-compatible) | State-of-the-art Chinese + English |
| **Agent** | LangGraph + langchain4j | Structured multi-turn interview orchestration |
| **Vector DB** | PostgreSQL 16 + pgvector (HNSW) | Production-grade, no separate Vector DB needed |
| **Embedding** | BAAI/bge-large-zh-v1.5 (1024-dim) | Top-tier Chinese text embeddings |
| **Cache** | Redis 7 + Caffeine L1 | Multi-level caching for session + frequent data |
| **Storage** | MinIO (S3-compatible) | Self-hosted file storage for resumes/reports |
| **Speech** | Whisper (local) / Volcengine (cloud) | Flexible ASR fallback |
| **Frontend** | Vue.js 3 + TypeScript + Vite + Pinia | Modern reactive SPA |
| **Deploy** | Docker Compose + Cloudflare Tunnel | One-command startup |

---

## 📦 Quick Start

### Prerequisites
- JDK 17+, Maven 3.8+
- Docker & Docker Compose
- Node.js 18+ (for frontend dev)
- DeepSeek API Key

### One-Command Setup

```bash
# 1. Clone
git clone https://github.com/imxiaoxian/offerdog.git
cd offerdog

# 2. Configure
cp .env.example .env
# Edit .env with your DEEPSEEK_API_KEY and optional SiliconFlow key

# 3. Start infrastructure
docker compose up -d postgres redis minio

# 4. Run backend (local JAR)
./run-local.ps1   # Windows PowerShell

# 5. Run frontend
cd OfferDog-frontend
pnpm install
pnpm dev           # → http://localhost:5173
```

Or use **full Docker Compose** mode:
```bash
docker compose up -d    # Starts PostgreSQL + Redis + MinIO + App + Whisper
```

---

## 📊 Performance

| Metric | Value |
|--------|-------|
| Average Interview Response | <3s (with RAG context) |
| Speech Recognition Latency | <500ms (Whisper tiny) |
| Knowledge Chunk Accuracy | >85% recall@5 |
| Concurrent Sessions | 50+ (Docker Compose) |

---

## 🗂️ Project Structure

```
offerdog/
├── src/main/java/com/hanserdev/interview/
│   ├── controller/          # REST API controllers
│   ├── service/             # Business logic layer
│   ├── mapper/              # MyBatis-Plus data access
│   ├── model/               # Domain entities & VO/DTO
│   ├── config/              # Spring configuration
│   └── ai/                  # AI integration (LangGraph, RAG, Speech)
├── src/main/resources/
│   ├── config/              # application-prod.yml, banner.txt
│   ├── knowledge-base/      # Curated technical interview knowledge (Markdown)
│   └── mapperxml/           # MyBatis XML mappings
├── OfferDog-frontend/       # Vue.js 3 frontend (pnpm workspace)
├── postgres-init/           # Database migration scripts
├── crawler/                 # Question bank crawler (NowCoder, Juejin)
├── docs/                    # Technical documentation
├── docker-compose.yml       # Full stack orchestration
└── run-local.ps1            # Local development launcher
```

---

## 🔮 Roadmap

- [ ] LeetCode-style coding problem integration with online judge
- [ ] System design interview simulation with whiteboard
- [ ] Multi-language support (Japanese, Korean)
- [ ] Enterprise SSO (OAuth2/OIDC)
- [ ] Kubernetes Helm chart
- [ ] Mobile app (Flutter)

---

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## 📄 License

MIT License — see [LICENSE](LICENSE) file.

---

<div align="center">
  <sub>Built for developers, by developers 🐕</sub>
</div>
