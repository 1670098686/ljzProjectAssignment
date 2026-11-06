// 根级 build.gradle.kts —— 使用 settings.gradle.kts 中声明的 plugins DSL
// 不再需要老的 buildscript {} 块（AGP/Kotlin 版本已在 settings 里统一声明）

// 将所有模块的构建输出目录统一迁移到项目外的 build 目录
val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
