param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][ValidateRange(1, 2147483647)][int]$DisplayId,
    [string]$ApkPath,
    [string]$SdkPath
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $PSScriptRoot '../app/build/outputs/apk/debug/app-debug.apk'
}
if ([string]::IsNullOrWhiteSpace($SdkPath)) {
    $SdkPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
}
$apk = Get-Item -LiteralPath $ApkPath
$adb = Join-Path $SdkPath 'platform-tools/adb.exe'
$aapt = Get-ChildItem -LiteralPath (Join-Path $SdkPath 'build-tools') -Filter aapt2.exe -Recurse |
    Sort-Object FullName -Descending | Select-Object -First 1
if (!(Test-Path -LiteralPath $adb) -or !$aapt) { throw 'Android SDK platform-tools and build-tools are required.' }

$badging = (& $aapt.FullName dump badging $apk.FullName) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Cannot read the APK manifest.' }
$package = [regex]::Match($badging, "(?m)^package: name='([^']+)'").Groups[1].Value
$activity = [regex]::Match($badging, "(?m)^launchable-activity: name='([^']+)'").Groups[1].Value
if ($package -notmatch '^com\.gamebox\.os(\.debug)?$' -or !$activity) {
    throw 'The APK must be a launchable GameBox build.'
}

function Invoke-DeviceAdb([string[]]$AdbArguments) {
    $result = (& $adb -s $Serial @AdbArguments) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $($AdbArguments -join ' ')`n$result" }
    return $result
}

$displays = Invoke-DeviceAdb -AdbArguments @('shell', 'dumpsys', 'display')
if ($displays -notmatch "DisplayViewport\{type=EXTERNAL, valid=true, isActive=true, displayId=$DisplayId,") {
    throw "Display $DisplayId is not an active physical external display. Reconnect projection and check its current display ID."
}

Write-Host "Installing $($apk.Name) as $package on $Serial; TV display $DisplayId"
Write-Host (Invoke-DeviceAdb -AdbArguments @('install', '-r', $apk.FullName))
Invoke-DeviceAdb -AdbArguments @('shell', 'am', 'force-stop', $package) | Out-Null
$component = "$package/$activity"
Write-Host (Invoke-DeviceAdb -AdbArguments @(
    'shell', 'am', 'start', '-W', '--display', "$DisplayId", '--windowingMode', '1', '-n', $component
))
$activities = Invoke-DeviceAdb -AdbArguments @('shell', 'dumpsys', 'activity', 'activities')
$tvActivities = [regex]::Match($activities, "(?ms)^Display #$DisplayId\b.*?(?=^Display #|\z)").Value
if ($tvActivities -notmatch "(?:topResumedActivity|mResumedActivity)=[^\r\n]*\s$([regex]::Escape($package))/") {
    throw "The installed package $package is not resumed on TV display $DisplayId. Do not use this run as acceptance evidence."
}

[pscustomobject]@{
    Package = $package
    Component = $component
    DisplayId = $DisplayId
    ApkSha256 = (Get-FileHash -LiteralPath $apk.FullName -Algorithm SHA256).Hash
    VerifiedTopResumed = $true
}
