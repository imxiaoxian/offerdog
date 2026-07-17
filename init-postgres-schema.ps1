# Apply SQL schema to an existing Postgres container (e.g. after "relation users does not exist").
# Uses docker cp + psql -f inside the container so UTF-8 is not corrupted by PowerShell pipes.
# Requires: postgres container with ./sql mounted at /schema (see docker-compose), or reseed scripts only.
# Usage: .\init-postgres-schema.ps1
# Requires: docker, running postgres container from this compose project.

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

$scripts = @(
  "sql\new_table.sql",
  "sql\new_ai.sql",
  "sql\question_favorites.sql",
  "sql\init_plan_templates.sql",
  "sql\questions_created_by_nullable.sql",
  "sql\seed_question_bank_bundle.sql"
)

$running = docker ps -q -f "name=$container"
if (-not $running) {
  Write-Error "Container matching name '$container' is not running. Start postgres: docker compose up -d postgres"
}

function Invoke-PsqlFile {
  param(
    [Parameter(Mandatory = $true)][string]$RelativePath
  )
  $hostPath = Join-Path $PSScriptRoot $RelativePath
  if (-not (Test-Path $hostPath)) {
    Write-Error "Missing file: $hostPath"
  }
  $baseName = [System.IO.Path]::GetFileName($RelativePath) -replace '[^a-zA-Z0-9._-]', '_'
  $tmp = "/tmp/_init_pg_$baseName"
  Write-Host "Applying $RelativePath (docker cp -> psql -f) ..."
  docker cp $hostPath "${container}:${tmp}"
  if ($LASTEXITCODE -ne 0) { Write-Error "docker cp failed for $RelativePath" }
  docker exec -e PGCLIENTENCODING=UTF8 $container psql -v ON_ERROR_STOP=1 -U $user -d $db -f $tmp
  $code = $LASTEXITCODE
  docker exec $container rm -f $tmp | Out-Null
  if ($code -ne 0) {
    Write-Error "psql failed on $RelativePath (exit $code)"
  }
}

foreach ($rel in $scripts) {
  Invoke-PsqlFile -RelativePath $rel
}

Write-Host "Done. You can retry login."
