param(
    [ValidateSet('run','test','package')][string]$Task = 'run',
    [switch]$DemoOpen,
    [int]$Port = 8080
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localJdk = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName; $env:Path = "$($localJdk.FullName)\bin;$env:Path" }
$maven = Join-Path $projectRoot '.tools\apache-maven-3.9.11\bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $maven)) {
    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mavenCommand) { throw 'Install Maven 3.6.3+ and Java 21, then run this script again.' }
    $maven = $mavenCommand.Source
}
Push-Location (Join-Path $projectRoot 'backend')
try {
    switch ($Task) {
        'run' {
            $env:PORT = [string]$Port
            $env:TRADING_DEMO_OPEN_MARKET = if ($DemoOpen) { 'true' } else { 'false' }
            & $maven spring-boot:run
        }
        'test' { & $maven test }
        'package' { & $maven verify }
    }
    if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
