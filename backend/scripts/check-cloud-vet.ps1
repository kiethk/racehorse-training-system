# check-cloud-vet.ps1
# Kiem tra du lieu bo sung nghiep vu Vet tren Neon Cloud (khong ghi gi).
# Su dung: .\scripts\check-cloud-vet.ps1

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = $PSScriptRoot
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir '..'))
$envCloudPath = Join-Path $backendRoot '.env.cloud'

$settings = @{}
foreach ($line in Get-Content $envCloudPath) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not $trimmed.StartsWith('#') -and $trimmed.Contains('=')) {
        $parts = $trimmed.Split('=', 2)
        $settings[$parts[0].Trim()] = $parts[1].Trim().Trim('"').Trim("'")
    }
}

$dbUrl = $settings['DB_URL']
$neonUser = $settings['DB_USERNAME']
$neonPassword = $settings['DB_PASSWORD']

if (-not $dbUrl -or -not $neonUser -or -not $neonPassword) {
    Write-Host "[ERROR] Thieu DB_URL, DB_USERNAME hoac DB_PASSWORD trong .env.cloud" -ForegroundColor Red
    exit 1
}

$rawUri = $dbUrl -replace '^jdbc:', ''
$uri = [System.Uri]$rawUri
$neonHost = $uri.Host
$neonDb = $uri.AbsolutePath.TrimStart('/')

$sql = @"
SELECT 'care_schedule: ' || count(*) FROM care_schedule WHERE description LIKE '[VET-DEMO]%'
UNION ALL SELECT 'health_records: ' || count(*) FROM health_records WHERE notes LIKE '[VET-DEMO]%'
UNION ALL SELECT 'horse_health_metrics: ' || count(*) FROM horse_health_metrics WHERE notes LIKE '[VET-DEMO]%'
UNION ALL SELECT 'treatment_plans: ' || count(*) FROM treatment_plans WHERE instructions LIKE '[VET-DEMO]%'
UNION ALL SELECT 'prescriptions: ' || count(*) FROM prescriptions WHERE instructions LIKE '[VET-DEMO]%'
UNION ALL SELECT 'injury_records: ' || count(*) FROM injury_records WHERE description LIKE '[VET-DEMO]%'
UNION ALL SELECT 'horses_BLOCKED: ' || count(*) FROM horses WHERE training_decision = 'BLOCKED';
"@

$env:PGPASSWORD = $neonPassword
$env:PGSSLMODE = 'require'
try {
    $queryArgs = @(
        'run', '--rm',
        '-e', 'PGPASSWORD', '-e', 'PGSSLMODE=require',
        'postgres:18',
        'psql', '-h', $neonHost, '-U', $neonUser, '-d', $neonDb,
        '-t', '-c', $sql
    )
    & docker @queryArgs
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PGSSLMODE -ErrorAction SilentlyContinue
}
