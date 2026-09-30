# run-cloud.ps1 — Start the backend with the cloud database profile.
#
# Prerequisites:
#   1. Copy .env.cloud.example to .env.cloud
#   2. Fill in real DB_URL, DB_USERNAME, DB_PASSWORD
#
# Usage (from the backend/ directory):
#   .\run-cloud.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$EnvFile = Join-Path $PSScriptRoot ".env.cloud"

if (-not (Test-Path $EnvFile)) {
    Write-Host ""
    Write-Host "ERROR: .env.cloud not found." -ForegroundColor Red
    Write-Host ""
    Write-Host "To set up cloud credentials:"
    Write-Host "  1. Copy the template:"
    Write-Host "       copy .env.cloud.example .env.cloud"
    Write-Host "  2. Open .env.cloud and fill in DB_URL, DB_USERNAME, DB_PASSWORD"
    Write-Host "  3. Re-run this script."
    Write-Host ""
    exit 1
}

# Parse key=value pairs from .env.cloud (skip blank lines and comments)
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $parts = $line.Split("=", 2)
    if ($parts.Count -ne 2) { return }
    $key   = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
}

Write-Host "Cloud profile loaded. DB_URL = $env:DB_URL" -ForegroundColor Cyan
Write-Host "Starting Spring Boot with profile: cloud ..." -ForegroundColor Green
Write-Host ""

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
