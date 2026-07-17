# Localtunnel for OfferDog frontend (no Docker). Fixes "page cannot be used" when HMR points at localhost.
#
# Steps:
# 1) Start backend (e.g. restart-backend.ps1 -Port 8081) and: cd OfferDog-frontend && npm run dev
# 2) Run: npx -y localtunnel --port 5173   -> copy host only, e.g. shiny-foo.loca.lt
# 3) In OfferDog-frontend/.env.development set: VITE_DEV_TUNNEL_HOST=shiny-foo.loca.lt
# 4) Restart npm run dev, then open https://shiny-foo.loca.lt in browser
# 5) If loca.lt shows "Tunnel Password", enter your public IPv4 (open https://api.ipify.org in browser on this PC)
#
param([int]$Port = 5173)

$ErrorActionPreference = "Stop"
Write-Host "Starting localtunnel -> localhost:$Port ..." -ForegroundColor Cyan
Write-Host "After you see 'your url is', set VITE_DEV_TUNNEL_HOST=<host> in OfferDog-frontend/.env.development and restart npm run dev." -ForegroundColor Yellow
Set-Location (Join-Path $PSScriptRoot 'OfferDog-frontend')
& npx -y localtunnel --port $Port
