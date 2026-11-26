[CmdletBinding()]
param(
    [string]$Checkout = (Join-Path $PSScriptRoot '..\..\tmp\nuvio-upstream'),
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$Checkout = [System.IO.Path]::GetFullPath($Checkout)
$revision = 'b7028012fbc19379974a005ae3ae79ac6787822d'
$upstream = 'https://github.com/Br0ck25/NuvioTVLauncher.git'
$patch = Join-Path $PSScriptRoot 'gamebox-nuvio.patch'

if (-not (Test-Path (Join-Path $Checkout '.git'))) {
    if (Test-Path $Checkout) {
        throw "Checkout path exists but is not a Git repository: $Checkout"
    }
    git clone --filter=blob:none --branch dev $upstream $Checkout
    if ($LASTEXITCODE -ne 0) { throw 'Could not clone the pinned upstream source.' }
    git -C $Checkout checkout --detach $revision
    if ($LASTEXITCODE -ne 0) { throw 'Could not select the pinned upstream revision.' }
}

$head = (git -C $Checkout rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $head -ne $revision) {
    throw "Expected upstream revision $revision; found $head. Use a fresh checkout or pass -Checkout."
}

# Make reruns safe: recognize an already applied patch, otherwise validate and apply it.
git -C $Checkout apply --reverse --check $patch 2>$null
if ($LASTEXITCODE -ne 0) {
    git -C $Checkout apply --check $patch
    if ($LASTEXITCODE -ne 0) { throw 'The integration patch does not apply cleanly to this checkout.' }
    git -C $Checkout apply $patch
    if ($LASTEXITCODE -ne 0) { throw 'Could not apply the GameBox integration patch.' }
}

$jdk = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.10002-hotspot'
if (-not (Test-Path (Join-Path $jdk 'bin\java.exe'))) {
    $jdk = $env:JAVA_HOME
}
if (-not $jdk -or -not (Test-Path (Join-Path $jdk 'bin\java.exe'))) {
    throw 'Install a Java 17 JDK and set JAVA_HOME before building.'
}
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not (Test-Path (Join-Path $sdk 'platform-tools\adb.exe'))) {
    throw "Android SDK was not found at $sdk. Set ANDROID_HOME."
}

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
Push-Location $Checkout
try {
    & .\gradlew.bat :app:assembleFullDebug '-PlivetvDebugAppId=com.gamebox.os.nuviointegration' '-PlivetvTestBuild=true'
    if ($LASTEXITCODE -ne 0) { throw 'Merged GameBox build failed.' }
    $apk = Join-Path $Checkout 'app\build\outputs\apk\full\debug\app-full-arm64-v8a-debug.apk'
    if (-not (Test-Path $apk)) { throw "Build completed but APK was not found: $apk" }
    Write-Host "Merged APK: $apk"
    if ($Install) {
        & (Join-Path $sdk 'platform-tools\adb.exe') install -r $apk
        if ($LASTEXITCODE -ne 0) { throw 'APK installation failed; check that the HONOR is connected to ADB.' }
    }
} finally {
    Pop-Location
}
