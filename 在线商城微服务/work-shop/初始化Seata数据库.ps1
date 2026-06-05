# Seata 数据库初始化脚本
# 使用 root 用户执行

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Seata 数据库初始化工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$mysqlPath = "mysql"
$host = "localhost"
$user = "root"
$password = "liujian3732028"
$sqlFile = "d:\BaiduNetdiskDownload\class\work-shop\初始化Seata数据库_单步执行.sql"

Write-Host "数据库配置:" -ForegroundColor Yellow
Write-Host "  主机: $host"
Write-Host "  用户: $user"
Write-Host "  SQL文件: $sqlFile"
Write-Host ""

Write-Host "正在连接数据库并执行 SQL..." -ForegroundColor Green

try {
    $env:MYSQL_PWD = $password
    Get-Content $sqlFile | & $mysqlPath -h$host -u$user
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ Seata 数据库初始化成功！" -ForegroundColor Green
        Write-Host ""
        Write-Host "现在可以启动 Seata 服务了。" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "✗ 执行失败，请检查错误信息。" -ForegroundColor Red
    }
} catch {
    Write-Host ""
    Write-Host "✗ 发生错误: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "备选方案:" -ForegroundColor Yellow
    Write-Host "1. 使用 MySQL 客户端工具（如 Navicat、DBeaver 等）" -ForegroundColor White
    Write-Host "2. 用 root 用户登录 MySQL" -ForegroundColor White
    Write-Host "3. 打开并执行文件: 初始化Seata数据库_单步执行.sql" -ForegroundColor White
}

Write-Host ""
Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
