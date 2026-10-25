param([string[]]$Tasks = @('assembleDebug'))
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
if (-not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) {
    throw 'JDK Android Studio tidak ditemukan. Sesuaikan JAVA_HOME di build.ps1.'
}
Push-Location $PSScriptRoot
try {
    & "$PSScriptRoot\gradlew.bat" @Tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle gagal dengan kode $LASTEXITCODE" }
} finally {
    Pop-Location
}
