// 项目级构建配置 - 腾讯云镜像 + 模拟器优化
plugins {
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

// Gradle 13兼容性配置
subprojects {
    configurations.all {
        resolutionStrategy {
            // 强制使用兼容版本
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
            
            // 缓存配置 - 模拟器开发优化
            cacheDynamicVersionsFor(10, "minutes")
            cacheChangingModulesFor(4, "hours")
        }
    }
}

// 任务配置
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}