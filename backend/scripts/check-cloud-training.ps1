# check-cloud-training.ps1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = $PSScriptRoot
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir ".."))
$envCloudPath = Join-Path $backendRoot ".env.cloud"

$settings = @{}
foreach ($line in Get-Content $envCloudPath) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not $trimmed.StartsWith("#") -and $trimmed.Contains("=")) {
        $parts = $trimmed.Split("=", 2)
        $settings[$parts[0].Trim()] = $parts[1].Trim().Trim('"').Trim("'")
    }
}

$dbUrl = $settings["DB_URL"]
$neonUser = $settings["DB_USERNAME"]
$neonPassword = $settings["DB_PASSWORD"]

$rawUri = $dbUrl -replace '^jdbc:', ''
$uri = [System.Uri]$rawUri
$neonHost = $uri.Host
$neonDb = $uri.AbsolutePath.TrimStart('/')

$sql = "SELECT 'courses: ' || count(*) FROM courses UNION ALL SELECT 'subjects: ' || count(*) FROM subjects UNION ALL SELECT 'training_plans: ' || count(*) FROM horse_training_plans UNION ALL SELECT 'training_lots: ' || count(*) FROM training_lots UNION ALL SELECT 'workouts: ' || count(*) FROM training_workouts;"

$queryArgs = @(
    "run", "--rm",
    "-e", "PGPASSWORD=$neonPassword",
    "postgres:18",
    "psql",
    "-h", $neonHost,
    "-U", $neonUser,
    "-d", $neonDb,
    "-t", "-c", $sql
)

& docker @queryArgs
