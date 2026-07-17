# Local Spring Boot (outside Docker). Example deps: docker compose up -d postgres redis minio whisper
# Optional: copy .env.example to .env — this script loads SPRING_* / REDIS_* / POSTGRES_PASSWORD from .env when set.
# Windows：8004-8103 常为 Hyper-V 保留，本机监听会失败；默认 9080，或 .\run-local.ps1 -Port <端口>
# 仅将 RAG/ 目录文件向量化入库后退出：.\run-local.ps1 -ImportRagOnly（需 Postgres、Redis、SILICONFLOW_API_KEY 可用）

param(
  [int]$Port = 0,
  [switch]$ImportRagOnly
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# 避免终端里残留的 SERVER_PORT（如上次调试）覆盖 .env
Remove-Item Env:SERVER_PORT -ErrorAction SilentlyContinue

$script:PostgresPortForJdbc = $null
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
  Get-Content -LiteralPath $envFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#")) { return }
    $eq = $line.IndexOf("=")
    if ($eq -lt 1) { return }
    $key = $line.Substring(0, $eq).Trim()
    $val = $line.Substring($eq + 1).Trim()
    if ($val.StartsWith('"') -and $val.EndsWith('"')) { $val = $val.Substring(1, $val.Length - 2) }
    switch ($key) {
      "SPRING_DATASOURCE_URL" {
        if ([string]::IsNullOrEmpty($env:SPRING_DATASOURCE_URL)) { $env:SPRING_DATASOURCE_URL = $val }
      }
      "SPRING_DATASOURCE_USERNAME" {
        if ([string]::IsNullOrEmpty($env:SPRING_DATASOURCE_USERNAME)) { $env:SPRING_DATASOURCE_USERNAME = $val }
      }
      "SPRING_DATASOURCE_PASSWORD" {
        if ([string]::IsNullOrEmpty($env:SPRING_DATASOURCE_PASSWORD)) { $env:SPRING_DATASOURCE_PASSWORD = $val }
      }
      "POSTGRES_PASSWORD" {
        if ([string]::IsNullOrEmpty($env:SPRING_DATASOURCE_PASSWORD)) {
          $env:SPRING_DATASOURCE_PASSWORD = $val
        }
      }
      "POSTGRES_PORT" { $script:PostgresPortForJdbc = $val }
      "REDIS_HOST" { $env:REDIS_HOST = $val }
      "REDIS_PORT" { $env:REDIS_PORT = $val }
      "REDIS_PASSWORD" { $env:REDIS_PASSWORD = $val }
      "SERVER_PORT" { $env:SERVER_PORT = $val }
      "WHISPER_PORT" { $env:WHISPER_ENDPOINT = "http://127.0.0.1:$val" }
      "WHISPER_ENDPOINT" { $env:WHISPER_ENDPOINT = $val }
      "ASR_PROVIDER" { $env:ASR_PROVIDER = $val }
      "ASR_WHISPER_DEFAULT_PCM_SAMPLE_RATE" { $env:ASR_WHISPER_DEFAULT_PCM_SAMPLE_RATE = $val }
      "ASR_WHISPER_LANGUAGE" { $env:ASR_WHISPER_LANGUAGE = $val }
      "CORS_ALLOWED_ORIGIN_PATTERNS" { $env:CORS_ALLOWED_ORIGIN_PATTERNS = $val }
      "DEEPSEEK_API_KEY" { $env:DEEPSEEK_API_KEY = $val }
      "DEEPSEEK_BASE_URL" { $env:DEEPSEEK_BASE_URL = $val }
      "DEEPSEEK_CHAT_MODEL" { $env:DEEPSEEK_CHAT_MODEL = $val }
      "DEEPSEEK_READ_TIMEOUT_MS" { $env:DEEPSEEK_READ_TIMEOUT_MS = $val }
      "DEEPSEEK_CONNECT_TIMEOUT_MS" { $env:DEEPSEEK_CONNECT_TIMEOUT_MS = $val }
      "SILICONFLOW_API_KEY" {
        $env:SILICONFLOW_API_KEY = $val
        # 与 docker-compose / application-prod 中 SPRING_AI_OPENAI_* 对齐，嵌入客户端必读到密钥
        $env:SPRING_AI_OPENAI_API_KEY = $val
      }
      "SPRING_AI_OPENAI_API_KEY" {
        $env:SPRING_AI_OPENAI_API_KEY = $val
        if ([string]::IsNullOrEmpty($env:SILICONFLOW_API_KEY)) { $env:SILICONFLOW_API_KEY = $val }
      }
      "SILICONFLOW_BASE_URL" {
        $env:SILICONFLOW_BASE_URL = $val
        $env:SPRING_AI_OPENAI_BASE_URL = $val
      }
      "SPRING_AI_OPENAI_BASE_URL" {
        $env:SPRING_AI_OPENAI_BASE_URL = $val
        if ([string]::IsNullOrEmpty($env:SILICONFLOW_BASE_URL)) { $env:SILICONFLOW_BASE_URL = $val }
      }
      "SILICONFLOW_EMBEDDING_MODEL" {
        $env:SILICONFLOW_EMBEDDING_MODEL = $val
        $env:SPRING_AI_OPENAI_EMBEDDING_OPTIONS_MODEL = $val
      }
      "SILICONFLOW_EMBEDDING_ENCODING_FORMAT" {
        $env:SILICONFLOW_EMBEDDING_ENCODING_FORMAT = $val
        $env:SPRING_AI_OPENAI_EMBEDDING_OPTIONS_ENCODING_FORMAT = $val
      }
    }
  }
}

