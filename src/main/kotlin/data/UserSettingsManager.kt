package data

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 用户设置管理器
 * 管理用户的个人设置
 */
object UserSettingsManager {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // 用户设置状态
    private val _settings = MutableStateFlow<UserSettings>(UserSettings(userId = ""))
    val settings: StateFlow<UserSettings> = _settings

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        // 加载用户设置
        loadSettings(userId)
    }

    /**
     * 获取当前设置
     */
    fun getCurrentSettings(): UserSettings = _settings.value

    /**
     * 更新设置
     */
    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    /**
     * 加载用户设置
     */
    private fun loadSettings(userId: String) {
        try {
            val userSettingsFile = getUserSettingsFile(userId)
            if (userSettingsFile.exists()) {
                val jsonString = userSettingsFile.readText()
                val loadedSettings = json.decodeFromString<UserSettings>(jsonString)
                _settings.value = loadedSettings
            } else {
                // 使用默认设置
                _settings.value = UserSettings(userId = userId)
            }
        } catch (e: Exception) {
            println("❌ 加载用户设置失败: ${e.message}")
            _settings.value = UserSettings(userId = userId)
        }
    }

    /**
     * 保存用户设置
     */
    private fun saveSettings(settings: UserSettings) {
        try {
            val userSettingsFile = getUserSettingsFile(settings.userId)
            userSettingsFile.parentFile?.mkdirs()

            val jsonString = json.encodeToString(settings)
            userSettingsFile.writeText(jsonString)

            println("✅ 用户设置已保存")
        } catch (e: Exception) {
            println("❌ 保存用户设置失败: ${e.message}")
        }
    }

    /**
     * 获取用户设置文件
     */
    private fun getUserSettingsFile(userId: String): File {
        val userDir = File(System.getProperty("user.home"), ".workhub/users/$userId")
        userDir.mkdirs()
        return File(userDir, "settings.json")
    }
}

/**
 * 用户设置数据类
 */
@Serializable
data class UserSettings(
    // 用户标识
    val userId: String = "",
    val userName: String = "",

    // 界面设置
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "zh-CN",
    val autoSave: Boolean = true,
    val autoSyncOnExit: Boolean = true,

    // 窗口设置
    val windowWidth: Int = 1200,
    val windowHeight: Int = 800,

    // 字体设置 - 界面字体（按钮、标签等）
    val uiFontSettings: FontSettings = FontSettings(family = "System", size = 13, lineHeight = 1.5f),

    // Git同步设置
    val gitSyncSettings: GitSyncSettings = GitSyncSettings(),

    // AI助手配置
    val aiSettings: AISettings = AISettings(),

    // 最后同步时间
    val lastSyncTime: String = ""
)

/**
 * 主题模式枚举
 */
@Serializable
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/**
 * 字体设置
 */
@Serializable
data class FontSettings(
    val family: String = "System",
    val size: Int = 13,
    val lineHeight: Float = 1.5f
)

/**
 * Git同步设置
 */
@Serializable
data class GitSyncSettings(
    val enabled: Boolean = true,
    val repositoryUrl: String = "",
    val username: String = "",
    val password: String = "",
    val branch: String = "main",
    val autoSyncOnStart: Boolean = true,
    val autoSyncOnExit: Boolean = true
)

/**
 * AI助手配置
 */
@Serializable
data class AISettings(
    val enabled: Boolean = false,
    val apiUrl: String = "",
    val apiKey: String = "",
    val model: String = ""
)
