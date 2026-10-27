param(
    [ValidateSet('Setup', 'Wrapper', 'Doctor', 'Build', 'Check', 'Emulator', 'Run')]
    [string]$Action = 'Doctor',
    [string]$DeviceSerial
)

$ErrorActionPreference = 'Stop'
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
}
$avdName = 'FoodTracker_Phone'
$image = 'system-images;android-36;default;x86_64'

# Gradle 9.1 supports the existing JDK 25 as well as JDK 17/21.
if (-not $env:JAVA_HOME) {
    $java = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($java) {
        $env:JAVA_HOME = Split-Path (Split-Path $java.Source -Parent) -Parent
    } elseif (Test-Path 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe') {
        $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
    } else {
        throw 'Install a JDK (17, 21 or 25) or Android Studio, then retry.'
    }
}
$env:PATH = "$env:JAVA_HOME\bin;$sdk\platform-tools;$env:PATH"
$sdkManager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
$avdManager = Join-Path $sdk 'cmdline-tools\latest\bin\avdmanager.bat'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'

function Assert-Success([string]$step) {
    if ($LASTEXITCODE -ne 0) { throw "$step failed (exit $LASTEXITCODE)." }
}

function Invoke-Build([string[]]$Tasks) {
    if (-not (Test-Path (Join-Path $PSScriptRoot 'gradlew.bat'))) {
        throw 'Run .\android.ps1 Setup to install the build tools and Gradle wrapper.'
    }
    Push-Location $PSScriptRoot
    try {
        & .\gradlew.bat @Tasks
        Assert-Success 'Gradle build'
    } finally { Pop-Location }
}

if ($Action -eq 'Setup') {
    if (-not (Test-Path $sdkManager)) {
        $stage = Join-Path (Join-Path $env:LOCALAPPDATA 'Temp') ('foodtracker-sdk-' + [guid]::NewGuid())
        New-Item -ItemType Directory -Path $stage | Out-Null
        $archive = Join-Path $stage 'command-tools.zip'
        & (Join-Path $PSScriptRoot 'Download-Archive.ps1') -Url 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip' -Length 155655386 -Sha256 '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a' -Destination $archive
        if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a') {
            throw 'Android command-line tools checksum mismatch.'
        }
        Expand-Archive -LiteralPath $archive -DestinationPath $stage
        New-Item -ItemType Directory -Force -Path (Join-Path $sdk 'cmdline-tools') | Out-Null
        if (Test-Path (Join-Path $sdk 'cmdline-tools\latest')) {
            throw 'An incomplete command-line tools installation exists; inspect it before retrying.'
        }
        Move-Item -LiteralPath (Join-Path $stage 'cmdline-tools') -Destination (Join-Path $sdk 'cmdline-tools\latest')
    }
    # SDK license agreements are displayed by sdkmanager during setup.
    1..100 | ForEach-Object { 'y' } | & $sdkManager "--sdk_root=$sdk" --licenses
    Assert-Success 'SDK licenses'
    & $sdkManager "--sdk_root=$sdk" 'platform-tools' 'platforms;android-36' 'build-tools;36.0.0' 'emulator' $image
    Assert-Success 'SDK installation'
    $existing = & $emulator -list-avds
    if ($existing -notcontains $avdName) {
        'no' | & $avdManager create avd --name $avdName --package $image --device 'pixel_7'
        Assert-Success 'Virtual phone creation'
    }
}