if ($Port -gt 0) {
  $env:SERVER_PORT = "$Port"
}

$env:SPRING_PROFILES_ACTIVE = "prod"
if ([string]::IsNullOrEmpty($env:SERVER_PORT)) {
  $env:SERVER_PORT = "9080"
}

if ([string]::IsNullOrEmpty($env:SPRING_DATASOURCE_URL)) {
  $pgPort = if ($script:PostgresPortForJdbc) { $script:PostgresPortForJdbc } else { "15432" }
  $url = "jdbc:postgresql://127.0.0.1:${pgPort}/interview?serverTimezone=Asia/Shanghai&reWriteBatchedInserts=true"
  $env:SPRING_DATASOURCE_URL = $url
}
if ([string]::IsNullOrEmpty($env:SPRING_DATASOURCE_USERNAME)) {
  $env:SPRING_DATASOURCE_USERNAME = "postgres"
}
if ([string]::IsNullOrEmpty($env:SPRING_DATASOURCE_PASSWORD)) {
  $env:SPRING_DATASOURCE_PASSWORD = "OfferDog2026"
}

if ([string]::IsNullOrEmpty($env:REDIS_HOST)) {
  $env:REDIS_HOST = "127.0.0.1"
}
if ([string]::IsNullOrEmpty($env:REDIS_PORT)) {
  $env:REDIS_PORT = "16378"
}
if ([string]::IsNullOrEmpty($env:REDIS_PASSWORD)) {
  $env:REDIS_PASSWORD = "OfferDog2026"
}

if ([string]::IsNullOrEmpty($env:MINIO_ENDPOINT)) {
  $env:MINIO_ENDPOINT = "http://127.0.0.1:9000"
}
if ([string]::IsNullOrEmpty($env:MINIO_URL_PREFIX)) {
  $env:MINIO_URL_PREFIX = "http://127.0.0.1:9000"
}
$env:SPRING_SESSION_NAMESPACE = "inai:session"

if ([string]::IsNullOrEmpty($env:WHISPER_ENDPOINT)) {
  $env:WHISPER_ENDPOINT = "http://127.0.0.1:5002"
}

# host.docker.internal in .env is for containers; replace for host-run JVM
$env:SPRING_DATASOURCE_URL = $env:SPRING_DATASOURCE_URL -replace "host\.docker\.internal", "127.0.0.1"
$env:REDIS_HOST = $env:REDIS_HOST -replace "host\.docker\.internal", "127.0.0.1"

Write-Host "SERVER_PORT=$($env:SERVER_PORT)  Redis=$($env:REDIS_HOST):$($env:REDIS_PORT)  JDBC=$($env:SPRING_DATASOURCE_USERNAME)" -ForegroundColor Cyan

if ($ImportRagOnly) {
  $env:SPRING_PROFILES_ACTIVE = "prod,rag-import-cli"
  Write-Host "RAG 向量化入库（profile=rag-import-cli，无 HTTP 端口）..." -ForegroundColor Cyan
  mvn -q spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.main.web-application-type=none"
  exit $LASTEXITCODE
}

mvn spring-boot:run
