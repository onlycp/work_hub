import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.kdroid.composetray.tray.api.Tray
import data.*
import kotlinx.coroutines.*
import service.*
import ui.*
import kotlin.system.exitProcess
import javax.swing.JOptionPane

fun main() {
    // 设置全局异常处理器
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        println("💥 未捕获异常 in ${thread.name}: ${throwable.message}")
        throwable.printStackTrace()
        // 在Windows上显示一个简单的错误对话框
        try {
            JOptionPane.showMessageDialog(
                null,
                "应用发生错误: ${throwable.message}\n\n请查看控制台输出获取详细信息。",
                "WorkHub 错误",
                JOptionPane.ERROR_MESSAGE
            )
        } catch (e: Exception) {
            // 如果连对话框都显示不了，那就只能打印了
            println("❌ 无法显示错误对话框: ${e.message}")
        }
    }

    application(exitProcessOnExit = false) {
    // 修复 Windows 上的 JMX 错误：禁用 JMX 远程管理
    System.setProperty("com.sun.management.jmxremote", "false")

    // 调试信息：输出系统信息
    println("🚀 WorkHub 启动中...")
    println("📊 系统信息:")
    println("  OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
    println("  Arch: ${System.getProperty("os.arch")}")
    println("  Java: ${System.getProperty("java.version")}")
    println("  User: ${System.getProperty("user.name")}")
    println("  Dir: ${System.getProperty("user.dir")}")

    // 应用初始化状态
    var isInitialized by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    // 重新评估应用状态的函数
    val reevaluateAppState: () -> Unit = {
        println("🔄 重新评估应用状态...")
        isInitialized = false
        isLoggedIn = false
        showLoginDialog = false
        initializationError = null
    }

    // 执行应用初始化的函数
    suspend fun performAppInitialization() {
        try {
            println("🚀 开始应用初始化...")
            val initResult = AppInitializer.initialize()
            println("📋 初始化结果: ${if (initResult.isSuccess) "成功" else "失败"}")

            if (initResult.isSuccess) {
                isInitialized = true
                println("✅ 应用初始化完成")

                // 检查是否已有登录用户
                val isLoggedInCheck = CurrentUserManager.isLoggedIn()
                println("👤 检查登录状态: ${if (isLoggedInCheck) "已登录" else "未登录"}")

                if (isLoggedInCheck) {
                    isLoggedIn = true
                    println("✅ 使用已登录状态")
                } else {
                    // 检查是否启用自动登录
                    val autoLoginEnabled = LoginSettingsManager.isAutoLoginEnabled()
                    println("🔐 自动登录启用: $autoLoginEnabled")

                    if (autoLoginEnabled) {
                        println("🔐 检测到自动登录设置，开始自动登录...")
                        try {
                            val username = LoginSettingsManager.getRememberedUsername()
                            val password = LoginSettingsManager.getRememberedPassword()
                            println("👤 自动登录用户名: ${username.takeIf { it.isNotBlank() } ?: "未设置"}")

                            if (username.isNotBlank() && password.isNotBlank()) {
                                val loginResult = AppInitializer.loginUser(username)
                                if (loginResult.isSuccess) {
                                    println("✅ 自动登录成功")
                                    isLoggedIn = true
                                    return
                                } else {
                                    println("❌ 自动登录失败: ${loginResult.exceptionOrNull()?.message}")
                                }
                            } else {
                                println("❌ 自动登录信息不完整")
                            }
                        } catch (e: Exception) {
                            println("❌ 自动登录异常: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    // 如果没有自动登录或自动登录失败，显示登录对话框
                    showLoginDialog = true
                    println("📝 显示登录对话框")
                }
            } else {
                val errorMsg = initResult.exceptionOrNull()?.message ?: "初始化失败"
                initializationError = errorMsg
                println("❌ 初始化失败: $errorMsg")
                initResult.exceptionOrNull()?.printStackTrace()
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "未知错误"
            initializationError = errorMsg
            println("💥 初始化异常: $errorMsg")
            e.printStackTrace()
        }
    }

    // 应用启动初始化
    LaunchedEffect(Unit) {
        performAppInitialization()
    }

    // 监听重新评估状态的触发
    var reevaluateTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(reevaluateTrigger) {
        if (reevaluateTrigger > 0) {
            performAppInitialization()
        }
    }

    val windowState = rememberWindowState(
        size = DpSize(1400.dp, 900.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )

    var shouldExit by remember { mutableStateOf(false) }
    var isWindowVisible by remember { mutableStateOf(true) }
    var shouldMinimizeToTray by remember { mutableStateOf(false) }

    // 监听窗口最小化状态，如果需要最小化到托盘，则隐藏窗口
    LaunchedEffect(windowState.isMinimized) {
        if (windowState.isMinimized && shouldMinimizeToTray) {
            // 延迟一下再隐藏，确保最小化动画完成
            kotlinx.coroutines.delay(100)
            isWindowVisible = false
            windowState.isMinimized = false
            shouldMinimizeToTray = false
            println("✓ 窗口已隐藏到托盘")
        }
    }

    // 监听退出信号
    if (shouldExit) {
        println("📤 应用准备退出...")
        LaunchedEffect(Unit) {
            performExitCleanup()
        }
    }

    // 显示/恢复窗口的函数
    val showWindow: () -> Unit = remember {
        {
            isWindowVisible = true
            windowState.isMinimized = false
            println("🔄 托盘：显示窗口")
        }
    }

    // 设置macOS Dock图标点击监听（在Window创建后设置）
    DisposableEffect(Unit) {
        println("🚀 开始设置Dock监听器...")
        var cleanup: (() -> Unit)? = null
        
        try {
            val osName = System.getProperty("os.name").lowercase()
            println("🖥️ 当前操作系统: $osName")
            if (osName.contains("mac")) {
                // 使用反射调用Desktop API（兼容不同JDK版本）
                val desktopClass = Class.forName("java.awt.Desktop")
                val isDesktopSupportedMethod = desktopClass.getMethod("isDesktopSupported")
                val isSupported = isDesktopSupportedMethod.invoke(null) as Boolean
                println("🖥️ Desktop支持: $isSupported")
                
                if (isSupported) {
                    val getDesktopMethod = desktopClass.getMethod("getDesktop")
                    val desktop = getDesktopMethod.invoke(null)
                    
                    // 尝试设置AppReopenedListener
                    try {
                        val actionClass = Class.forName("java.awt.Desktop\$Action")
                        val appReopenAction = actionClass.enumConstants.find { 
                            it.toString() == "APP_REOPEN"
                        }
                        println("🖥️ APP_REOPEN action找到: ${appReopenAction != null}")
                        
                        if (appReopenAction != null) {
                            val isSupportedMethod = desktopClass.getMethod("isSupported", actionClass)
                            val actionSupported = isSupportedMethod.invoke(desktop, appReopenAction) as Boolean
                            println("🖥️ APP_REOPEN支持: $actionSupported")
                            
                            if (actionSupported) {
                                val listenerClass = Class.forName("java.awt.desktop.AppReopenedListener")
                                val proxy = java.lang.reflect.Proxy.newProxyInstance(
                                    listenerClass.classLoader,
                                    arrayOf(listenerClass)
                                ) { _, _, _ ->
                                    println("🖱️ Dock图标被点击！")
                                    showWindow()
                                    null
                                }
                                
                                val setListenerMethod = desktopClass.getMethod("setAppReopenedListener", listenerClass)
                                setListenerMethod.invoke(desktop, proxy)
                                println("✓ macOS Dock图标监听已设置")
                            } else {
                                println("⚠️ 系统不支持APP_REOPEN action")
                            }
                        } else {
                            println("⚠️ 未找到APP_REOPEN action")
                        }
                    } catch (e: Exception) {
                        println("⚠️ 设置Dock监听失败（可能是JDK版本不支持）")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ 设置Dock监听失败")
            e.printStackTrace()
        }
        
        onDispose {
            cleanup?.invoke()
        }
    }

    // 系统托盘
    Tray(
        icon = painterResource("icon.png"),
        tooltip = "WorkHub - 您的工作室",
        primaryAction = {
            // 点击托盘图标显示窗口
            showWindow()
        },
        menuContent = {
            Item(label = "显示窗口") {
                showWindow()
            }

            Divider()

            Item(label = "退出") {
                println("📤 托盘：请求退出")
                shouldExit = true
            }
        }
    )

    println("🏗️ 创建窗口...")

    Window(
        onCloseRequest = {
            println("❌ 窗口关闭请求")
            // 点击关闭按钮时最小化到托盘
            shouldMinimizeToTray = true
            windowState.isMinimized = true
        },
        title = "WorkHub - 您的工作室",
        icon = painterResource("icon.png"),
        state = windowState,
        visible = isWindowVisible,
        alwaysOnTop = false,
        onPreviewKeyEvent = { false },
        focusable = true
    ) {
        println("✅ 窗口创建成功，准备显示内容")
        // 监听窗口焦点变化，处理任务栏点击
        LaunchedEffect(isWindowVisible) {
            if (isWindowVisible) {
                println("✓ 窗口已显示")
            }
        }
        // 显示内容
        when {
            initializationError != null -> {
                // 初始化错误显示
                ErrorScreen(
                    errorMessage = initializationError!!,
                    onRetry = {
                        initializationError = null
                        // 重新初始化
                        // 这里可以添加重试逻辑
                    }
                )
            }
            !isLoggedIn -> {
                // 显示加载中或登录对话框
                if (showLoginDialog) {
                    UserLoginDialog(
                        onLoginSuccess = {
                            showLoginDialog = false
                            isLoggedIn = true
                        },
                        onDismiss = {
                            // 不允许关闭登录对话框，除非退出应用
                        },
                        onRepositoryConfigured = {
                            // 仓库配置完成后，触发重新评估应用状态
                            reevaluateTrigger++
                        }
                    )
                } else {
                    // 显示加载屏幕
                    LoadingScreen("正在初始化应用...")
                }
            }
            else -> {
                // 显示主应用
                App(onLogout = {
                    isLoggedIn = false
                    showLoginDialog = true
                })
            }
        }
    }
}
}


/**
 * 执行退出清理并退出应用
 */
fun performExitCleanup() {
    println("📤 开始执行退出清理...")

    // 同步Git数据
    runBlocking {
        try {
            val syncResult = AppInitializer.syncData()
            if (syncResult.isSuccess) {
                println("✓ 数据同步完成")
            } else {
                println("⚠️ 数据同步失败: ${syncResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            println("⚠️ 数据同步异常: ${e.message}")
        }
    }

    // 清理应用资源
    AppInitializer.shutdown()

    // 这里可以添加其他清理逻辑，比如断开SSH连接等
    // TODO: 如果需要清理SSH连接或其他资源，在这里添加

    println("✓ 清理完成，正在退出进程...")
    kotlin.system.exitProcess(0)
}
