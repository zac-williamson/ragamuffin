# build-local.ps1
# Builds the Ragamuffin TeaVM web export locally.
# Run from the project root: .\build-local.ps1

param(
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

if ($Clean) {
    Write-Host "Cleaning previous build..." -ForegroundColor Yellow
    .\gradlew.bat clean
}

Write-Host "Building TeaVM web export..." -ForegroundColor Cyan
.\gradlew.bat teavm:build

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build succeeded." -ForegroundColor Green
    Write-Host "Output: teavm/build/generated/js/" -ForegroundColor Gray
} else {
    Write-Host "Build failed (exit code $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}
