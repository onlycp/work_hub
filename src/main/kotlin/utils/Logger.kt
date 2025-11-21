package utils

import java.io.File
import java.io.PrintWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志级别枚举
 */
enum class LogLevel(val symbol: String, val priority: Int) {
    DEBUG("🐛", 0),
    INFO("ℹ️", 1),
    WARN("⚠️", 2),
    ERROR("❌", 3)
}

/**
 * 日志工具类
 * 将日志同时输出到控制台和文件，方便在 Windows 上查看
 * 支持多种日志级别和结构化格式
 */
object Logger {
    private var logFile: File? = null
    private var logWriter: PrintWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val logDir = File(System.getProperty("user.home"), ".workhub")
    private val logFilePath = File(logDir, "app.log")
    @Volatile
    private var initialized = false
    private var currentLogLevel = LogLevel.INFO // 默认日志级别
    
    /**
     * 初始化日志系统
     */
    private fun ensureInitialized() {
        if (initialized) return
        
        synchronized(this) {
            if (initialized) return
            
            try {
                // 确保日志目录存在
                if (!logDir.exists()) {
                    val created = logDir.mkdirs()
                    if (!created && !logDir.exists()) {
                        println("⚠️ 无法创建日志目录: ${logDir.absolutePath}")
                        return
                    }
                }
                
                // 创建日志文件（追加模式）
                logFile = logFilePath
                logWriter = PrintWriter(FileWriter(logFile, true), true)
                
                // 标记为已初始化
                initialized = true
                
                // 写入启动标记（直接写入，避免递归调用）
                val timestamp = dateFormat.format(Date())
                val threadName = Thread.currentThread().name
                val separator = "=".repeat(80)
                val startMessage = "[$timestamp] [🚀] [$threadName] [Logger.ensureInitialized] 应用启动: ${Date()}"
                val fileMessage = "[$timestamp] [🚀] [$threadName] [Logger.ensureInitialized] 日志文件: ${logFilePath.absolutePath}"

                logWriter?.println(separator)
                logWriter?.println(startMessage)
                logWriter?.println(fileMessage)
                logWriter?.println(separator)
                logWriter?.flush()

                println("✅ 日志系统已初始化，日志文件: ${logFilePath.absolutePath}")
            } catch (e: Exception) {
                // 如果无法创建日志文件，至少输出到控制台
                println("⚠️ 无法创建日志文件: ${e.message}")
                e.printStackTrace()
                initialized = false
            }
        }
    }
    
    /**
     * 设置日志级别
     */
    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
    }

    /**
     * 获取当前日志级别
     */
    fun getLogLevel(): LogLevel = currentLogLevel

    /**
     * 记录日志（内部方法）
     */
    private fun log(level: LogLevel, message: String, callerInfo: String? = null) {
        // 检查日志级别
        if (level.priority < currentLogLevel.priority) return

        // 确保日志系统已初始化
        ensureInitialized()

        val timestamp = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        val caller = callerInfo ?: getCallerInfo()
        val logMessage = "[$timestamp] [${level.symbol}] [$threadName] [$caller] $message"

        // 输出到控制台
        println(logMessage)

        // 输出到文件
        if (initialized && logWriter != null) {
            try {
                logWriter?.println(logMessage)
                logWriter?.flush()
            } catch (e: Exception) {
                // 如果写入文件失败，至少输出到控制台
                println("⚠️ 写入日志文件失败: ${e.message}")
                initialized = false // 标记为未初始化，下次尝试重新初始化
            }
        }
    }

    /**
     * 获取调用者信息
     */
    private fun getCallerInfo(): String {
        val stackTrace = Thread.currentThread().stackTrace
        // 跳过Thread.getStackTrace和Logger相关的方法调用
        for (i in stackTrace.indices) {
            val element = stackTrace[i]
            if (!element.className.startsWith("utils.Logger") &&
                !element.className.startsWith("java.lang.Thread")) {
                val className = element.className.substringAfterLast('.')
                val methodName = element.methodName
                return "$className.$methodName"
            }
        }
        return "Unknown"
    }

    /**
     * DEBUG级别日志
     */
    fun debug(message: String) = log(LogLevel.DEBUG, message)

    /**
     * INFO级别日志
     */
    fun info(message: String) = log(LogLevel.INFO, message)

    /**
     * WARN级别日志
     */
    fun warn(message: String) = log(LogLevel.WARN, message)

    /**
     * 记录日志（向后兼容）
     */
    fun log(message: String) = info(message)
    
    /**
     * ERROR级别日志
     */
    fun error(message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, message)
        throwable?.let {
            log(LogLevel.ERROR, "异常类型: ${it.javaClass.simpleName}")
            log(LogLevel.ERROR, "异常消息: ${it.message ?: "无消息"}")
            log(LogLevel.ERROR, "异常堆栈:")
            it.stackTrace.take(8).forEach { element ->
                log(LogLevel.ERROR, "  └─ ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
            }
            if (it.stackTrace.size > 8) {
                log(LogLevel.ERROR, "  └─ ... (${it.stackTrace.size - 8} 更多帧)")
            }
        }
    }

    /**
     * 记录异常（简化版本）
     */
    fun error(throwable: Throwable) {
        error("发生异常: ${throwable.javaClass.simpleName}", throwable)
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String {
        return logFilePath.absolutePath
    }
    
    /**
     * 关闭日志文件
     */
    fun close() {
        try {
            info("应用关闭: ${Date()}")
            info("=".repeat(80))
            logWriter?.close()
        } catch (e: Exception) {
            // 忽略关闭时的异常
        }
    }
}

