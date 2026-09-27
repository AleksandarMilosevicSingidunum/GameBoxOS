[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $PackagePath,
    [Parameter(Mandatory = $true)] [string] $ChecksumPath,
    [Parameter(Mandatory = $true)] [string] $ManifestPath,
    [switch] $RequireSigned
)

$ErrorActionPreference = 'Stop'
$package = Get-Item -LiteralPath $PackagePath
if ($package.Length -le 0) { throw 'MSIX package is empty.' }
$actualHash = (Get-FileHash -LiteralPath $package.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
if ((Get-Content -LiteralPath $ChecksumPath -Raw).Trim().ToLowerInvariant() -ne $actualHash) {
    throw 'MSIX checksum does not match the package.'
}
$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
if ($manifest.artifact -ne $package.Name -or $manifest.sha256 -ne $actualHash -or
    [int64]$manifest.sizeBytes -ne $package.Length -or $manifest.packageFormat -ne 'MSIX') {
    throw 'MSIX provenance manifest does not match the package.'
}
if ($RequireSigned -and $manifest.authenticodeSigned -ne $true) {
    throw 'MSIX provenance does not declare a signed package.'
}
Write-Output "Validated MSIX package $($package.Name) ($($package.Length) bytes)."
