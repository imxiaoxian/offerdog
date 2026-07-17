# 修复因 PowerShell 管道导入导致的面试模板中文乱码（库内已是问号时需执行）。
# 会删除 interview_plan_templates、interview_templates 后按 UTF-8 重新导入（需容器内存在 /schema 挂载）。
# 若已有 interview_sessions 引用模板，请先自行处理或勿执行。
# Usage: .\repair-interview-templates-encoding.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$container = "OfferDog_postgres"
$user = "postgres"
$db = "interview"

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
      "POSTGRES_CONTAINER_NAME" { $container = $val }
      "POSTGRES_USER" { $user = $val }
      "POSTGRES_DB" { $db = $val }
    }
  }
}

$running = docker ps -q -f "name=$container"
if (-not $running) {
  Write-Error "Container matching name '$container' is not running."
}

Write-Host "Clearing plan + template rows..."
docker exec -e PGCLIENTENCODING=UTF8 $container psql -U $user -d $db -v ON_ERROR_STOP=1 -c @"
DELETE FROM interview_plan_templates;
DELETE FROM interview_templates;
"@

$seed = Join-Path $PSScriptRoot "sql\seed_interview_templates_data.sql"
$plans = Join-Path $PSScriptRoot "sql\init_plan_templates.sql"
if (-not (Test-Path $seed) -or -not (Test-Path $plans)) {
  Write-Error "Missing sql\seed_interview_templates_data.sql or sql\init_plan_templates.sql"
}

foreach ($pair in @(
    @{ Host = $seed; Tmp = "/tmp/_repair_seed_templates.sql" },
    @{ Host = $plans; Tmp = "/tmp/_repair_plan_templates.sql" }
  )) {
  Write-Host "Importing $($pair.Host) ..."
  docker cp $pair.Host "${container}:$($pair.Tmp)"
  docker exec -e PGCLIENTENCODING=UTF8 $container psql -v ON_ERROR_STOP=1 -U $user -d $db -f $pair.Tmp
  if ($LASTEXITCODE -ne 0) { Write-Error "psql failed" }
  docker exec $container rm -f $pair.Tmp | Out-Null
}

Write-Host "Done. Refresh the page; template titles should show Chinese correctly."
