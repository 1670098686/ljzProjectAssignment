@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo ========================================
echo   One-Click: Rebuild + Launch + Monitor
echo ========================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0oneclick.ps1"
echo.
echo ========================================
echo   Script finished
echo ========================================
pause