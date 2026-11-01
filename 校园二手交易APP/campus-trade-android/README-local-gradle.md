# 本地Gradle使用指南

## 概述
本项目已配置为使用本地安装的Gradle 8.13版本，避免网络下载，提高构建速度。

## 使用方法

### 1. 使用本地Gradle脚本（推荐）
运行以下命令使用本地Gradle：
```powershell
.\use-local-gradle.ps1 <gradle命令>
```

示例：
```powershell
# 查看版本
.\use-local-gradle.ps1 --version

# 清理项目
.\use-local-gradle.ps1 clean

# 构建项目
.\use-local-gradle.ps1 build

# 运行应用
.\use-local-gradle.ps1 assembleDebug
```

### 2. 直接使用本地Gradle路径
您也可以直接调用本地Gradle：
```powershell
C:\Users\1670098686\.gradle\wrapper\dists\gradle-8.13-all\b3r9nucads82twfdqsrgpqpui\gradle-8.13\bin\gradle.bat <命令>
```

## 本地Gradle位置
- **路径**: `C:\Users\1670098686\.gradle\wrapper\dists\gradle-8.13-all\b3r9nucads82twfdqsrgpqpui\gradle-8.13`
- **版本**: 8.13
- **构建时间**: 2025-02-25

## 优势
- ✅ 无需网络下载Gradle
- ✅ 构建速度更快
- ✅ 避免网络问题导致的构建失败
- ✅ 使用已验证的稳定版本

## 注意事项
- 如果本地Gradle路径发生变化，需要更新`use-local-gradle.ps1`脚本中的路径
- 项目仍保留原始的`gradlew`脚本用于兼容性
- 如果遇到构建问题，可以切换回使用`gradlew`脚本