if ($Action -in @('Setup', 'Wrapper')) {
    if (-not (Test-Path (Join-Path $PSScriptRoot 'gradlew.bat'))) {
        $stage = Join-Path (Join-Path $env:LOCALAPPDATA 'Temp') ('foodtracker-gradle-' + [guid]::NewGuid())
        New-Item -ItemType Directory -Path $stage | Out-Null
        $archive = Join-Path $stage 'gradle.zip'
        & curl.exe -fL --retry 2 'https://services.gradle.org/distributions/gradle-9.1.0-bin.zip' -o $archive
        Assert-Success 'Gradle download'
        $checksum = (Invoke-RestMethod 'https://services.gradle.org/distributions/gradle-9.1.0-bin.zip.sha256').Trim()
        if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $checksum) {
            throw 'Gradle distribution checksum mismatch.'
        }
        Expand-Archive -LiteralPath $archive -DestinationPath $stage
        # Generate the official wrapper independently of the app configuration.
        $bootstrap = Join-Path $stage 'bootstrap'
        New-Item -ItemType Directory -Path $bootstrap | Out-Null
        & (Join-Path $stage 'gradle-9.1.0\bin\gradle.bat') -p $bootstrap init --type basic --dsl groovy --project-name foodtracker-wrapper --use-defaults
        Assert-Success 'Gradle bootstrap project'
        & (Join-Path $stage 'gradle-9.1.0\bin\gradle.bat') -p $bootstrap wrapper --gradle-version 9.1.0 --distribution-type bin --gradle-distribution-sha256-sum $checksum
        Assert-Success 'Gradle wrapper generation'
        Copy-Item -LiteralPath (Join-Path $bootstrap 'gradlew'), (Join-Path $bootstrap 'gradlew.bat') -Destination $PSScriptRoot
        Copy-Item -LiteralPath (Join-Path $bootstrap 'gradle') -Destination $PSScriptRoot -Recurse
    }
    Write-Host 'Setup complete. Run .\android.ps1 Run to build and launch the app.'
    exit
}

if ($Action -eq 'Doctor') {
    & "$env:JAVA_HOME\bin\java.exe" -version
    Write-Host "Android SDK: $sdk"
    foreach ($tool in @($sdkManager, $adb, $emulator)) {
        Write-Host "$tool : $(Test-Path $tool)"
    }
    if (Test-Path $emulator) {
        # Call the checker directly; the emulator wrapper mishandles spaces in
        # some Windows user-profile paths when forwarding this command.
        & (Join-Path $sdk 'emulator\emulator-check.exe') accel
        & $emulator -list-avds
    }
    if (Test-Path $adb) { & $adb devices -l }
    exit
}

if ($Action -eq 'Build') { Invoke-Build -Tasks @('assembleDebug'); exit }
if ($Action -eq 'Check') { Invoke-Build -Tasks @('assembleDebug', 'lintDebug'); exit }

if (-not (Test-Path $emulator) -or -not (Test-Path $adb)) { throw 'Run .\android.ps1 Setup first.' }
if ($Action -eq 'Emulator') {
    Start-Process -FilePath $emulator -ArgumentList @('-avd', $avdName) -WindowStyle Normal
    exit
}

Invoke-Build -Tasks @('assembleDebug')
if (-not $DeviceSerial) {
    $devices = @(& $adb devices | Select-String '^\S+\s+device$' | ForEach-Object { ($_.Line -split '\s+')[0] })
    if ($devices.Count -gt 1) { throw 'Multiple devices connected. Run with -DeviceSerial <serial> (see adb devices).' }
    if ($devices.Count -eq 1) { $DeviceSerial = $devices[0] }
    else {
        Start-Process -FilePath $emulator -ArgumentList @('-avd', $avdName) -WindowStyle Normal
    }
}
$deadline = (Get-Date).AddMinutes(4)
do {
    if (-not $DeviceSerial) {
        $DeviceSerial = (& $adb devices | Select-String '^emulator-\d+\s+device$' | Select-Object -First 1 | ForEach-Object { ($_.Line -split '\s+')[0] })
    }
    if ($DeviceSerial) { $boot = & $adb -s $DeviceSerial shell getprop sys.boot_completed 2>$null }
    if ($boot -eq '1') { break }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
if ($boot -ne '1') { throw 'Device did not finish booting. Run Doctor to check emulator acceleration and connected devices.' }
& $adb -s $DeviceSerial install -r (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk')
Assert-Success 'APK installation'
& $adb -s $DeviceSerial shell am start -W -n 'com.collegediscovery.foodtracker/.FoodTrackerApp'
Assert-Success 'App launch'
