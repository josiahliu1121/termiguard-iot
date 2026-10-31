@echo off
title TermaGuard 4G Edge Station Telemetry Simulator
cd /d "%~dp0"
echo ======================================================================
echo Starting TermaGuard.AI 4G Cellular Telemetry Edge Simulator...
echo Target: http://localhost:8080/api/v1/telemetry/uplink
echo ======================================================================
powershell -ExecutionPolicy Bypass -File "%~dp0simulate-uplink.ps1" -Target all
pause
