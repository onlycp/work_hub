package utils

import java.io.File
import java.io.PrintWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志工具类
 * 将日志同时输出到控制台和文件，方便在 Windows 上查看
 */
object Logger {
    private var logFile: File? = null
    private var logWriter: PrintWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logDir = File(System.getProperty("user.home"), ".workhub")
    private val logFilePath = File(logDir, "app.log")
    @Volatile
    private var initialized = false
    
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
                val separator = "=".repeat(80)
                logWriter?.println("[$timestamp] $separator")
                logWriter?.println("[$timestamp] 应用启动: ${Date()}")
                logWriter?.println("[$timestamp] 日志文件: ${logFilePath.absolutePath}")
                logWriter?.println("[$timestamp] $separator")
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
     * 记录日志
     */
    fun log(message: String) {
        // 确保日志系统已初始化
        ensureInitialized()
        
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $message"
        
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
     * 记录错误日志
     */
    fun error(message: String, throwable: Throwable? = null) {
        log("❌ ERROR: $message")
        throwable?.let {
            log("   异常类型: ${it.javaClass.name}")
            log("   异常消息: ${it.message}")
            it.stackTrace.take(10).forEach { element ->
                log("   at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
            }
        }
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
            log("应用关闭: ${Date()}")
            log("=".repeat(80))
            logWriter?.close()
        } catch (e: Exception) {
            // 忽略关闭时的异常
        }
    }
}

