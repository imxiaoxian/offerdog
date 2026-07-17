# Cloudflare Quick Tunnel -> https://*.trycloudflare.com
# Full app: TUNNEL_TARGET=Vite 5173. Run: cd OfferDog-frontend && npm run dev
# Usage: .\tunnel-cloudflared.ps1 [-Detached] [-LogsOnly] [-Preset App|Api] [-Target <url>]

param(
  [ValidateSet('', 'App', 'Api')]
  [string]$Preset = '',
  [string]$Target = "",
  [switch]$Detached,
  [switch]$LogsOnly
)

$ErrorActionPreference = "Stop"
$script:ProjectRoot = $PSScriptRoot
$script:ComposeFile = Join-Path $ProjectRoot 'docker-compose.yml'

function Invoke-ProjectCompose {
  param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
  if (-not (Test-Path -LiteralPath $script:ComposeFile)) {
    throw "docker-compose.yml not found: $script:ComposeFile"
  }
  & docker compose -f $script:ComposeFile @Args
}

function Show-TryCloudflareUrls {
  $logOutput = Invoke-ProjectCompose logs cloudflared 2>&1 | Out-String
  if ($logOutput -match 'no such service' -or $logOutput -match 'No resource found') {
    Write-Host "[tunnel] No cloudflared service logs yet. Start with: .\tunnel-cloudflared.ps1 -Detached" -ForegroundColor Yellow
    Write-Host "         docker compose -f `"$script:ComposeFile`" --profile tunnel up -d cloudflared" -ForegroundColor DarkGray
    return
  }
  $urlMatches = [regex]::Matches($logOutput, 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com')
  if ($urlMatches.Count -eq 0) {
    if ($logOutput.Trim().Length -lt 30) {
      Write-Host "[tunnel] Log empty. Run -Detached, wait a few seconds, then -LogsOnly." -ForegroundColor Yellow
      return
    }
    Write-Host "[tunnel] No trycloudflare URL in log yet. Full log:" -ForegroundColor Yellow
    Write-Host $logOutput
    return
  }
  Write-Host "[tunnel] Public URL (from log):" -ForegroundColor Green
  foreach ($u in ($urlMatches | ForEach-Object { $_.Value } | Select-Object -Unique)) {
    Write-Host "  $u"
  }
}

if ($LogsOnly) {
  Show-TryCloudflareUrls
  exit 0
}

if ($Preset -eq 'App') {
  $Target = 'http://host.docker.internal:5173'
}
elseif ($Preset -eq 'Api') {
  $script:TunnelApiPort = '9080'
  $envFile = Join-Path $script:ProjectRoot '.env'
  if (Test-Path $envFile) {
    Get-Content -LiteralPath $envFile -Encoding UTF8 | ForEach-Object {
      $line = $_.Trim()
      if ($line -match '^\s*SERVER_PORT\s*=\s*(\d+)\s*$') { $script:TunnelApiPort = $Matches[1] }
    }
  }
  $Target = "http://host.docker.internal:$($script:TunnelApiPort)"
}

if (-not $Target) {
  $Target = $env:TUNNEL_TARGET
}
if (-not $Target) {
  $Target = 'http://host.docker.internal:5173'
}

$env:TUNNEL_TARGET = $Target
Set-Location $script:ProjectRoot
Write-Host "TUNNEL_TARGET=$Target" -ForegroundColor Cyan
Write-Host "compose: $script:ComposeFile" -ForegroundColor DarkGray
if ($Target -match '5173') {
  Write-Host "[tunnel] Full-site mode: need Vite on 5173 (npm run dev in OfferDog-frontend)." -ForegroundColor DarkCyan
}

if ($Detached) {
  Invoke-ProjectCompose --profile tunnel up -d cloudflared
  Start-Sleep -Seconds 6
  Show-TryCloudflareUrls
  Write-Host "Follow logs: docker compose -f `"$script:ComposeFile`" logs -f cloudflared" -ForegroundColor DarkGray
  exit 0
}

Write-Host "Running foreground. Find https://....trycloudflare.com below, or use -LogsOnly in another window." -ForegroundColor DarkGray
Invoke-ProjectCompose --profile tunnel up cloudflared
