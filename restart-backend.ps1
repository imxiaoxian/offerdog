# 结束常见后端端口上的监听进程，并结束本仓库遗留的 Spring Boot / Maven 进程，再启动后端（与 run-local.ps1 相同）
# 可选：.\restart-backend.ps1 -Port 9080（与 Vite 默认代理 9080 一致；不传则沿用 .env 的 SERVER_PORT）
param([int]$Port = 0)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$projectMarker = [regex]::Escape($PSScriptRoot)
function Stop-ProjectJavaProcesses {
  try {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
      Where-Object { $_.CommandLine -match 'InAiInterviewApplication' -or ($_.CommandLine -match 'spring-boot:run' -and $_.CommandLine -match $projectMarker) } |
      ForEach-Object {
        Write-Host "Stopping Java PID $($_.ProcessId) (OfferDog / spring-boot:run)" -ForegroundColor Yellow
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
      }
  } catch {
    Write-Host "Java process scan failed: $_" -ForegroundColor DarkYellow
  }
}

function Stop-ListenersOnPort {
  param([int]$PortNumber)
  if ($PortNumber -le 0) {
    throw "Invalid port: $PortNumber"
  }
  try {
    $pids = Get-NetTCPConnection -State Listen -LocalPort $PortNumber -ErrorAction SilentlyContinue |
      Select-Object -ExpandProperty OwningProcess -Unique |
      Where-Object { $_ -and $_ -gt 0 }
    foreach ($p in $pids) {
      try {
        $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.ProcessName } else { "?" }
        Write-Host "Port $PortNumber -> stopping PID $p ($name)" -ForegroundColor Yellow
        Stop-Process -Id $p -Force -ErrorAction Stop
      } catch {
        Write-Host "Port $PortNumber -> could not stop PID $p : $_" -ForegroundColor Red
      }
    }
  } catch {
    Write-Host "Port $PortNumber lookup failed: $_" -ForegroundColor DarkYellow
  }
}

function Stop-ListenersNetstat {
  param([int]$PortNumber)
  if ($PortNumber -le 0) {
    throw "Invalid port: $PortNumber"
  }
  $portPat = ":$PortNumber\s"
  foreach ($line in (netstat -ano)) {
    if ($line -notmatch 'LISTENING') { continue }
    if ($line -notmatch $portPat) { continue }
    if ($line -match 'LISTENING\s+(\d+)\s*$') {
      $procId = [int]$Matches[1]
      if ($procId -le 4) { continue }
      try {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        $name = if ($proc) { $proc.ProcessName } else { "?" }
        Write-Host "netstat Port $PortNumber -> stopping PID $procId ($name)" -ForegroundColor Yellow
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
      } catch {
        Write-Host "netstat Port $PortNumber -> could not stop PID $procId : $_" -ForegroundColor Red
      }
    }
  }
}

Stop-ProjectJavaProcesses

foreach ($listenPort in @(9080, 9081, 8081, 8082)) {
  Stop-ListenersOnPort -PortNumber $listenPort
  Stop-ListenersNetstat -PortNumber $listenPort
}

Start-Sleep -Seconds 2

if ($Port -gt 0) {
  & "$PSScriptRoot\run-local.ps1" -Port $Port
} else {
  & "$PSScriptRoot\run-local.ps1"
}
