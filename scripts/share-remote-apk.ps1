$ErrorActionPreference = 'Stop'

$apkPath = (Resolve-Path (Join-Path $PSScriptRoot '..\companion-remote\build\outputs\apk\debug\companion-remote-debug.apk')).Path
$apkLength = (Get-Item -LiteralPath $apkPath).Length
$adb = 'C:\Users\calak\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$adbOutput = & $adb devices -l 2>&1
if ($LASTEXITCODE -ne 0) { throw "ADB is unavailable: $($adbOutput -join ' ')" }
$devices = @($adbOutput | ForEach-Object {
    if ($_ -match '^(.+?)\s+device\s+product:PGT-N19\S*') { $Matches[1] }
})
if ($devices.Count -ne 1) { throw "Expected one connected HONOR PGT-N19; found $($devices.Count)." }
$serial = $devices[0]
$ipOutput = & $adb -s $serial shell ip -4 addr show wlan0
if ($LASTEXITCODE -ne 0 -or ($ipOutput -join "`n") -notmatch 'inet ([0-9.]+)/') {
    throw 'Could not read the HONOR Wi-Fi IPv4 address.'
}
$ip = $Matches[1]
$octets = $ip.Split('.') | ForEach-Object { [int]$_ }
if ($octets.Count -ne 4 -or -not (
    $octets[0] -eq 10 -or
    ($octets[0] -eq 192 -and $octets[1] -eq 168) -or
    ($octets[0] -eq 172 -and $octets[1] -ge 16 -and $octets[1] -le 31)
)) { throw "HONOR address is not private IPv4: $ip" }

$id = [guid]::NewGuid().ToString('N')
$remoteApk = "/data/local/tmp/gamebox-remote-$id.apk"
$remoteHandler = "/data/local/tmp/gamebox-remote-$id.sh"
$localHandler = Join-Path $env:TEMP "gamebox-remote-$id.sh"
$handler = @"
#!/system/bin/sh
IFS=' ' read -r method path protocol
if [ "`$method" != GET ] || [ "`$path" != /gamebox-remote.apk ]; then
    printf 'HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n'
    exit 0
fi
printf 'HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\nContent-Length: $apkLength\r\nConnection: close\r\n\r\n'
cat '$remoteApk'
"@
[System.IO.File]::WriteAllText(
    $localHandler,
    ($handler -replace "`r`n", "`n"),
    [System.Text.Encoding]::ASCII
)

try {
    & $adb -s $serial push $apkPath $remoteApk | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy the APK to temporary HONOR storage.' }
    & $adb -s $serial push $localHandler $remoteHandler | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy the temporary HTTP handler to HONOR.' }
    Write-Host "Download GameBox Remote on the same Wi-Fi: http://${ip}:8765/gamebox-remote.apk"
    Write-Host 'The server is bound to the HONOR Wi-Fi address. Press Ctrl+C to stop and delete its temporary files.'
    & $adb -s $serial shell toybox nc -L -s $ip -p 8765 sh $remoteHandler
} finally {
    & $adb -s $serial shell rm -f $remoteApk $remoteHandler 2>$null | Out-Null
    Remove-Item -LiteralPath $localHandler -Force -ErrorAction SilentlyContinue
}
