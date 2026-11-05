param(
    [string]$DbHost = 'localhost',
    [int]$Port = 5432,
    [string]$Database = 'rtms_db',
    [string]$User = 'postgres',
    [string]$Password = '1',
    [string]$PsqlPath
)

$ErrorActionPreference = 'Stop'

if (-not $PsqlPath) {
    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd) { $PsqlPath = $cmd.Source }
}
if (-not $PsqlPath) {
    $found = Get-ChildItem -Path 'C:\Program Files\PostgreSQL\*\bin\psql.exe' -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Directory.Parent.Name -replace '\D', '') } | Select-Object -Last 1
    if ($found) { $PsqlPath = $found.FullName }
}
if (-not $PsqlPath) { throw 'psql.exe not found. Install PostgreSQL client tools or pass -PsqlPath.' }

$env:PGPASSWORD = $Password
$env:PGCLIENTENCODING = 'UTF8'
$connection = @('-h', $DbHost, '-p', "$Port", '-U', $User, '-d', $Database, '-X', '-v', 'ON_ERROR_STOP=1')

$sqlFile = Join-Path $PSScriptRoot 'seed\sql\55_eda_demo.sql'

Write-Host "Seeding dedicated EDA demo scenario into '$Database'..." -ForegroundColor Cyan
& $PsqlPath @connection -f $sqlFile

Write-Host "EDA demo scenarios seeded successfully!" -ForegroundColor Green
