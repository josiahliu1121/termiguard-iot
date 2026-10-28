# TermaGuard Backend Start Script
$env:JAVA_HOME = "D:\Java\JDK_17"
$env:Path = "D:\Java\JDK_17\bin;D:\Java\Maven\apache-maven-3.9.6-bin\apache-maven-3.9.6\bin;" + $env:Path

Set-Location -Path $PSScriptRoot

if (Test-Path "backend\target\termiguard-backend-1.0.0.jar") {
    Write-Host ">>> Starting TermaGuard Spring Boot Backend (Port 8080)..." -ForegroundColor Green
    java -jar backend\target\termiguard-backend-1.0.0.jar
} else {
    Write-Host ">>> Building and starting via Maven..." -ForegroundColor Yellow
    Set-Location "backend"
    mvn spring-boot:run
}
