@echo off
setlocal

echo ========================================
echo  Mall System Build and Start Script
echo ========================================
echo.

REM Build all modules first
echo [1] Building all modules...
call .\build-all.bat
if %errorlevel% neq 0 (
    echo ERROR: Build failed, aborting startup.
    pause
    exit /b 1
)
echo.

REM Start all services
echo [2] Starting all services...
call .\start-all-simple.bat

echo ========================================
echo  Build and Start process completed!
echo ========================================
echo.