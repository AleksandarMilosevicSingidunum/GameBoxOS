param(
    [Parameter(Mandatory = $true)][string]$ArchivePath,
    [Parameter(Mandatory = $true)][string]$ChecksumPath,
    [Parameter(Mandatory = $true)][string]$ManifestPath
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
if ($manifest.sourceCommit -notmatch '^[0-9a-f]{40}$') { throw 'Manifest source commit must be a full Git SHA.' }

Write-Host "Validated Windows artifact manifest for $($archive.Name)"
