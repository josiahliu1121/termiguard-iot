@echo off
title TermaGuard ML Microservice (FastAPI :5000)
cd /d "%~dp0ml-service"
echo ========================================================
echo Starting TermaGuard.AI Machine Learning Microservice...
echo Serving at http://localhost:5000 (FastAPI + Uvicorn)
echo ========================================================
python -m uvicorn app:app --host 0.0.0.0 --port 5000 --reload
pause
