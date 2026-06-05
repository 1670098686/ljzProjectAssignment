// 项目设置配置 - 腾讯云镜像
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        maven(url = "https://jitpack.io")
    }
}

// 项目根目录名称
rootProject.name = "CampusTrade"

// 前端 Android 模块已移除，后续将使用 Flutter 重新实现