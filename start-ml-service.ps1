# PowerShell script to launch TermaGuard FastAPI ML Microservice on port 5000
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $ScriptDir "ml-service")

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Starting TermaGuard.AI FastAPI ML Microservice..." -ForegroundColor Green
Write-Host "Endpoint: http://localhost:5000 (Predict & Retrain)" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

python -m uvicorn app:app --host 0.0.0.0 --port 5000 --reload
