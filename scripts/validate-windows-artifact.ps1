param(
    [Parameter(Mandatory = $true)][string]$ArchivePath,
    [Parameter(Mandatory = $true)][string]$ChecksumPath,
    [Parameter(Mandatory = $true)][string]$ManifestPath,
    [switch]$RequireAuthenticode,
    [string]$ExecutablePath,
    [string]$ExecutableName = 'GameBox.Windows.exe'
)

$archive = Get-Item -LiteralPath $ArchivePath -ErrorAction Stop
$expectedHash = (Get-Content -Raw -LiteralPath $ChecksumPath -ErrorAction Stop).Trim().ToLowerInvariant()
$actualHash = (Get-FileHash -LiteralPath $archive.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
if ($expectedHash -notmatch '^[0-9a-f]{64}$') { throw 'Checksum file must contain exactly one SHA-256 value.' }
if ($actualHash -ne $expectedHash) { throw 'Windows archive checksum does not match.' }

$manifest = Get-Content -Raw -LiteralPath $ManifestPath -ErrorAction Stop | ConvertFrom-Json
if ($manifest.schemaVersion -ne 1) { throw 'Unsupported Windows artifact manifest schema.' }
if ($manifest.artifact -ne $archive.Name) { throw 'Manifest artifact name does not match the archive.' }
if ($manifest.sha256 -ne $actualHash) { throw 'Manifest SHA-256 does not match the archive.' }
if ([int64]$manifest.sizeBytes -ne $archive.Length) { throw 'Manifest size does not match the archive.' }
if ($manifest.runtime -ne 'win-x64') { throw 'Manifest runtime must be win-x64.' }
if ($manifest.selfContained -ne $true) { throw 'Manifest must identify a self-contained build.' }
if ($manifest.authenticodeSigned -isnot [bool]) { throw 'Manifest must declare authenticodeSigned as a boolean.' }
if ([string]::IsNullOrWhiteSpace($ExecutableName) -or $ExecutableName -match '[\\/]') {
    throw 'ExecutableName must be a single file name.'
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($archive.FullName)
try {
    $entries = @($zip.Entries | Where-Object { $_.FullName -ieq $ExecutableName })
    if ($entries.Count -ne 1) { throw "Archive must contain exactly one $ExecutableName entry." }
    if ($entries[0].Length -le 0) { throw "Archive executable $ExecutableName must not be empty." }
} finally {
    $zip.Dispose()
}

if (-not [string]::IsNullOrWhiteSpace($ExecutablePath)) {
    $executable = Get-Item -LiteralPath $ExecutablePath -ErrorAction Stop
    if ($executable.Name -ne $ExecutableName) { throw 'ExecutablePath file name does not match ExecutableName.' }
    if ($executable.Length -le 0) { throw 'Published executable must not be empty.' }
}
if ($RequireAuthenticode) {
    if ($manifest.authenticodeSigned -ne $true) { throw 'Manifest must identify an Authenticode-signed build.' }
    if ([string]::IsNullOrWhiteSpace($ExecutablePath)) { throw 'ExecutablePath is required when Authenticode is required.' }
    $signature = Get-AuthenticodeSignature -LiteralPath $ExecutablePath -ErrorAction Stop
    if ($signature.Status -ne 'Valid') { throw "Executable Authenticode signature is not valid: $($signature.Status)" }
}
if ($manifest.sourceCommit -notmatch '^[0-9a-f]{40}$') { throw 'Manifest source commit must be a full Git SHA.' }

Write-Host "Validated Windows artifact manifest for $($archive.Name)"

