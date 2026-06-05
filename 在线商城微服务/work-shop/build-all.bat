@echo off
setlocal

echo ========================================
echo  Mall System Build Script
echo ========================================
echo.

set PROJECT_ROOT=D:\BaiduNetdiskDownload\class\work-shop

REM Build all modules from root (using parent pom.xml)
echo Building all modules...
cd "%PROJECT_ROOT%"
mvn clean package -DskipTests
echo.

echo ========================================
echo  Build process completed!
echo ========================================
echo.
echo JAR files are available in each module's target directory.
echo.
pause