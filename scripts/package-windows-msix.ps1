[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $PublishDirectory,
    [Parameter(Mandatory = $true)]
    [string] $OutputPath,
    [string] $Version = '0.1.0.0',
    [string] $Publisher = 'CN=GameBoxOS'
)

$ErrorActionPreference = 'Stop'
$publish = (Resolve-Path -LiteralPath $PublishDirectory).Path
$output = [IO.Path]::GetFullPath($OutputPath)
if (-not (Test-Path -LiteralPath (Join-Path $publish 'GameBox.Windows.exe') -PathType Leaf)) {
    throw 'The self-contained publish directory must contain GameBox.Windows.exe.'
}
if ($Version -notmatch '^\d+\.\d+\.\d+\.\d+$') { throw 'MSIX version must contain four numeric components.' }
if ([string]::IsNullOrWhiteSpace($Publisher)) { throw 'MSIX publisher is required.' }

$makeAppx = Get-ChildItem -LiteralPath (Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin') -Filter makeappx.exe -Recurse |
    Where-Object { $_.FullName -match '\\x64\\makeappx\.exe$' } |
    Sort-Object FullName -Descending |
    Select-Object -First 1
if ($null -eq $makeAppx) { throw 'Windows SDK makeappx.exe was not found.' }

$stage = Join-Path ([IO.Path]::GetTempPath()) ('gamebox-msix-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage -Force | Out-Null
try {
    Get-ChildItem -LiteralPath $publish -Force | Copy-Item -Destination $stage -Recurse -Force
    $assets = Join-Path $stage 'Assets'
    New-Item -ItemType Directory -Path $assets -Force | Out-Null

    Add-Type -AssemblyName System.Drawing
    foreach ($asset in @('StoreLogo.png', 'Square150x150Logo.png', 'Square44x44Logo.png')) {
        $size = if ($asset -eq 'Square44x44Logo.png') { 44 } elseif ($asset -eq 'Square150x150Logo.png') { 150 } else { 50 }
        $bitmap = [Drawing.Bitmap]::new($size, $size)
        try {
            $graphics = [Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.Clear([Drawing.Color]::FromArgb(11, 16, 24))
                $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
                $brush = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(85, 214, 190))
                try {
                    $points = [Drawing.Point[]] @(
                        [Drawing.Point]::new([int]($size * .50), [int]($size * .10)),
                        [Drawing.Point]::new([int]($size * .88), [int]($size * .30)),
                        [Drawing.Point]::new([int]($size * .88), [int]($size * .70)),
                        [Drawing.Point]::new([int]($size * .50), [int]($size * .90)),
                        [Drawing.Point]::new([int]($size * .12), [int]($size * .70)),
                        [Drawing.Point]::new([int]($size * .12), [int]($size * .30))
                    )
                    $graphics.FillPolygon($brush, $points)
                } finally { $brush.Dispose() }
            } finally { $graphics.Dispose() }
            $bitmap.Save((Join-Path $assets $asset), [Drawing.Imaging.ImageFormat]::Png)
        } finally { $bitmap.Dispose() }
    }

    $manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot '..\windows\packaging\AppxManifest.xml') -Raw
    $manifest = $manifest.Replace('@VERSION@', $Version).Replace('@PUBLISHER@', $Publisher)
    Set-Content -LiteralPath (Join-Path $stage 'AppxManifest.xml') -Value $manifest -Encoding UTF8

    New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($output)) -Force | Out-Null
    & $makeAppx.FullName pack /d $stage /p $output /o
    if ($LASTEXITCODE -ne 0) { throw "makeappx.exe failed with exit code $LASTEXITCODE." }
} finally {
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
