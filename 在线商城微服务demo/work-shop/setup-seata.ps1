$mysqlPath = "mysql"
$host = "localhost"
$user = "root"
$password = "liujian3732028"
$sqlFile = "d:\BaiduNetdiskDownload\class\work-shop\init-seata.sql"

# 使用 mysql 命令行工具执行 SQL
$env:MYSQL_PWD = $password
& $mysqlPath -h$host -u$user -e "source $sqlFile"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Seata 数据库初始化成功！" -ForegroundColor Green
} else {
    Write-Host "Seata 数据库初始化失败！" -ForegroundColor Red
}
