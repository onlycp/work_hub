import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("plugin.compose") version "2.1.0"
    id("org.jetbrains.compose")
}

group = "com.free50s"
version = "1.0-SNAPSHOT"

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
        
        // 添加 JVM 参数以修复 Windows 上的 JMX 错误
        jvmArgs("-Dcom.sun.management.jmxremote=false")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "workhub"
            packageVersion = "1.0.0"

            // 设置应用图标 - 使用 icon.png 作为主图标，不同平台使用对应格式
            macOS {
                // macOS 使用 .icns 格式（包含多个尺寸）
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
            }
            windows {
                // Windows 使用 .ico 格式，确保使用正确的图标文件
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
