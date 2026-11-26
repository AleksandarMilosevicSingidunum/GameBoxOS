[CmdletBinding()]
param(
    [string]$Checkout = (Join-Path $PSScriptRoot '..\..\tmp\nuvio-upstream'),
    [switch]$Install,
    [string]$DeviceSerial
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
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) {
    throw "Android SDK was not found at $sdk. Set ANDROID_HOME."
}

$installDeviceSerial = $null
if ($Install) {
    $deviceLines = @(& $adb devices -l | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' })
    if ($LASTEXITCODE -ne 0) { throw 'Could not query ADB devices.' }
    if ($DeviceSerial) {
        $installDeviceSerial = $deviceLines |
            Where-Object { ($_ -split '\s+')[0] -eq $DeviceSerial } |
            ForEach-Object { ($_ -split '\s+')[0] } |
            Select-Object -First 1
        if (-not $installDeviceSerial) {
            throw "ADB device '$DeviceSerial' is not connected and authorized. Run 'adb devices -l' to see available serials."
        }
    } else {
        $honorDevices = @($deviceLines | Where-Object { $_ -match 'model:PGT_N19|product:PGT-N19EEA' })
        $directHonorDevices = @($honorDevices | Where-Object { ($_ -split '\s+')[0] -match '^\d{1,3}(?:\.\d{1,3}){3}:\d+$' })
        if ($directHonorDevices.Count -eq 1) {
            $installDeviceSerial = ($directHonorDevices[0] -split '\s+')[0]
        } elseif ($honorDevices.Count -eq 1) {
            $installDeviceSerial = ($honorDevices[0] -split '\s+')[0]
        } elseif ($deviceLines.Count -eq 1) {
            $installDeviceSerial = ($deviceLines[0] -split '\s+')[0]
        } else {
            $available = if ($deviceLines.Count) { ($deviceLines | ForEach-Object { ($_ -split '\s+')[0] }) -join ', ' } else { 'none' }
            throw "Choose a device with -DeviceSerial. Connected authorized ADB devices: $available"
        }
    }
    Write-Host "Installing to ADB device $installDeviceSerial"
}

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$goCommand = if ($env:NUVIO_GO) {
    Get-Command $env:NUVIO_GO -ErrorAction SilentlyContinue
} else {
    Get-Command go -ErrorAction SilentlyContinue
}
if (-not $goCommand) {
    $installedGo = Join-Path $env:ProgramFiles 'Go\bin\go.exe'
    if (Test-Path $installedGo) { $goCommand = Get-Item $installedGo }
}
if (-not $goCommand) {
    throw 'Go is required to build the Nuvio native VPN transport. Install Go 1.21 or newer and add go.exe to PATH, or set NUVIO_GO to its full path.'
}
$goPath = if ($goCommand -is [System.IO.FileInfo]) { $goCommand.FullName } else { $goCommand.Source }
$env:NUVIO_GO = $goPath
$goVersion = (& $env:NUVIO_GO version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $goVersion -notmatch '^go version go1\.(\d+)') {
    throw "Could not determine the installed Go version: $goVersion"
}
if ([int]$Matches[1] -lt 21) {
    throw "Go 1.21 or newer is required for the pinned native build; found $goVersion"
}
if (-not (Test-Path (Join-Path $sdk 'ndk\29.0.14206865'))) {
    throw "Android NDK 29.0.14206865 is required. Install it from Android Studio > SDK Manager > SDK Tools."
}
Push-Location $Checkout
try {
    & .\gradlew.bat :app:assembleFullDebug '-PgameBoxDebugAppId=com.gamebox.os.nuviointegration' '-PlivetvTestBuild=true'
    if ($LASTEXITCODE -ne 0) { throw 'Merged GameBox build failed.' }
    $apk = Join-Path $Checkout 'app\build\outputs\apk\full\debug\app-full-arm64-v8a-debug.apk'
    if (-not (Test-Path $apk)) { throw "Build completed but APK was not found: $apk" }
    Write-Host "Merged APK: $apk"
    if ($Install) {
        & $adb -s $installDeviceSerial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw 'APK installation failed; check that the HONOR is connected to ADB.' }
        Write-Host "Installed APK on ADB device $installDeviceSerial."
    }
} finally {
    Pop-Location
}
