@echo off
echo ========================================
echo  个人收支记账APP - 后端服务启动脚本
echo ========================================
echo.

REM 检查Java版本
echo [INFO] 检查Java版本...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java未安装或环境变量未配置
    pause
    exit /b 1
)

REM 检查Maven
echo [INFO] 检查Maven...
mvn -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven未安装或环境变量未配置
    pause
    exit /b 1
)

echo [INFO] 开始启动后端服务...
echo.

REM 进入项目目录
cd /d "%~dp0"

REM 检查MySQL连接
echo [INFO] 检查MySQL服务状态...
net start mysql 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] MySQL服务已启动
) else (
    echo [WARNING] MySQL服务未启动，请手动启动MySQL
)

REM 构建项目
echo [INFO] 正在构建项目...
mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 项目构建失败
    pause
    exit /b 1
)

REM 启动应用
echo [INFO] 启动Spring Boot应用...
echo [INFO] 服务将在 http://localhost:8081 启动
echo [INFO] API文档: http://localhost:8081/swagger-ui.html
echo [INFO] 健康检查: http://localhost:8081/api/health/status
echo.
echo 按Ctrl+C停止服务
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=dev

pause