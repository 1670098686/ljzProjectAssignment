@echo off
setlocal enabledelayedexpansion

echo ========================================
echo  Mall System Startup Script (Dev Mode)
echo ========================================
echo.

set PROJECT_ROOT=D:\BaiduNetdiskDownload\class\work-shop
set NACOS_PATH=D:\yyrj\nacos
set SEATA_PATH=D:\yyrj\seata-server

echo [1] Starting Nacos (Service Discovery)...
if exist "%NACOS_PATH%\bin\startup.cmd" (
    start "Nacos" /d "%NACOS_PATH%\bin" cmd /k "startup.cmd -m standalone"
    echo Nacos starting on port 8848 (Console: http://localhost:8848/nacos/)
) else (
    echo ERROR: Nacos not found at %NACOS_PATH%
)
echo.

echo [2] Waiting for Nacos to be ready (60 seconds)...
echo This ensures Nacos is fully started before Gateway registration...
ping 127.0.0.1 -n 61 >nul
echo.

echo [3] Checking Nacos health...
powershell -Command "$result = Get-NetTCPConnection -LocalPort 8848 -ErrorAction SilentlyContinue; if ($result) { Write-Host 'Nacos is listening on port 8848' } else { Write-Host 'Nacos not ready yet, waiting...' }"
echo.

echo [4] Starting Seata (Distributed Transaction - Optional)...
if exist "%SEATA_PATH%\bin\seata-server.bat" (
    start "Seata" /d "%SEATA_PATH%\bin" cmd /k "seata-server.bat"
    echo Seata starting on port 8091
) else (
    echo WARNING: Seata not found at %SEATA_PATH% - skipping (not required)
)
echo.

echo [5] Waiting for Seata to start...
ping 127.0.0.1 -n 10 >nul
echo.

echo [6] Starting Gateway (must register to Nacos first)...
if exist "%PROJECT_ROOT%\mall-gateway\target\mall-gateway-1.0.0.jar" (
    start "Gateway" cmd /k "java -jar %PROJECT_ROOT%\mall-gateway\target\mall-gateway-1.0.0.jar"
    echo Gateway starting on port 8080
) else (
    echo ERROR: Gateway JAR not found at %PROJECT_ROOT%\mall-gateway\target\mall-gateway-1.0.0.jar
    echo Please build the gateway first: cd %PROJECT_ROOT%\mall-gateway ^&^& mvn clean package
)
echo.

echo [7] Waiting for Gateway to register with Nacos (20 seconds)...
ping 127.0.0.1 -n 21 >nul
echo.

echo [8] Starting microservices...
set SERVICES=User Product Stock Cart Order Favorite Review
set PORTS=8081 8082 8083 8084 8085 8086 8087

for %%s in (%SERVICES%) do (
    set SERVICE_JAR=%PROJECT_ROOT%\mall-%%s\target\mall-%%s-1.0.0.jar
    if exist "!SERVICE_JAR!" (
        start "%%s" cmd /k "java -jar !SERVICE_JAR!"
        echo %%s service starting
    ) else (
        echo WARNING: %%s service JAR not found - !SERVICE_JAR!
    )
)
echo All microservices starting
echo.

echo [9] Waiting for microservices to register with Nacos (30 seconds)...
ping 127.0.0.1 -n 31 >nul
echo.

echo ========================================
echo  Service Status Check
echo ========================================
echo.

call :check_port 8848 Nacos
call :check_port 8080 Gateway
call :check_port 8091 Seata
call :check_port 8081 User
call :check_port 8082 Product
call :check_port 8083 Stock
call :check_port 8084 Cart
call :check_port 8085 Order
call :check_port 8086 Favorite
call :check_port 8087 Review

echo.
echo ========================================
echo  All backend services startup attempted!
echo ========================================
echo.
echo Access Points:
echo - Frontend (Dev): http://localhost:3000  ^(Vite Dev Server^)
echo - Gateway:        http://localhost:8080 (direct)
echo - Nacos:         http://localhost:8848/nacos/ (user: nacos, pass: nacos)
echo.
echo IMPORTANT - To start Frontend:
echo   Open a NEW terminal and run:
echo   cd %PROJECT_ROOT%\mall-frontend
echo   npm run dev
echo.
echo Startup Order Summary:
echo 1. Nacos (8848) - Service Discovery
echo 2. Seata (8091) - Distributed Transaction
echo 3. Gateway (8080) - API Gateway
echo 4. Microservices (8081-8087)
echo 5. Frontend (3000) - Vite Dev Server ^(separate terminal^)
echo.
echo If services failed to start, check individual service windows for errors.
echo.
pause
exit /b

:check_port
netstat -ano | findstr ":%1 " | findstr LISTENING >nul
if %errorlevel% equ 0 (
    echo %2: Running on port %1
) else (
    echo %2: NOT running on port %1 - check service window
)
exit /b 0