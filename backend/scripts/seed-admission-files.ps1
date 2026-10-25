# seed-admission-files.ps1
# Chep file ho so nhap hoc (anh + PDF) tu bo ho so da duyet vao thu muc upload cua backend tren o dia.
# (Copies the admission documents (photos + PDFs) from the reviewed pack into the backend upload folder on disk.)
#
# - Tu tim thu muc RTMS_Admission_Packs_* nam canh thu muc repo (moi nhat theo ten), hoac dung -Source.
# - Tinh dau van tay (SHA-256) cua toan bo file nguon. Neu trung voi lan chep truoc va du file dich -> bo qua.
# - Neu khac (nguon doi, thieu file dich) hoac co -Force -> XOA SACH thu muc upload roi chep lai, nen dung luong
#   thu muc luon bang dung bo nguon, khong bao gio nhan doi.
# - Ten file dich = local:<uuid>.<duoi>, uuid = md5('rtms-seed/' + duong dan trong bo ho so); seed database dung
#   dung cong thuc nay (pg_temp.file_key) nen hai script khop nhau.
#
# Cach dung (Usage), dung o thu muc backend:
#   .\scripts\seed-admission-files.ps1
#   .\scripts\seed-admission-files.ps1 -Source 'D:\path\RTMS_Admission_Packs_2026-10-17'
#   .\scripts\seed-admission-files.ps1 -Force
param(
    [string]$Source,
    [string]$UploadDir,
    [switch]$Force
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $backendRoot '..'))

# 1. Thu muc nguon (Source folder)
if (-not $Source) { $Source = $env:RTMS_ADMISSION_PACKS }
if (-not $Source) {
    $candidate = Get-ChildItem -LiteralPath (Join-Path $repoRoot '..') -Directory -Filter 'RTMS_Admission_Packs_*' |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'upload-manifest.csv') } |
        Sort-Object Name | Select-Object -Last 1
    if (-not $candidate) {
        throw "Admission pack not found. Put the RTMS_Admission_Packs_<date> folder next to the repository folder, or pass -Source."
    }
    $Source = $candidate.FullName
}
$Source = (Resolve-Path -LiteralPath $Source).Path
$manifestPath = Join-Path $Source 'upload-manifest.csv'
if (-not (Test-Path -LiteralPath $manifestPath)) { throw "upload-manifest.csv is missing in $Source" }

# 2. Thu muc dich: giong rtms.storage.upload-dir cua backend (./uploads/admissions khi chay tu thu muc backend)
# (Target folder: the same as the backend's rtms.storage.upload-dir.)
if (-not $UploadDir) { $UploadDir = $env:RTMS_STORAGE_UPLOAD_DIR }
if (-not $UploadDir) { $UploadDir = $env:RTMS_ADMISSION_UPLOAD_DIR }
if (-not $UploadDir) { $UploadDir = Join-Path $backendRoot 'uploads\admissions' }
if (-not [System.IO.Path]::IsPathRooted($UploadDir)) { $UploadDir = Join-Path $backendRoot $UploadDir }
New-Item -ItemType Directory -Path $UploadDir -Force | Out-Null
$UploadDir = (Resolve-Path -LiteralPath $UploadDir).Path
$statePath = Join-Path $UploadDir '.rtms-seed-state.json'

Write-Host "Source : $Source" -ForegroundColor Cyan
Write-Host "Target : $UploadDir" -ForegroundColor Cyan

# 3. Danh sach file + dau van tay tu noi dung that (Files and a fingerprint of their real content)
$md5 = [System.Security.Cryptography.MD5]::Create()
$rows = Import-Csv -LiteralPath $manifestPath -Encoding UTF8
$files = @()
$mismatch = 0
$fingerprintLines = New-Object System.Collections.Generic.List[string]
foreach ($row in $rows) {
    $relative = $row.path
    $sourceFile = Join-Path $Source ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $sourceFile)) { throw "Missing source file: $sourceFile" }
    $hash = (Get-FileHash -LiteralPath $sourceFile -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($row.sha256 -and $hash -ne $row.sha256.ToLowerInvariant()) { $mismatch++ }
    $bytes = $md5.ComputeHash([System.Text.Encoding]::UTF8.GetBytes('rtms-seed/' + $relative))
    $hex = -join ($bytes | ForEach-Object { $_.ToString('x2') })
    $uuid = '{0}-{1}-{2}-{3}-{4}' -f $hex.Substring(0, 8), $hex.Substring(8, 4), $hex.Substring(12, 4), $hex.Substring(16, 4), $hex.Substring(20, 12)
    $name = $uuid + [System.IO.Path]::GetExtension($relative).ToLowerInvariant()
    $size = (Get-Item -LiteralPath $sourceFile).Length
    $files += [pscustomobject]@{ Source = $sourceFile; Name = $name; Size = $size }
    $fingerprintLines.Add("$relative|$hash|$size")
}
$joined = [System.Text.Encoding]::UTF8.GetBytes(($fingerprintLines -join "`n"))
$fingerprint = -join ([System.Security.Cryptography.SHA256]::Create().ComputeHash($joined) | ForEach-Object { $_.ToString('x2') })
if ($mismatch -gt 0) {
    Write-Host "Note: $mismatch file(s) differ from the SHA-256 listed in upload-manifest.csv (the pack was edited)." -ForegroundColor Yellow
}

# 4. Da chep dung bo nay roi thi bo qua (Skip when this exact pack is already in place)
$upToDate = $false
if ((Test-Path -LiteralPath $statePath) -and -not $Force) {
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    if ($state.fingerprint -eq $fingerprint) {
        $missing = @($files | Where-Object {
            $target = Join-Path $UploadDir $_.Name
            -not (Test-Path -LiteralPath $target) -or (Get-Item -LiteralPath $target).Length -ne $_.Size
        })
        $upToDate = $missing.Count -eq 0
        if (-not $upToDate) { Write-Host "$($missing.Count) file(s) are missing or changed in the target folder." -ForegroundColor Yellow }
    } else {
        Write-Host 'The admission pack changed since the last copy.' -ForegroundColor Yellow
    }
}
if ($upToDate) {
    Write-Host "Admission files are up to date ($($files.Count) files). Nothing copied. Use -Force to copy again." -ForegroundColor Green
    exit 0
}

# 5. Xoa sach roi chep lai (Wipe, then copy again)
$old = @(Get-ChildItem -LiteralPath $UploadDir -Force -File)
if ($old.Count -gt 0) {
    Write-Host "Removing $($old.Count) old file(s) from the upload folder..." -ForegroundColor Cyan
    $old | Remove-Item -Force
}
Get-ChildItem -LiteralPath $UploadDir -Force -Directory | Remove-Item -Recurse -Force
$i = 0
foreach ($file in $files) {
    Copy-Item -LiteralPath $file.Source -Destination (Join-Path $UploadDir $file.Name)
    $i++
    if ($i % 50 -eq 0) { Write-Host "  copied $i / $($files.Count)" }
}
[pscustomobject]@{
    fingerprint = $fingerprint
    source      = $Source
    copiedAt    = (Get-Date).ToString('s')
    fileCount   = $files.Count
} | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8

$totalMb = [math]::Round((($files | Measure-Object -Property Size -Sum).Sum) / 1MB, 1)
Write-Host "Copied $($files.Count) admission files ($totalMb MB) into $UploadDir" -ForegroundColor Green
Write-Host 'Run .\scripts\seed-database.ps1 next if you have not seeded the database yet.' -ForegroundColor Green
