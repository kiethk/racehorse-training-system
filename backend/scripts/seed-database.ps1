# seed-database.ps1
# Xoa sach du lieu nghiep vu va seed lai toan bo du lieu demo cho moi actor vao PostgreSQL local.
# (Wipes all business data and seeds the full demo data set for every actor into the local PostgreSQL.)
#
# - Ket noi lay tu backend/src/main/resources/application-local.properties (giong backend), hoac tu tham so.
# - Bat buoc: backend da chay it nhat mot lan de Flyway tao/cap nhat bang (script tu kiem tra).
# - Chay toan bo trong MOT transaction: loi o dau thi DB giu nguyen nhu truoc khi chay.
# - Cuoi cung kiem tra hon 50 luat nghiep vu; vi pham thi dung va khong ghi gi.
#
# Cach dung (Usage), dung o thu muc backend:
#   .\scripts\seed-database.ps1
#   .\scripts\seed-database.ps1 -Yes                       # khong hoi xac nhan (no confirmation)
#   .\scripts\seed-database.ps1 -Port 5432 -User postgres -Password 123 -Database rtms_db
#   .\scripts\seed-database.ps1 -AsOf '2026-11-02 08:00'   # CHI DE THU: seed nhu the dang la luc do (testing only)
param(
    [string]$DbHost,
    [int]$Port,
    [string]$Database,
    [string]$User,
    [string]$Password,
    [string]$PsqlPath,
    [switch]$Yes,
    [switch]$AllowRemote,
    [string]$AsOf
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$seedSql = Join-Path $PSScriptRoot 'seed\sql\seed.sql'
$propsPath = Join-Path $backendRoot 'src\main\resources\application-local.properties'

# 1. Thong tin ket noi (Connection settings)
$props = @{}
if (Test-Path -LiteralPath $propsPath) {
    foreach ($line in Get-Content -LiteralPath $propsPath) {
        if ($line -match '^\s*([^#=\s][^=]*?)\s*=\s*(.*?)\s*$') { $props[$Matches[1]] = $Matches[2] }
    }
}
if ($props.ContainsKey('spring.datasource.url') -and $props['spring.datasource.url'] -match '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') {
    if (-not $DbHost) { $DbHost = $Matches[1] }
    if (-not $Port -and $Matches[2]) { $Port = [int]$Matches[2] }
    if (-not $Database) { $Database = $Matches[3] }
}
if (-not $User -and $props.ContainsKey('spring.datasource.username')) { $User = $props['spring.datasource.username'] }
if (-not $Password -and $props.ContainsKey('spring.datasource.password')) { $Password = $props['spring.datasource.password'] }
if (-not $DbHost) { $DbHost = 'localhost' }
if (-not $Port) { $Port = 5432 }
if (-not $Database) { $Database = 'rtms_db' }
if (-not $User) { $User = 'postgres' }
if ("$DbHost$User$Password$Database" -match '\$\{') {
    throw 'application-local.properties uses ${...} placeholders; pass -DbHost -Port -Database -User -Password instead.'
}
if (-not $AllowRemote -and $DbHost -notin @('localhost', '127.0.0.1', '::1')) {
    throw "Refusing to wipe a database on '$DbHost'. This script is for the local database only (use -AllowRemote if you really mean it)."
}

# 2. Tim psql (Find psql)
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
$env:PGOPTIONS = $null
if ($AsOf) {
    $AsOf = ([datetime]::Parse($AsOf)).ToString("yyyy-MM-ddTHH:mm")
    $env:PGOPTIONS = "-c rtms.seed_now=$AsOf"
    Write-Host "Testing only: seeding as if it were $AsOf (club time)." -ForegroundColor Yellow
}
$connection = @('-h', $DbHost, '-p', "$Port", '-U', $User, '-d', $Database, '-X', '-v', 'ON_ERROR_STOP=1')

# psql ghi NOTICE ra stderr; PowerShell 5.1 coi do la loi khi ErrorActionPreference = Stop, nen tam tat trong luc goi.
# (psql writes NOTICEs to stderr, which PowerShell 5.1 treats as errors under Stop, so relax it around the call.)
function Invoke-Psql([string[]]$arguments) {
    $ErrorActionPreference = 'Continue'
    $out = & $PsqlPath @connection @arguments 2>&1 | ForEach-Object { "$_" }
    return [pscustomobject]@{ Output = $out; ExitCode = $LASTEXITCODE }
}

function Invoke-Scalar([string]$sql) {
    $result = Invoke-Psql @('-tA', '-c', $sql)
    if ($result.ExitCode -ne 0) { throw "Cannot query $Database on ${DbHost}:${Port} as ${User}: $($result.Output)" }
    return ($result.Output | Out-String).Trim()
}

Write-Host "Database : $Database on ${DbHost}:$Port (user $User)" -ForegroundColor Cyan
Write-Host "psql     : $PsqlPath" -ForegroundColor Cyan

# 3. Flyway phai chay xong migration moi nhat (Flyway must have applied the newest migration)
$migrationDir = Join-Path $backendRoot 'src\main\resources\db\migration'
$fileVersions = Get-ChildItem -LiteralPath $migrationDir -Filter 'V*__*.sql' |
    ForEach-Object { ($_.Name -replace '^V([0-9_.]+)__.*$', '$1') -replace '_', '.' }
$hasHistory = Invoke-Scalar "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL"
if ($hasHistory -ne 't') {
    throw 'The database has no tables yet. Start the backend once (mvnw spring-boot:run) so Flyway creates them, then run this script again.'
}
$dbVersions = (Invoke-Scalar "SELECT string_agg(version, ',') FROM flyway_schema_history WHERE success") -split ','
$notApplied = @($fileVersions | Where-Object { $_ -notin $dbVersions })
if ($notApplied.Count -gt 0) {
    throw "Migrations not applied yet: $($notApplied -join ', '). Start the backend once so Flyway applies them, then run this script again."
}

# 4. Xac nhan (Confirm)
if (-not $Yes) {
    Write-Host ''
    Write-Host "This DELETES all data in '$Database' (accounts, horses, admissions, training, vet, groom...) and seeds the demo data." -ForegroundColor Yellow
    $answer = Read-Host 'Type YES to continue'
    if ($answer -ne 'YES') { Write-Host 'Cancelled.'; exit 1 }
}

# 5. Seed trong mot transaction (Seed in one transaction)
Write-Host 'Seeding...' -ForegroundColor Cyan
$started = Get-Date
$result = Invoke-Psql @('-q', '-1', '-f', $seedSql)
$exit = $result.ExitCode
$result.Output | ForEach-Object {
    $text = $_
    if ($text -match 'ERROR|FATAL') { Write-Host $text -ForegroundColor Red }
    elseif ($text -match 'NOTICE:\s*(.*)$') { Write-Host ('  ' + $Matches[1]) }
    elseif ($text.Trim()) { Write-Host $text }
}
if ($exit -ne 0) { throw 'Seed failed; the database was left unchanged. See the error above.' }
$seconds = [math]::Round(((Get-Date) - $started).TotalSeconds, 1)

# 6. Tom tat (Summary)
Write-Host ''
Write-Host "Seed finished in $seconds s." -ForegroundColor Green
& $PsqlPath @connection -q -P pager=off -c @"
SELECT 'Accounts' AS data, count(*)::TEXT AS rows FROM users
UNION ALL SELECT 'Horses (eligible / candidate / rejected)', format('%s / %s / %s', count(*) FILTER (WHERE current_status = 'ELIGIBLE'),
       count(*) FILTER (WHERE current_status = 'CANDIDATE'), count(*) FILTER (WHERE current_status = 'REJECTED')) FROM horses
UNION ALL SELECT 'Admissions', count(*)::TEXT FROM admission_applications
UNION ALL SELECT 'Admission documents', count(*)::TEXT FROM admission_documents
UNION ALL SELECT 'Care schedules', count(*)::TEXT FROM care_schedule
UNION ALL SELECT 'Health records', count(*)::TEXT FROM health_records
UNION ALL SELECT 'Training plans', count(*)::TEXT FROM horse_training_plans
UNION ALL SELECT 'Training workouts', count(*)::TEXT FROM training_workouts
UNION ALL SELECT 'Groom daily tasks', count(*)::TEXT FROM groom_daily_tasks
UNION ALL SELECT 'Incident reports', count(*)::TEXT FROM horse_incident_reports
UNION ALL SELECT 'Race nominations', count(*)::TEXT FROM race_registrations
UNION ALL SELECT 'Notifications', count(*)::TEXT FROM notifications;
"@
Write-Host 'Main accounts (password admin123): admin@rtms.com, owner@rtms.com, trainer1@rtms.com, vet1@rtms.com, groom1@rtms.com' -ForegroundColor Green
