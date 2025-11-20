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

    // JGit for Git operations
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        
        // 设置 JVM 参数，禁用 JMX 以避免 Windows 上的 MalformedObjectNameException
        jvmArgs(
            "-Djava.awt.headless=false",
            "-Dcom.sun.management.jmxremote=false",
            "-Dorg.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled=true",
            "-Djavax.management.builder.initial=java.lang.management.ManagementFactory"
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
                // Windows 使用 .png 格式（Compose Desktop 支持，参考 HomeApp 配置）
                // 使用主图标文件，确保图标能正确显示
                iconFile.set(project.file("src/main/resources/icon.png"))
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
