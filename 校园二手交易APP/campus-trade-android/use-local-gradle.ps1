# 使用本地Gradle 8.13的脚本
# 直接调用本地安装的Gradle，避免网络下载

# 设置本地Gradle路径
$gradlePath = "C:\Users\1670098686\.gradle\wrapper\dists\gradle-8.13-all\b3r9nucads82twfdqsrgpqpui\gradle-8.13\bin\gradle.bat"

# 检查Gradle是否存在
if (Test-Path $gradlePath) {
    Write-Host "使用本地Gradle 8.13..."
    Write-Host "Gradle路径: $gradlePath"
    Write-Host ""
    
    # 执行Gradle命令
    & $gradlePath $args
} else {
    Write-Error "本地Gradle 8.13未找到，请检查路径: $gradlePath"
    Write-Host ""
    Write-Host "可用的Gradle版本:"
    Get-ChildItem "C:\Users\1670098686\.gradle\wrapper\dists\" -Directory | ForEach-Object { $_.Name }
}