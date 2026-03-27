@echo off
echo ========================================
echo  数据库部署脚本 - 个人收支记账APP
echo ========================================
echo.

REM 设置数据库配置
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=flutter
set DB_USER=test
set DB_PASS=

echo [INFO] 数据库配置:
echo   主机: %DB_HOST%:%DB_PORT%
echo   数据库: %DB_NAME%
echo   用户: %DB_USER%
echo.

REM 检查MySQL服务
echo [INFO] 检查MySQL服务状态...
net start mysql 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] MySQL服务已启动
) else (
    echo [WARNING] MySQL服务未启动，尝试启动...
    net start mysql
    if %ERRORLEVEL% EQU 0 (
        echo [INFO] MySQL服务启动成功
    ) else (
        echo [ERROR] MySQL服务启动失败，请手动启动MySQL
        pause
        exit /b 1
    )
)

REM 创建数据库
echo [INFO] 创建数据库 %DB_NAME%...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 创建数据库失败
    echo [TIP] 请检查MySQL连接配置和用户权限
    pause
    exit /b 1
)

REM 执行初始化脚本
echo [INFO] 执行数据库初始化脚本...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < "src\main\resources\db\migration\V1__init_schema.sql"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 数据库初始化失败
    echo [TIP] 请检查SQL脚本文件是否存在
    pause
    exit /b 1
)

REM 验证表结构
echo [INFO] 验证数据库表结构...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% -e "SHOW TABLES;"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 验证数据库表失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo [SUCCESS] 数据库部署成功！
echo ========================================
echo.
echo [NEXT] 现在可以启动后端服务了:
echo   1. 运行 start-backend.bat
echo   2. 或者执行: mvn spring-boot:run
echo.
echo [INFO] 启动后可以访问:
echo   API文档: http://localhost:8081/swagger-ui.html
echo   健康检查: http://localhost:8081/api/health/status
echo.

pause