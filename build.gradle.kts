import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// 规范化版本号，确保符合 MSI 要求的 MAJOR.MINOR.BUILD 格式
fun normalizeVersion(version: String): String {
    val parts = version.split(".")
    val major = parts.getOrElse(0) { "1" }.toIntOrNull() ?: 1
    val minor = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
    val build = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
    return "$major.$minor.$build"
}

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("plugin.compose") version "2.1.0"
    id("org.jetbrains.compose")
}

group = "com.free50s"

// 从环境变量读取版本号，如果没有则使用默认值
val buildVersion = System.getenv("BUILD_VERSION") ?: "1.0-SNAPSHOT"
version = buildVersion

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)  // 扩展图标库

    // Coroutines for asynchronous operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Serialization for JSON handling
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // SSH client library (using SSHJ, same as HomeApp)
    implementation("com.hierynomus:sshj:0.37.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")

    // System tray support (cross-platform native)
    implementation("io.github.kdroidfilter:composenativetray:1.0.4")

    // JGit for Git operations - 使用较新版本，可能修复了 JMX 问题
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        // JVM参数优化 - 减少内存使用
        jvmArgs(
            "-Djava.awt.headless=false",
            "-Dorg.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled=true",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=100",
            "-Xms64m",
            "-Xmx512m"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "WorkHub"
            // 从环境变量读取包版本号，如果没有则从项目版本中提取（去掉 -SNAPSHOT）
            val packageVersionStr = System.getenv("PACKAGE_VERSION") ?: buildVersion.removeSuffix("-SNAPSHOT")
            // 确保版本号格式为 MAJOR.MINOR.BUILD（MSI 要求）
            val normalizedVersion = normalizeVersion(packageVersionStr)
            packageVersion = normalizedVersion

            // 设置应用图标 - 使用 icon.png 作为主图标，不同平台使用对应格式
            macOS {
                // macOS 使用 .icns 格式（包含多个尺寸）
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
            }
            windows {
                // Windows 使用 .ico 格式（包含多个尺寸，更好的显示效果）
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
                // 配置开始菜单快捷方式
                menu = true
                menuGroup = "WorkHub"
            }
            linux {
                // Linux 使用 .png 格式，直接使用主图标文件
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

// 自定义任务：创建精简的JRE运行时
tasks.register<Exec>("createCustomJre") {
    dependsOn("createDistributable")

    val jreDir = file("${buildDir}/compose/binaries/main-release/app/WorkHub-jre")
    val jlinkExecutable = "${System.getProperty("java.home")}/bin/jlink"

    commandLine(
        jlinkExecutable,
        "--module-path", "${System.getProperty("java.home")}/jmods",
        "--add-modules",
        "java.base,java.desktop,java.logging,java.xml,java.naming,java.security.jgss,java.instrument,jdk.unsupported,jdk.crypto.ec",
        "--output", jreDir.absolutePath,
        "--strip-debug",
        "--compress", "2",
        "--no-header-files",
        "--no-man-pages"
    )

    doLast {
        println("Custom JRE created at: ${jreDir.absolutePath}")
        println("JRE size: ${jreDir.listFiles()?.sumOf { it.length() } ?: 0} bytes")
    }
}

// 优化任务：分析和清理不必要的文件
tasks.register("optimizePackage") {
    dependsOn("createDistributable", "createCustomJre")

    doLast {
        val appDir = file("${buildDir}/compose/binaries/main-release/app")

        // 删除不必要的文件
        appDir.walkTopDown().forEach { file ->
            if (file.isFile && (
                file.name.endsWith(".pdb") ||  // Windows调试符号
                file.name.endsWith(".exp") ||  // Windows导出文件
                file.name.endsWith(".lib") ||  // Windows库文件
                file.name.contains("debug")    // 调试相关文件
            )) {
                file.delete()
                println("Deleted unnecessary file: ${file.name}")
            }
        }

        // 分析包大小
        val totalSize = appDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        println("Total package size: ${totalSize / (1024 * 1024)} MB")

        val jreSize = file("${appDir}/WorkHub-jre").walkTopDown().filter { it.isFile }.sumOf { it.length() }
        println("JRE size: ${jreSize / (1024 * 1024)} MB")

        val appSize = totalSize - jreSize
        println("Application size (without JRE): ${appSize / (1024 * 1024)} MB")
    }
}
