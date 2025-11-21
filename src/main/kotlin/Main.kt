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
import utils.Logger
import kotlin.system.exitProcess
import java.net.ServerSocket
import java.net.SocketException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName

/**
 * 检查应用是否已有实例在运行
 * 使用Socket端口绑定方式实现单实例检查
 */
fun checkSingleInstance(): Boolean {
    val port = 9999 // 使用一个不常用的端口号
    return try {
        // 尝试绑定到指定端口
        val serverSocket = ServerSocket(port)
        // 如果绑定成功，说明这是第一个实例
        // 启动后台线程监听端口，保持绑定状态
        Thread {
            try {
                while (true) {
                    val clientSocket = serverSocket.accept()
                    // 如果有其他实例尝试连接，关闭连接
                    clientSocket.close()
                }
            } catch (e: Exception) {
                // 忽略异常，可能是正常关闭
            }
        }.apply {
            isDaemon = true
            start()
        }
        true
    } catch (e: Exception) {
        // 如果绑定失败，说明已有实例在运行
        Logger.log("⚠️ 检测到已有WorkHub实例在运行，退出当前实例")
        false
    }
}

fun main() {
    // 设置全局未捕获异常处理器 - 捕获所有逃逸的异常包括Error
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        when (throwable) {
            is OutOfMemoryError -> {
                Logger.error("未捕获的内存不足错误 (线程: ${thread.name})", throwable)
                println("💥 未捕获的内存不足错误 (线程: ${thread.name}): ${throwable.message}")
                println("🚨 建议重启应用以释放内存")
            }
            is StackOverflowError -> {
                Logger.error("未捕获的栈溢出错误 (线程: ${thread.name})", throwable)
                println("💥 未捕获的栈溢出错误 (线程: ${thread.name}): ${throwable.message}")
                println("🚨 可能存在递归调用问题，建议检查代码逻辑")
            }
            is Error -> {
                Logger.error("未捕获的系统错误 (线程: ${thread.name})", throwable)
                println("💥 未捕获的系统错误 (线程: ${thread.name}): ${throwable.javaClass.simpleName} - ${throwable.message}")
                println("🚨 系统级错误，应用可能无法正常运行")
            }
            is Exception -> {
                Logger.error("未捕获的异常 (线程: ${thread.name})", throwable)
                println("💥 未捕获的异常 (线程: ${thread.name}): ${throwable.javaClass.simpleName} - ${throwable.message}")
            }
            else -> {
                Logger.error("未捕获的未知异常 (线程: ${thread.name})", throwable)
                println("💥 未捕获的未知异常 (线程: ${thread.name}): ${throwable.javaClass.simpleName} - ${throwable.message}")
            }
        }
    }

    // 检查单实例运行
    if (!checkSingleInstance()) {
        Logger.log("📤 已有实例在运行，退出应用")
        return
    }

    // 立即初始化日志系统
    Logger.log("🚀 WorkHub 应用启动中...")
    Logger.log("📝 日志文件位置: ${Logger.getLogFilePath()}")

    // 禁用 JMX 相关功能，避免在 Windows 上出现 MalformedObjectNameException
    // 这个错误通常由 JGit 的 JMX 监控功能引起
    try {
        // 确保在 GUI 模式下运行
        System.setProperty("java.awt.headless", "false")
        // 禁用 JMX 服务器
        System.setProperty("com.sun.management.jmxremote", "false")
        // 禁用 JGit 的 JMX 监控（WindowCache 的 MXBean）- 这是最关键的设置
        System.setProperty("org.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled", "true")

        // 额外禁用 JMX 设置以增强兼容性
        System.setProperty("com.sun.management.jmxremote.port", "")
        System.setProperty("java.lang.management.ManagementFactory.createPlatformMXBean", "false")
        System.setProperty("javax.management.builder.initial", "")
        System.setProperty("javax.management.MBeanServerBuilder", "")

        Logger.log("✅ 系统属性设置完成")
    } catch (e: Exception) {
        // 忽略设置系统属性时的异常，不影响应用启动
        Logger.error("设置系统属性时出现异常", e)
    }

    Logger.log("🪟 准备创建应用窗口...")
    application(exitProcessOnExit = false) {
    Logger.log("🪟 application 块已进入")
    // 应用初始化状态
    var isInitialized by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    // 立即显示登录对话框，确保窗口有内容显示
    var showLoginDialog by remember { mutableStateOf(true) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    // 应用启动初始化 - 使用超时机制，避免阻塞
    LaunchedEffect(Unit) {
        try {
            // 设置协程异常处理器
            val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                when (throwable) {
                    is OutOfMemoryError -> {
                        Logger.error("协程内存不足错误 (${coroutineContext[CoroutineName]?.name ?: "unknown"})", throwable)
                        println("💥 协程内存不足错误: ${throwable.message}")
                    }
                    is StackOverflowError -> {
                        Logger.error("协程栈溢出错误 (${coroutineContext[CoroutineName]?.name ?: "unknown"})", throwable)
                        println("💥 协程栈溢出错误: ${throwable.message}")
                    }
                    is Error -> {
                        Logger.error("协程系统错误 (${coroutineContext[CoroutineName]?.name ?: "unknown"})", throwable)
                        println("💥 协程系统错误: ${throwable.javaClass.simpleName} - ${throwable.message}")
                    }
                    is Exception -> {
                        Logger.error("协程异常 (${coroutineContext[CoroutineName]?.name ?: "unknown"})", throwable)
                        println("💥 协程异常: ${throwable.javaClass.simpleName} - ${throwable.message}")
                    }
                    else -> {
                        Logger.error("协程未知异常 (${coroutineContext[CoroutineName]?.name ?: "unknown"})", throwable)
                        println("💥 协程未知异常: ${throwable.javaClass.simpleName} - ${throwable.message}")
                    }
                }
            }
            // 设置协程异常处理器
            val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                Logger.error("协程异常 (${coroutineContext[CoroutineName]?.name ?: "unknown"})", throwable)
                println("💥 协程异常: ${throwable.javaClass.simpleName} - ${throwable.message}")
            }
            Logger.log("🚀 开始应用初始化...")
            Logger.log("🪟 窗口应该已经显示，showLoginDialog = $showLoginDialog")

            // 在后台异步初始化，设置超时
            val initResult = withTimeoutOrNull(10000) { // 10秒超时
                AppInitializer.initialize()
            }

            if (initResult != null && initResult.isSuccess) {
                isInitialized = true
                Logger.log("✅ 应用初始化完成")

                // 检查是否已有登录用户
                if (CurrentUserManager.isLoggedIn()) {
                    isLoggedIn = true
                    showLoginDialog = false
                } else {
                    // 检查是否启用自动登录
                    if (LoginSettingsManager.isAutoLoginEnabled()) {
                        Logger.log("🔐 检测到自动登录设置，开始自动登录...")
                        try {
                            // 在自动登录前确保 JMX 被禁用
                            System.setProperty("org.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled", "true")
                            System.setProperty("com.sun.management.jmxremote", "false")
                            System.setProperty("com.sun.management.jmxremote.port", "")
                            System.setProperty("java.lang.management.ManagementFactory.createPlatformMXBean", "false")
                            System.setProperty("javax.management.builder.initial", "")
                            Logger.log("✅ 自动登录前 JMX 禁用设置")

                            val username = LoginSettingsManager.getRememberedUsername()
                            val password = LoginSettingsManager.getRememberedPassword()

                            if (username.isNotBlank() && password.isNotBlank()) {
                                val loginResult = withTimeoutOrNull(5000) { // 5秒超时
                                    AppInitializer.loginUser(username)
                                }
                                if (loginResult != null && loginResult.isSuccess) {
                                    Logger.log("✅ 自动登录成功")
                                    isLoggedIn = true
                                    showLoginDialog = false
                                    return@LaunchedEffect
                                } else {
                                    Logger.log("❌ 自动登录失败: ${loginResult?.exceptionOrNull()?.message ?: "超时"}")
                                }
                            }
                        } catch (e: Exception) {
                            Logger.error("自动登录异常", e)
                        }
                    }
                    // 保持显示登录对话框
                }
            } else {
                val errorMsg = initResult?.exceptionOrNull()?.message ?: "初始化超时或失败"
                Logger.log("⚠️ 应用初始化失败: $errorMsg")
                // 即使初始化失败，也显示登录界面，让用户可以继续使用
                initializationError = errorMsg
            }
        } catch (e: Exception) {
            Logger.error("应用初始化异常", e)
            // 即使出现异常，也显示登录界面
            initializationError = e.message ?: "未知错误"
        }
    }

    val windowState = rememberWindowState(
        size = DpSize(1300.dp, 800.dp), // macOS风格的窗口尺寸
        position = WindowPosition.Aligned(Alignment.Center)
    )

    var shouldExit by remember { mutableStateOf(false) }
    // 确保窗口默认可见
    var isWindowVisible by remember {
        mutableStateOf(true).also {
            Logger.log("🪟 isWindowVisible 初始化为 true")
        }
    }
    var shouldMinimizeToTray by remember { mutableStateOf(false) }

    // 监听窗口最小化状态，如果需要最小化到托盘，则隐藏窗口
    LaunchedEffect(windowState.isMinimized) {
        if (windowState.isMinimized && shouldMinimizeToTray) {
            // 延迟一下再隐藏，确保最小化动画完成
            kotlinx.coroutines.delay(100)
            isWindowVisible = false
            windowState.isMinimized = false
            shouldMinimizeToTray = false
            Logger.log("✓ 窗口已隐藏到托盘")
        }
    }

    // 监听退出信号
    if (shouldExit) {
        Logger.log("📤 应用准备退出...")
        LaunchedEffect(Unit) {
            performExitCleanup()
        }
    }

    // 显示/恢复窗口的函数
    val showWindow: () -> Unit = remember {
        {
            isWindowVisible = true
            windowState.isMinimized = false
            Logger.log("🔄 托盘：显示窗口")
        }
    }

    // 设置macOS Dock图标点击监听（在Window创建后设置）
    DisposableEffect(Unit) {
        Logger.log("🚀 开始设置Dock监听器...")
        var cleanup: (() -> Unit)? = null

        try {
            val osName = System.getProperty("os.name").lowercase()
            Logger.log("🖥️ 当前操作系统: $osName")
            if (osName.contains("mac")) {
                // 使用反射调用Desktop API（兼容不同JDK版本）
                val desktopClass = Class.forName("java.awt.Desktop")
                val isDesktopSupportedMethod = desktopClass.getMethod("isDesktopSupported")
                val isSupported = isDesktopSupportedMethod.invoke(null) as Boolean
                Logger.log("🖥️ Desktop支持: $isSupported")

                if (isSupported) {
                    val getDesktopMethod = desktopClass.getMethod("getDesktop")
                    val desktop = getDesktopMethod.invoke(null)

                    // 尝试设置AppReopenedListener
                    try {
                        val actionClass = Class.forName("java.awt.Desktop\$Action")
                        val appReopenAction = actionClass.enumConstants.find {
                            it.toString() == "APP_REOPEN"
                        }
                        Logger.log("🖥️ APP_REOPEN action找到: ${appReopenAction != null}")

                        if (appReopenAction != null) {
                            val isSupportedMethod = desktopClass.getMethod("isSupported", actionClass)
                            val actionSupported = isSupportedMethod.invoke(desktop, appReopenAction) as Boolean
                            Logger.log("🖥️ APP_REOPEN支持: $actionSupported")

                            if (actionSupported) {
                                val listenerClass = Class.forName("java.awt.desktop.AppReopenedListener")
                                val proxy = java.lang.reflect.Proxy.newProxyInstance(
                                    listenerClass.classLoader,
                                    arrayOf(listenerClass)
                                ) { _, _, _ ->
                                    Logger.log("🖱️ Dock图标被点击！")
                                    showWindow()
                                    null
                                }

                                val setListenerMethod = desktopClass.getMethod("setAppReopenedListener", listenerClass)
                                setListenerMethod.invoke(desktop, proxy)
                                Logger.log("✓ macOS Dock图标监听已设置")
                            } else {
                                Logger.log("⚠️ 系统不支持APP_REOPEN action")
                            }
                        } else {
                            Logger.log("⚠️ 未找到APP_REOPEN action")
                        }
                    } catch (e: Exception) {
                        Logger.error("设置Dock监听失败（可能是JDK版本不支持）", e)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error("设置Dock监听失败", e)
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
                Logger.log("📤 托盘：请求退出")
                shouldExit = true
            }
        }
    )

    Logger.log("🪟 准备创建 Window composable，isWindowVisible = $isWindowVisible")
    Window(
        onCloseRequest = {
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
        // 监听窗口状态，确保窗口正确显示
        LaunchedEffect(Unit) {
            Logger.log("🪟 Window composable 已创建，visible = $isWindowVisible")
            delay(100)
            Logger.log("🪟 窗口状态检查: isWindowVisible = $isWindowVisible, showLoginDialog = $showLoginDialog, isLoggedIn = $isLoggedIn")
        }

        LaunchedEffect(isWindowVisible) {
            if (isWindowVisible) {
                Logger.log("✓ 窗口已显示，isWindowVisible = true")
            } else {
                Logger.log("⚠️ 窗口已隐藏，isWindowVisible = false")
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
                            // 先设置登录状态，然后再关闭对话框
                            isLoggedIn = true
                            showLoginDialog = false
                        },
                        onDismiss = {
                            // 不允许关闭登录对话框，除非退出应用
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
