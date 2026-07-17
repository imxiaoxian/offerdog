# 一键跑通：Docker 起 PostgreSQL / Redis / MinIO / Whisper，再新开两个窗口分别启动本机后端与前端（Vite）。
# 前置：已安装 Docker Desktop；根目录有 .env（可复制 .env.example 并填 SILICONFLOW_API_KEY、DEEPSEEK_API_KEY）。
# 用法：.\run-dev-full.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Get-DotEnvValue {
  param([string]$Key)
  $envFile = Join-Path $PSScriptRoot ".env"
  if (-not (Test-Path $envFile)) { return $null }
  $prefix = "$Key="
  foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
    $t = $line.Trim()
    if ($t.Length -eq 0 -or $t.StartsWith("#")) { continue }
    if (-not $t.StartsWith($prefix)) { continue }
    $v = $t.Substring($prefix.Length).Trim()
    if ($v.StartsWith('"') -and $v.EndsWith('"')) { $v = $v.Substring(1, $v.Length - 2) }
    return $v
  }
  return $null
}

if (-not (Test-Path (Join-Path $PSScriptRoot ".env"))) {
  Write-Host "未找到 .env，请复制 .env.example 为 .env 并填写密钥后重试。" -ForegroundColor Yellow
  Write-Host "  Copy-Item .env.example .env" -ForegroundColor Gray
  exit 1
}

$feDir = Join-Path $PSScriptRoot "OfferDog-frontend"
if (-not (Test-Path (Join-Path $feDir "node_modules"))) {
  Write-Host "首次运行：安装前端依赖 npm install ..." -ForegroundColor Yellow
  Push-Location $feDir
  npm install
  if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
  Pop-Location
}

Write-Host "Starting postgres, redis, minio, whisper (build whisper if needed) ..." -ForegroundColor Cyan
docker compose up -d --build postgres redis minio whisper
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$whPort = Get-DotEnvValue "WHISPER_PORT"
if ([string]::IsNullOrEmpty($whPort)) { $whPort = "5002" }
$whName = Get-DotEnvValue "WHISPER_CONTAINER_NAME"
if ([string]::IsNullOrEmpty($whName)) { $whName = "OfferDog_whisper" }
$healthUrl = "http://127.0.0.1:$whPort/health"

Write-Host "Waiting for Whisper at $healthUrl (首次拉模型可能需数分钟) ..." -ForegroundColor Yellow
$deadline = (Get-Date).AddMinutes(20)
$ok = $false
while ((Get-Date) -lt $deadline) {
  try {
    $r = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
    if ($r.StatusCode -eq 200) { $ok = $true; break }
  } catch {}
  Start-Sleep -Seconds 5
}
if (-not $ok) {
  Write-Host "Whisper 在时限内未就绪。查看日志: docker logs $whName" -ForegroundColor Red
  exit 1
}
Write-Host "Whisper OK." -ForegroundColor Green

$backendCmd = "Set-Location '$PSScriptRoot'; .\run-local.ps1"
$feCmd = "Set-Location '$PSScriptRoot\OfferDog-frontend'; npm run dev"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList "-NoExit", "-Command", $feCmd

Write-Host ""
Write-Host "已打开新窗口：后端 run-local.ps1（默认 http://127.0.0.1:9080 ）与前端 npm run dev（http://localhost:5173 ）" -ForegroundColor Green
Write-Host "Whisper 健康检查: $healthUrl" -ForegroundColor Cyan
